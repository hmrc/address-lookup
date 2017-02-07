/*
 * Copyright 2017 HM Revenue & Customs
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

package osgb

import javax.inject.Inject

import osgb.outmodel.Marshall
import osgb.services._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.address.v2.AddressRecord
import uk.gov.hmrc.logging.SimpleLogger

import scala.concurrent.{ExecutionContext, Future}

class AddressSearchController @Inject() (addressSearch: AddressSearcher, responseProcessor: ResponseProcessor, logger: SimpleLogger, ec: ExecutionContext) extends AddressController(logger) {
  implicit private val xec = ec

  import SearchParameters._

  def searchV1(): Action[AnyContent] = Action.async {
    request =>
      searchRequest(request, Marshall.marshallV1List)
  }

  def searchV2(): Action[AnyContent] = Action.async {
    request =>
      searchRequest(request, Marshall.marshallV2List)
  }

  private[osgb] def searchRequest[A](request: Request[A], marshall: List[AddressRecord] => JsValue): Future[Result] = {
    val origin = getOriginHeaderIfSatisfactory(request.headers)
    val sp = SearchParameters.fromRequest(request.queryString).clean
    if (sp.uprn.isDefined) searchUprnRequest(request, sp.uprn.get, origin, marshall)
    else if (sp.outcode.isDefined) searchOutcodeRequest(request, sp, origin, marshall)
    else if (sp.isFuzzy) searchFuzzyRequest(request, sp, origin, marshall)
    else searchPostcodeRequest(request, sp, origin, marshall)
  }

  private[osgb] def searchUprnRequest[A](request: Request[A], uprn: String, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
    val unwantedQueryParams = request.queryString.filterKeys(k => k != UPRN).keys.toSeq

    if (unwantedQueryParams.nonEmpty) Future.successful {
      val paramList = unwantedQueryParams.mkString(", ")
      badRequest("BAD-PARAMETER", "origin" -> origin, "uprn" -> uprn, "error" -> s"unexpected query parameter(s): $paramList")

    } else
      addressSearch.findUprn(uprn).map {
        a =>
          val a2 = responseProcessor.convertAddressList(a)
          logEvent("LOOKUP", "origin" -> origin, "uprn" -> uprn, "matches" -> a2.size.toString)
          Ok(marshall(a2))
      }
  }

  private[osgb] def searchPostcodeRequest[A](request: Request[A], sp: SearchParameters, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
    val unwantedQueryParams = request.queryString.filterKeys(k => k != POSTCODE && k != FILTER).keys.toSeq

    if (unwantedQueryParams.nonEmpty) Future.successful {
      val paramList = unwantedQueryParams.mkString(", ")
      badRequest("BAD-PARAMETER", "origin" -> origin, "postcode" -> paramFromRequest(request, POSTCODE), "error" -> s"unexpected query parameter(s): $paramList")

    } else if (sp.postcode.isEmpty) Future.successful {
      badRequest("BAD-POSTCODE", "origin" -> origin, "error" -> s"missing or badly-formed $POSTCODE parameter")

    } else {
      addressSearch.findPostcode(sp.postcode.get, sp.filter).map {
        a =>
          val a2 = responseProcessor.convertAddressList(a)
          logEvent("LOOKUP", origin, a2.size, sp.tupled)
          Ok(marshall(a2))
      }
    }
  }

  private[osgb] def searchOutcodeRequest[A](request: Request[A], sp: SearchParameters, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
    val unwantedQueryParams = request.queryString.filterKeys(k => k != OUTCODE && k != FILTER).keys.toSeq

    if (unwantedQueryParams.nonEmpty) Future.successful {
      val paramList = unwantedQueryParams.mkString(", ")
      badRequest("BAD-PARAMETER", "origin" -> origin, "outcode" -> paramFromRequest(request, OUTCODE), "error" -> s"unexpected query parameter(s): $paramList")

    } else if (sp.outcode.isEmpty) Future.successful {
      badRequest("BAD-OUTCODE", "origin" -> origin, "error" -> s"missing or badly-formed $OUTCODE parameter")

    } else if (sp.filter.isEmpty) Future.successful {
      badRequest("BAD-FILTER", "origin" -> origin, "error" -> s"missing $FILTER parameter")

    } else {
      addressSearch.findOutcode(sp.outcode.get, sp.filter.get).map {
        a =>
          val a2 = responseProcessor.convertAddressList(a)
          logEvent("LOOKUP", origin, a2.size, sp.tupled)
          Ok(marshall(a2))
      }
    }
  }

  private[osgb] def searchFuzzyRequest[A](request: Request[A], sp: SearchParameters, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
    addressSearch.searchFuzzy(sp).map {
      a =>
        val a2 = responseProcessor.convertAddressList(a)
        logEvent("LOOKUP", origin, a2.size, sp.tupled)
        Ok(marshall(a2))
    }
  }

  private def paramFromRequest[A](request: Request[A], param: String): String = {
    request.getQueryString(param).getOrElse("None")
  }
}
