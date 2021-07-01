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

package osgb

import address.model.AddressRecord
import osgb.inmodel.LookupRequest
import osgb.outmodel.Marshall
import osgb.services._
import play.api.libs.json.JsValue
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressSearchController @Inject()(addressSearch: AddressSearcher, responseProcessor: ResponseProcessor,
                                        ec: ExecutionContext, cc: ControllerComponents)
    extends AddressController(cc) {

  implicit private val xec: ExecutionContext = ec

  import SearchParameters._

  def search(): Action[LookupRequest] = Action.async(parse.json[LookupRequest]) {
    request =>
      val lookupRequest = request.body
      val sp = SearchParameters(lookupRequest).clean
      processSearch(request, sp, Marshall.marshallV2List)
  }

  @deprecated
  def searchWithGet(): Action[AnyContent] = Action.async {
    request =>
      val sp = SearchParameters.fromQueryParameters(request.queryString).clean
      processSearch(request, sp, Marshall.marshallV2List)
  }

  private[osgb] def processSearch[A](request: Request[A], sp: SearchParameters, marshall: List[AddressRecord] => JsValue): Future[Result] = {
    val origin = getOriginHeaderIfSatisfactory(request.headers)
    if (sp.uprn.isDefined) searchByUprn(request, sp.uprn.get, origin, marshall)
    else if (sp.outcode.isDefined) searchByOutcode(request, sp, origin, marshall)
    else if (sp.isFuzzy) searchByFuzzy(request, sp, origin, marshall)
    else searchByPostcode(request, sp, origin, marshall)
  }

  private[osgb] def searchByUprn[A](request: Request[A], uprn: String, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
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

  private[osgb] def searchByPostcode[A](request: Request[A], sp: SearchParameters, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
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

  private[osgb] def searchByOutcode[A](request: Request[A], sp: SearchParameters, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
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

  private[osgb] def searchByFuzzy[A](request: Request[A], sp: SearchParameters, origin: String, marshall: List[AddressRecord] => JsValue): Future[Result] = {
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
