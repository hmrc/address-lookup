/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.effect.IO
import controllers.services.{AddressSearcher, ResponseProcessor}
import model.address.Postcode
import model.request.{LookupByPostTownRequest, LookupByPostcodeRequest, LookupByUprnRequest}
import model.{AddressSearchAuditEvent, AddressSearchAuditEventMatchedAddress}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AddressSearchController @Inject()(addressSearch: AddressSearcher, responseProcessor: ResponseProcessor,
                                        auditConnector: AuditConnector, ec: ExecutionContext, cc: ControllerComponents)
  extends AddressController(cc) {

  implicit private val xec: ExecutionContext = ec

  def search(): Action[String] = Action.async(parse.tolerantText) {
    request =>
      Json.parse(request.body).validate[LookupByPostcodeRequest] match {
        case JsSuccess(lookupByPostcodeRequest, _) =>
          val origin = getOriginHeaderIfSatisfactory(request.headers)
          searchByPostcode(request, lookupByPostcodeRequest.postcode, lookupByPostcodeRequest.filter, origin)
        case JsError(errors) =>
          Future.successful(BadRequest(JsError.toJson(errors)))
      }
  }

  def searchByUprn(): Action[String] = Action.async(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json) => json.validate[LookupByUprnRequest] match {
          case JsSuccess(lookupByUprnRequest, _) =>
            val origin = getOriginHeaderIfSatisfactory(request.headers)
            searchByUprn(lookupByUprnRequest.uprn, origin)
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(_) => Future.successful(BadRequest("""{"obj":[{"msg":["error.payload.missing"],"args":[]}]}"""))
      }
  }

  def searchByPostTown(): Action[String] = Action.async(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json) => json.validate[LookupByPostTownRequest] match {
          case JsSuccess(lookupByTownRequest, _) =>
            val origin = getOriginHeaderIfSatisfactory(request.headers)
            searchByTown(lookupByTownRequest.posttown, lookupByTownRequest.filter, origin)
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(_) => Future.successful(BadRequest("""{"obj":[{"msg":["error.payload.missing"],"args":[]}]}"""))
      }
  }

  private[controllers] def searchByUprn[A](uprn: String, origin: String): Future[Result] = {
    if (Try(uprn.toLong).isFailure) {
      IO {
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
  }.unsafeToFuture()

  private[controllers] def searchByPostcode[A](request: Request[A], postcode: Postcode, filter: Option[String], origin: String): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    if (postcode.toString.isEmpty) {
      IO {
        badRequest("BAD-POSTCODE", "origin" -> origin, "error" -> s"missing or badly-formed $postcode parameter")
      }
    } else {
      import model.address.AddressRecord.formats._
      addressSearch.findPostcode(postcode, filter).map {
        a =>
          import model.AddressSearchAuditEvent._

          if (a.nonEmpty) {
            val userAgent = request.headers.get("User-Agent")
            auditConnector.sendExplicitAudit("AddressSearch",
              AddressSearchAuditEvent(userAgent, a.length, a.map { ma =>
                AddressSearchAuditEventMatchedAddress(
                  ma.uprn.toString, ma.lines, ma.town, ma.administrativeArea, ma.postcode, ma.country)
              }))
          }

          val a2 = responseProcessor.convertAddressList(a)
          logEvent("LOOKUP", origin, a2.size, List(Option("postcode" -> postcode.toString), filter.map(f => "filter" -> f)).flatten)
          Ok(Json.toJson(a2))
      }
    }
  }.unsafeToFuture

  private[controllers] def searchByTown[A](posttown: String, filter: Option[String], origin: String): Future[Result] = {
    if (posttown.isEmpty) {
      IO {
        badRequest("BAD-POSTCODE", "origin" -> origin, "error" -> s"missing or badly-formed $posttown parameter")
      }
    } else {
      import model.address.AddressRecord.formats._

      addressSearch.findTown(posttown, filter).map {
        a =>
          val a2 = responseProcessor.convertAddressList(a)
          logEvent("LOOKUP", origin, a2.size, List(Option("posttown" -> posttown), filter.map(f => "filter" -> f)).flatten)
          Ok(Json.toJson(a2))
      }
    }
  }.unsafeToFuture()
}
