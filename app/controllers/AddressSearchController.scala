/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import model.address.{AddressRecord, Postcode}
import model.request.{LookupByCountryRequest, LookupByPostTownRequest, LookupByPostcodeRequest, LookupByUprnRequest}
import model.response.SupportedCountryCodes
import model.{AddressSearchAuditEvent, AddressSearchAuditEventMatchedAddress, AddressSearchAuditEventRequestDetails}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import repositories.{ABPAddressRepository, NonABPAddressRepository}
import services.ResponseProcessor
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AddressSearchController @Inject()(addressSearch: ABPAddressRepository, nonABPAddressSearcher: NonABPAddressRepository,
                                        responseProcessor: ResponseProcessor, auditConnector: AuditConnector, ec: ExecutionContext,
                                        cc: ControllerComponents, supportedCountryCodes: SupportedCountryCodes)
  extends AddressController(cc) {

  import model.AddressSearchAuditEvent._

  implicit private val xec: ExecutionContext = ec

  def search(): Action[String] = Action.async(parse.tolerantText) {
    request =>
      Json.parse(request.body).validate[LookupByPostcodeRequest] match {
        case JsSuccess(lookupByPostcodeRequest, _) =>
          val origin = getOriginHeaderIfSatisfactory(request.headers)
          searchByPostcode(request, lookupByPostcodeRequest.postcode, lookupByPostcodeRequest.filter, origin)
        case JsError(errors)                       =>
          Future.successful(BadRequest(JsError.toJson(errors)))
      }
  }

  def searchByUprn(): Action[String] = Action.async(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json)      => json.validate[LookupByUprnRequest] match {
          case JsSuccess(lookupByUprnRequest, _) =>
            val origin = getOriginHeaderIfSatisfactory(request.headers)
            searchByUprn(request, lookupByUprnRequest.uprn, origin)
          case JsError(errors)                   =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(exception) => Future.successful(BadRequest("""{"obj":[{"msg":["error.payload.missing"],"args":[]}]}"""))
      }
  }

  def searchByPostTown(): Action[String] = Action.async(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json)      => json.validate[LookupByPostTownRequest] match {
          case JsSuccess(lookupByTownRequest, _) =>
            val origin = getOriginHeaderIfSatisfactory(request.headers)
            searchByTown(request, lookupByTownRequest.posttown, lookupByTownRequest.filter, origin)
          case JsError(errors)                   =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(exception) => Future.successful(BadRequest("""{"obj":[{"msg":["error.payload.missing"],"args":[]}]}"""))
      }
  }

  def supportedCountries(): Action[AnyContent] = Action.async {
    import model.response.SupportedCountryCodes._
    Future.successful(Ok(Json.toJson(supportedCountryCodes)))
  }

  def searchByCountry(countryCode: String): Action[String] = Action.async(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json)      => json.validate[LookupByCountryRequest] match {
          case JsSuccess(lookupByCountryRequest, _) =>
            val origin = getOriginHeaderIfSatisfactory(request.headers)
            searchByCountry(request, countryCode.toLowerCase(), lookupByCountryRequest.filter, origin)
          case JsError(errors)                      =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(exception) => Future.successful(BadRequest("""{"obj":[{"msg":["error.payload.missing"],"args":[]}]}"""))
      }
  }

  private[controllers] def searchByUprn[A](request: Request[A], uprn: String, origin: String): Future[Result] = {
    if (Try(uprn.toLong).isFailure) {
      Future.successful {
        badRequest("BAD-UPRN", "origin" -> origin, "uprn" -> uprn, "error" -> s"uprn must only consist of digits")
      }
    } else {
      import model.address.AddressRecord.formats._

      addressSearch.findUprn(uprn).map {
        a =>
          val a2 = responseProcessor.convertAddressList(a)
          logEvent("LOOKUP", "origin" -> origin, "uprn" -> uprn, "matches" -> a2.size.toString)
          Ok(Json.toJson(a2))
      }
    }
  }

  private[controllers] def searchByPostcode[A](request: Request[A], postcode: Postcode, filter: Option[String], origin: String): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    if (postcode.toString.isEmpty) {
      Future.successful {
        badRequest("BAD-POSTCODE", "origin" -> origin, "error" -> s"missing or badly-formed $postcode parameter")
      }
    } else {
      import model.address.AddressRecord.formats._
      addressSearch.findPostcode(postcode, filter).map {
        a =>
          val userAgent = request.headers.get("User-Agent")
          val a2 = responseProcessor.convertAddressList(a)

          if (a2.nonEmpty) {
            auditAddressSearch(userAgent, a2, postcode = Some(postcode), filter = filter)
          }

          logEvent("LOOKUP", origin, a2.size, List(Some("postcode" -> postcode.toString), filter.map(f => "filter" -> f)).flatten)
          Ok(Json.toJson(a2))
      }
    }
  }

  private[controllers] def searchByTown[A](request: Request[A], posttown: String, filter: Option[String], origin: String): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    if (posttown.isEmpty) {
      Future.successful {
        badRequest("BAD-POSTTOWN", "origin" -> origin, "error" -> s"missing or badly-formed $posttown parameter")
      }
    } else {
      import model.address.AddressRecord.formats._

      addressSearch.findTown(posttown, filter).map {
        a =>
          val userAgent = request.headers.get("User-Agent")
          val a2 = responseProcessor.convertAddressList(a)

          if (a2.nonEmpty) {
            auditAddressSearch(userAgent, a2, postTown = Some(posttown), filter = filter)
          }

          logEvent("LOOKUP", origin, a2.size, List(Some("posttown" -> posttown), filter.map(f => "filter" -> f)).flatten)
          Ok(Json.toJson(a2))
      }
    }
  }

  private[controllers] def searchByCountry[A](request: Request[A], countryCode: String, filter: String, origin: String): Future[Result] = {
    if (countryCode.isEmpty || "[a-zA-Z]{2}".r.unapplySeq(countryCode).isEmpty) {
      Future.successful {
        badRequest("BAD-COUNTRYCODE", "origin" -> origin, "error" -> s"missing or badly-formed country code")
      }
    } else if (supportedCountryCodes.abp.contains(countryCode)) {
      Future.successful {
        badRequest("ABP-COUNTRYCODE", "origin" -> origin, "error" -> s"country code $countryCode is abp.")
      }
    } else if (!supportedCountryCodes.nonAbp.contains(countryCode)) {
      Future.successful {
        notFound("UNSUPPORTED-COUNTRYCODE", "origin" -> origin, "error" -> s"country code  $countryCode unsupported")
      }
    } else {
      import model.internal.NonUKAddress._

      nonABPAddressSearcher.findInCountry(countryCode, filter).map {
        a =>
          logEvent("LOOKUP", origin, a.size, List("countryCode" -> countryCode, "filter" -> filter))
          Ok(Json.toJson(a))
      }
    }
  }

  private def auditAddressSearch[A](userAgent: Option[String], a2: List[AddressRecord], postcode: Option[Postcode] = None,
                                    postTown: Option[String] = None, filter: Option[String] = None)(implicit hc: HeaderCarrier): Unit = {

    auditConnector.sendExplicitAudit("AddressSearch",
      AddressSearchAuditEvent(userAgent,
        AddressSearchAuditEventRequestDetails(postcode.map(_.toString), postTown, filter),
        a2.length,
        a2.map { ma =>
          AddressSearchAuditEventMatchedAddress(
            ma.uprn.getOrElse("").toString,
            ma.parentUprn,
            ma.usrn,
            ma.organisation,
            ma.address.lines,
            ma.address.town,
            ma.localCustodian,
            ma.location,
            ma.administrativeArea,
            ma.poBox,
            ma.address.postcode,
            ma.address.subdivision,
            ma.address.country)
        }))
  }
}
