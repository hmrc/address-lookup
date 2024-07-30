/*
 * Copyright 2023 HM Revenue & Customs
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

import access.AccessChecker
import config.AppConfig
import connectors.DownstreamConnector
import model._
import model.address.{AddressRecord, NonUKAddress, Postcode}
import model.request._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.request.RequestTarget
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressSearchController @Inject()(connector: DownstreamConnector, auditConnector: AuditConnector, cc: ControllerComponents, val configHelper: AppConfig)(implicit ec: ExecutionContext)
  extends BackendController(cc) with Logging with AccessChecker {
  private val actorSystem = ActorSystem("AddressSearchController")
  private implicit val materializer: Materializer = Materializer.createMaterializer(actorSystem)

  def searchByPostcode(): Action[LookupByPostcodeRequest] = accessCheckedAction(parse.json[LookupByPostcodeRequest]) {
    implicit request: Request[LookupByPostcodeRequest] =>
      searchByPostcode(request)
  }

  def searchByUprn(): Action[LookupByUprnRequest] = accessCheckedAction(parse.json[LookupByUprnRequest]) {
    implicit request: Request[LookupByUprnRequest] =>
      searchByUprn(request)
  }

  def searchByPostTown(): Action[LookupByPostTownRequest] = accessCheckedAction(parse.json[LookupByPostTownRequest]) {
    implicit request: Request[LookupByPostTownRequest] =>
      searchByTown(request)
  }

  def supportedCountries(): Action[AnyContent] = Action.async {
    request: Request[AnyContent] =>
      forwardIfAllowed[JsValue, JsValue](request.map(r => JsObject.empty), _ => ())
  }

  def searchByCountry(countryCode: String): Action[LookupByCountryFilterRequest] = accessCheckedAction(parse.json[LookupByCountryFilterRequest]) {
    implicit request: Request[LookupByCountryFilterRequest] =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      val newRequest: Request[LookupByCountryRequest] =
        request.withTarget(RequestTarget("/country/lookup", "/country/lookup", request.queryString))
          .withBody(addCountryTo(request.body, countryCode.toLowerCase))


      searchByCountry(newRequest)
  }

  private[controllers] def searchByUprn(request: Request[LookupByUprnRequest]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    val userAgent = request.headers.get(HeaderNames.USER_AGENT)

    val uprn: LookupByUprnRequest = request.body

    import model.address.AddressRecord.formats._

    forwardIfAllowed[LookupByUprnRequest, List[AddressRecord]](request,
      addresses => auditAddressSearch(userAgent, addresses, uprn = Some(uprn.uprn))
    )
  }

  private[controllers] def searchByPostcode[A](request: Request[LookupByPostcodeRequest]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    val userAgent = request.headers.get(HeaderNames.USER_AGENT)

    val postcode: LookupByPostcodeRequest = request.body

    import model.address.AddressRecord.formats._

    forwardIfAllowed[LookupByPostcodeRequest, List[AddressRecord]](request,
      addresses => auditAddressSearch(userAgent, addresses, postcode = Some(postcode.postcode), filter = postcode.filter))
  }

  private[controllers] def searchByTown[A](request: Request[LookupByPostTownRequest]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    val userAgent = request.headers.get(HeaderNames.USER_AGENT)

    val posttown: LookupByPostTownRequest = request.body

    import model.address.AddressRecord.formats._

    forwardIfAllowed[LookupByPostTownRequest, List[AddressRecord]](request,
      addresses => auditAddressSearch(userAgent, addresses, posttown = Some(posttown.posttown.toUpperCase), filter = posttown.filter))
  }

  private[controllers] def searchByCountry[A](request: Request[LookupByCountryRequest])(implicit hc: HeaderCarrier): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    val userAgent = request.headers.get(HeaderNames.USER_AGENT)

    val country: LookupByCountryRequest = request.body

    import model.address.NonUKAddress._

    forwardIfAllowed[LookupByCountryRequest, List[NonUKAddress]](request,
      addresses => auditNonUKAddressSearch(userAgent, country = country.country, filter = Option(country.filter), nonUKAddresses = addresses))
  }

  private def addCountryTo(body: LookupByCountryFilterRequest, country: String): LookupByCountryRequest = {
    LookupByCountryRequest(country, body.filter)
  }

  private def url(path: String) = s"${configHelper.addressSearchApiBaseUrl}$path"

  private def forwardIfAllowed[Req: Writes, Resp:Reads](request: Request[Req], auditFn: Resp => Unit): Future[Result] = {
    val newHeadersMap = request.headers.toSimpleMap ++ Map(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
    val jsonRequest = request.withHeaders(Headers(newHeadersMap.toSeq: _*))
    connector.forward(jsonRequest.map((r: Req) => Json.toJson(r)), url(jsonRequest.target.uri.toString), configHelper.addressSearchApiAuthToken)
      .flatMap(res => res.body.consumeData.map(d => res.header.status -> d))
      .map { case (s, bs) => s -> bs.utf8String }
      .map { case (s, res) => s -> Json.parse(res) }
      .map {
        case (OK, js)           =>
          auditFn(Json.fromJson[Resp](js).get)
          Ok(js)
        case (NOT_FOUND, err)   => NotFound(err)
        case (BAD_REQUEST, err) => BadRequest(err)
        case (FORBIDDEN, err) => Forbidden(err)
      }

  }

  connector.checkConnectivity(url("/ping/ping"), configHelper.addressSearchApiAuthToken).map {
    case true => logger.warn("Downstream connectivity to address-search-api service successfully established")
    case _    => logger.error("Downstream connectivity check to address-search-api service FAILED")
  }

  private def auditAddressSearch[A](userAgent: Option[String], addressRecords: List[AddressRecord], postcode: Option[Postcode] = None,
                                    posttown: Option[String] = None, uprn: Option[String] = None, filter: Option[String] = None)(implicit hc: HeaderCarrier): Unit = {

    if (addressRecords.nonEmpty) {
      val auditEventRequestDetails = AddressSearchAuditEventRequestDetails(postcode.map(_.toString), posttown, uprn, filter)
      val addressSearchAuditEventMatchedAddresses = addressRecords.map { ma =>
        AddressSearchAuditEventMatchedAddress(
          ma.uprn.map(_.toString).getOrElse(""),
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
      }

      auditConnector.sendExplicitAudit("AddressSearch",
        AddressSearchAuditEvent(userAgent,
          auditEventRequestDetails,
          addressRecords.length,
          addressSearchAuditEventMatchedAddresses))
    }
  }

  private def auditNonUKAddressSearch[A](userAgent: Option[String], nonUKAddresses: List[NonUKAddress], country: String,
                                         filter: Option[String] = None)(implicit hc: HeaderCarrier): Unit = {

    if (nonUKAddresses.nonEmpty) {
      auditConnector.sendExplicitAudit("NonUKAddressSearch",
        NonUKAddressSearchAuditEvent(userAgent,
          NonUKAddressSearchAuditEventRequestDetails(filter),
          nonUKAddresses.length,
          nonUKAddresses.map { ma =>
            NonUKAddressSearchAuditEventMatchedAddress(
              ma.id,
              ma.number,
              ma.street,
              ma.unit,
              ma.city,
              ma.district,
              ma.region,
              ma.postcode,
              country)
          }))
    }
  }
}
