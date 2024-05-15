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
import config.ConfigHelper
import model.address.Postcode
import model.request._
import model.response.{ErrorResponse, SupportedCountryCodes}
import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json, Reads}
import play.api.mvc._
import services.{AddressSearchService, CheckAddressDataScheduler}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AddressSearchController @Inject()(addressSearchService: AddressSearchService,
                                        val controllerComponents: ControllerComponents,
                                        supportedCountryCodes: SupportedCountryCodes,
                                        scheduler: CheckAddressDataScheduler,
                                        val configHelper: ConfigHelper)(implicit ec: ExecutionContext)
  extends BaseController with AccessChecker with Logging {

  import ErrorResponse.Implicits._

  scheduler.enable()

  def search(): Action[String] =
    accessCheckedAction(parse.tolerantText) {
      (request, hc) =>
        implicit val hcc: HeaderCarrier = hc
        implicit val sfn: (Request[String], LookupByPostcodeRequest) => Future[Result] = addressSearchService.searchByPostcode(_, _)

        doSearch[Postcode, LookupByPostcodeRequest](request)
    }

  def searchByUprn(): Action[String] = accessCheckedAction(parse.tolerantText) {
    (request, hc) =>
      implicit val hcc: HeaderCarrier = hc
      implicit val sfn: (Request[String], LookupByUprnRequest) => Future[Result] = addressSearchService.searchByUprn(_, _)

      doSearch[String, LookupByUprnRequest](request)
  }

  def searchByPostTown(): Action[String] = accessCheckedAction(parse.tolerantText) {
    (request, hc) =>
      implicit val hcc: HeaderCarrier = hc
      implicit val sfn: (Request[String], LookupByPostTownRequest) => Future[Result] = addressSearchService.searchByTown(_, _)

      doSearch[String, LookupByPostTownRequest](request)
  }

  def searchByCountry(countryCode: String): Action[String] = accessCheckedAction(parse.tolerantText) {
    (request, hc) =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      withValidJson[LookupByCountryRequestFilter](request, _ => Json.toJson(ErrorResponse.invalidJson)) match {
        case Left(err)                                    =>
          err
        case Right(country: LookupByCountryRequestFilter) =>
          val countryLookup = LookupByCountryRequest.fromLookupByCountryRequestFilter(countryCode.toLowerCase, country)
          doLookup[String, LookupByCountryRequest](request, addressSearchService.searchByCountry(_, _), countryLookup)
      }
  }

  def supportedCountries(): Action[AnyContent] = Action.async {
    import model.response.SupportedCountryCodes._
    Future.successful(Ok(Json.toJson(supportedCountryCodes)))
  }

  private def doSearch[U, T <: LookupRequest[U] : Reads](request: Request[String])(implicit hc: HeaderCarrier, searchFn: (Request[String], T) => Future[Result]): Future[Result] = {
    withValidJson[T](request) match {
      case Left(err)        => err
      case Right(lookup: T) =>
        doLookup[U, T](request, searchFn, lookup)
    }
  }

  private def withValidJson[T: Reads](request: Request[String],
                                      validateError: JsError => JsValue = err => JsError.toJson(err)): Either[Future[Result], Any] = {
    Try(Json.parse(request.body)).toEither match {
      case Right(json) => json.validate[T].asEither match {
        case Right(value) =>
          Right(value)
        case Left(errs)   =>
          Left(Future.successful(BadRequest(validateError(JsError(errs)))))
      }
      case Left(_)     => Left(Future.successful(BadRequest(Json.toJson(ErrorResponse.invalidJson))))
    }
  }

  private def doLookup[T, B <: LookupRequest[T]](request: Request[String], lookup: (Request[String], B) => Future[Result], lookupRequest: B)(implicit hc: HeaderCarrier) =
    lookup(request, lookupRequest)
}
