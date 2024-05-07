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
import model.request.{LookupByCountryRequest, LookupByPostTownRequest, LookupByPostcodeRequest, LookupByUprnRequest}
import model.response.{ErrorResponse, SupportedCountryCodes}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
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
                                        val configHelper: ConfigHelper)(ec: ExecutionContext)
  extends BaseController with AccessChecker with Logging {
  import ErrorResponse.Implicits._

  scheduler.enable()

  def search(): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json) =>
          json.validate[LookupByPostcodeRequest](LookupByPostcodeRequest.reads) match {
            case JsSuccess(lookupByPostcodeRequest, _) =>
              addressSearchService.searchByPostcode(request, lookupByPostcodeRequest.postcode, lookupByPostcodeRequest.filter)
            case JsError(errors) =>
              Future.successful(BadRequest(JsError.toJson(errors)))
          }
        case Failure(_) => Future.successful(BadRequest(Json.toJson(ErrorResponse.invalidJson)))
      }
  }

  def searchByUprn(): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json) => json.validate[LookupByUprnRequest] match {
          case JsSuccess(lookupByUprnRequest, _) =>
            addressSearchService.searchByUprn(request, lookupByUprnRequest.uprn)
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(_) => Future.successful(BadRequest(Json.toJson(ErrorResponse.invalidJson)))
      }
  }

  def searchByPostTown(): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json) => json.validate[LookupByPostTownRequest] match {
          case JsSuccess(lookupByTownRequest, _) =>
            addressSearchService.searchByTown(request, lookupByTownRequest.posttown, lookupByTownRequest.filter)
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(_) => Future.successful(BadRequest(Json.toJson(ErrorResponse.invalidJson)))
      }
  }

  def supportedCountries(): Action[AnyContent] = Action.async {
    import model.response.SupportedCountryCodes._
    Future.successful(Ok(Json.toJson(supportedCountryCodes)))
  }

  def searchByCountry(countryCode: String): Action[String] = accessCheckedAction(parse.tolerantText) {
    request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val maybeJson = Try(Json.parse(request.body))
      maybeJson match {
        case Success(json) => json.validate[LookupByCountryRequest] match {
          case JsSuccess(lookupByCountryRequest, _) =>
            val userAgent = request.headers.get("User-Agent")
            addressSearchService.searchByCountry(userAgent, countryCode.toLowerCase(), lookupByCountryRequest.filter)
          case JsError(errors) =>
            Future.successful(BadRequest(JsError.toJson(errors)))
        }
        case Failure(_) => Future.successful(BadRequest(Json.toJson(ErrorResponse.invalidJson)))
      }
  }
}
