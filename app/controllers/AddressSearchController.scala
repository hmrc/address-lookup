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
import audit.Auditor
import config.AppConfig
import connectors.DownstreamConnector
import model.address.{AddressRecord, NonUKAddress}
import model.request._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.request.RequestTarget
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressSearchController @Inject()(connector: DownstreamConnector, auditor: Auditor, cc: ControllerComponents, val configHelper: AppConfig)(implicit ec: ExecutionContext)
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
      val newRequest: Request[LookupByCountryRequest] =
        request.withTarget(RequestTarget("/country/lookup", "/country/lookup", request.queryString))
          .withBody(addCountryTo(request.body, countryCode.toLowerCase))


      searchByCountry(newRequest)
  }

  private[controllers] def searchByUprn(request: Request[LookupByUprnRequest])(implicit hc: HeaderCarrier, userAgent: Option[UserAgent]): Future[Result] = {
    import model.address.AddressRecord.formats._

    forwardIfAllowed[LookupByUprnRequest, List[AddressRecord]](request,
      addresses => auditor.auditAddressSearch(userAgent, addresses, uprn = Some(request.body.uprn))
    )
  }

  private[controllers] def searchByPostcode[A](request: Request[LookupByPostcodeRequest])(implicit hc: HeaderCarrier, userAgent: Option[UserAgent]): Future[Result] = {
    import model.address.AddressRecord.formats._

    val postcode: LookupByPostcodeRequest = request.body

    forwardIfAllowed[LookupByPostcodeRequest, List[AddressRecord]](request,
      addresses => auditor.auditAddressSearch(userAgent, addresses, postcode = Some(postcode.postcode), filter = postcode.filter))
  }

  private[controllers] def searchByTown[A](request: Request[LookupByPostTownRequest])(implicit hc: HeaderCarrier, userAgent: Option[UserAgent]): Future[Result] = {
    import model.address.AddressRecord.formats._

    val posttown: LookupByPostTownRequest = request.body

    forwardIfAllowed[LookupByPostTownRequest, List[AddressRecord]](request,
      addresses => auditor.auditAddressSearch(userAgent, addresses, posttown = Some(posttown.posttown.toUpperCase), filter = posttown.filter))
  }

  private[controllers] def searchByCountry[A](request: Request[LookupByCountryRequest])(implicit hc: HeaderCarrier, userAgent: Option[UserAgent]): Future[Result] = {
    import model.address.NonUKAddress._

    val country: LookupByCountryRequest = request.body

    forwardIfAllowed[LookupByCountryRequest, List[NonUKAddress]](request,
      addresses => auditor.auditNonUKAddressSearch(userAgent, country = country.country, filter = Option(country.filter), nonUKAddresses = addresses))
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

  implicit def requestToHeaderCarrier[T](implicit request: Request[T]): HeaderCarrier =
    HeaderCarrierConverter.fromRequest(request)

  implicit def requestToUserAgent[T](implicit request: Request[T]): Option[UserAgent] =
    UserAgent(request)

  connector.checkConnectivity(url("/ping/ping"), configHelper.addressSearchApiAuthToken).map {
    case true => logger.warn("Downstream connectivity to address-search-api service successfully established")
    case _    => logger.error("Downstream connectivity check to address-search-api service FAILED")
  }
}
