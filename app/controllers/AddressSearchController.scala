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
import model.request.{LookupByCountryRequest, LookupByCountryRequestFilter, LookupByPostTownRequest, LookupByPostcodeRequest, LookupByUprnRequest}
import model.response.{ErrorResponse, SupportedCountryCodes}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import play.api.mvc._
import services.{AddressSearchService, CheckAddressDataScheduler}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AddressSearchController @Inject()(addressSearchService: AddressSearchService,
                                        val controllerComponents: ControllerComponents,
                                        supportedCountryCodes: SupportedCountryCodes,
                                        scheduler: CheckAddressDataScheduler,
                                        val configHelper: ConfigHelper)(implicit ec: ExecutionContext)
  extends BaseController with AccessChecker with Logging {
  import ErrorResponse.Implicits._

  scheduler.enable()

  def search(): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      withValidJson[LookupByPostcodeRequest](request) match {
        case Left(err)                      => err
        case Right(lookupByPostcodeRequest: LookupByPostcodeRequest) =>
          addressSearchService.searchByPostcode(request, lookupByPostcodeRequest.postcode, lookupByPostcodeRequest.filter)
      }
  }

  def searchByUprn(): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      withValidJson[LookupByUprnRequest](request) match {
        case Left(err)                      => err
        case Right(lookupByUprnRequest: LookupByUprnRequest) =>
          addressSearchService.searchByUprn(request, lookupByUprnRequest.uprn)
      }
  }

  def searchByPostTown(): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      withValidJson[LookupByPostTownRequest](request) match {
        case Left(err)                      => err
        case Right(lookupByPostTownRequest: LookupByPostTownRequest) =>
          addressSearchService.searchByTown(request, lookupByPostTownRequest.posttown.toLowerCase(), lookupByPostTownRequest.filter)
      }
  }

  def searchByCountry(countryCode: String): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      withValidJson[LookupByCountryRequestFilter](request, je => Json.toJson(ErrorResponse.invalidJson)) match {
        case Left(err)                                    => err
        case Right(country: LookupByCountryRequestFilter) =>
          val countryLookup = LookupByCountryRequest.fromLookupByCountryRequestFilter(countryCode, country)
          addressSearchService.searchByCountry(request, countryLookup.country.toLowerCase(), countryLookup.filter)
      }
  }

  private def withValidJson[T: Reads](request: Request[String],
                                      validateError: JsError => JsValue = err => JsError.toJson(err)): Either[Future[Result], Any] = {
    Try(Json.parse(request.body)) match {
      case Success(json) => json.validate[T] match {
        case JsSuccess(value, _) =>
          Right(value)
        case jse@JsError(_)     =>
          Left(Future.successful(BadRequest(validateError(jse))))
      }
      case Failure(_) => Left(Future.successful(BadRequest(Json.toJson(ErrorResponse.invalidJson))))
    }
  }

  def supportedCountries(): Action[AnyContent] = Action.async {
    import model.response.SupportedCountryCodes._
    Future.successful(Ok(Json.toJson(supportedCountryCodes)))
  }
}
