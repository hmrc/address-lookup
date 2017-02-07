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

package bfpo

import javax.inject.Inject
import bfpo.outmodel.{BFPO, BFPOReadWrite}
import play.api.libs.json.Json
import play.api.mvc.{Action, Headers, Request, Result}
import uk.gov.hmrc.address.uk.Postcode
import uk.gov.hmrc.logging.{LoggerFacade, SimpleLogger}
import uk.gov.hmrc.play.http.Upstream4xxResponse
import uk.gov.hmrc.play.microservice.controller.BaseController

class BFPOLookupController @Inject() (bfpoData: List[BFPO], logger: SimpleLogger) extends BaseController {

  import BFPOReadWrite._

  def findByPostCode() = Action {
    request =>
      postcodeRequest(request)
  }

  def postcodeRequest[A](request: Request[A]): Result = {
    val origin = getOriginHeaderIfSatisfactory(request.headers)
    val bfpo = request.getQueryString(BFPO)
    val actualPostcode = request.getQueryString(POSTCODE)
    val cleanPostcode = actualPostcode.flatMap(Postcode.cleanupPostcode)
    val filter = request.getQueryString(FILTER)
    val unwantedQueryParams = request.queryString.keySet.diff(allowed)

    def filteredResponse(matches: List[BFPO], filter: Option[String], term: (String, String)): Result = {
      val filtered =
        if (filter.isEmpty) matches
        else matches.filter(_.anyLineContains(filter.get))
      logEvent("LOOKUP", origin, term, "matches" -> filtered.size.toString)
      Ok(Json.toJson(filtered))
    }

    if (unwantedQueryParams.nonEmpty) {
      val paramList = unwantedQueryParams.mkString(", ")
      badRequest("BAD-PARAMETER", origin,
        "postcode" -> actualPostcode.getOrElse("None"),
        "error" -> s"unexpected query parameter(s): $paramList")

    } else if (cleanPostcode.nonEmpty) {
      val matches = bfpoData.filter(_.postcode == cleanPostcode.get.toString)
      filteredResponse(matches, filter, "postcode" -> cleanPostcode.get.toString)

    } else if (bfpo.nonEmpty) {
      val matches = bfpoData.filter(_.bfpoNo == bfpo.get)
      filteredResponse(matches, filter, "bfpo" -> bfpo.get)

    } else {
      badRequest("BAD-POSTCODE", origin, "postcode" -> "None", "error" -> s"missing or badly-formed $POSTCODE parameter")
    }
  }

  private def logEvent(tag: String, origin: String, term: (String, String), outcome: (String, String)) {
    logger.info(tag + kv("origin" -> origin) + kv(term) + kv(outcome))
  }

  private def badRequest(status: String, origin: String, term: (String, String), msg: (String, String)): Result = {
    logEvent(status, origin, term, msg)
    BadRequest(msg._2)
  }

  private def getOriginHeaderIfSatisfactory(headers: Headers): String = {
    val userAgent = headers.get("User-Agent").getOrElse {
      throw Upstream4xxResponse("User-Agent header is required", 400, 400, Map())
    }
    if (userAgent.indexOf('/') >= 0) {
      // reject User-Agent set by default by frameworks, browsers etc
      throw Upstream4xxResponse(s"User-Agent header rejected: $userAgent", 400, 400, Map())
    }
    userAgent
  }

  private def kv(tuple: (String, String)): String = " " + tuple._1 + "=" + tuple._2

  private val BFPO = "bfpo"
  private val POSTCODE = "postcode"
  private val FILTER = "filter"

  private val allowed = Set(BFPO, POSTCODE, FILTER)
}
