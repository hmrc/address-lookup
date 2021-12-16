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

import com.typesafe.config.ConfigFactory
import play.api.Logger
import play.api.mvc.{ControllerComponents, Headers, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.http.UpstreamErrorResponse

abstract class AddressController(cc: ControllerComponents) extends BackendController(cc) {

  private val logger = Logger(this.getClass.getSimpleName)

  lazy private val headerOrigin:String =  ConfigFactory.load().getString("header.x-origin")

  protected final def getOriginHeaderIfSatisfactory(headers: Headers): String = {
    val hmrcOrigin = headers.get(headerOrigin)
    if (hmrcOrigin.isDefined) {
      hmrcOrigin.get
    }
    else {
      val userAgent = headers.get("User-Agent").getOrElse {
        throw UpstreamErrorResponse(s"User-Agent or $headerOrigin header is required", 400, 400, Map())
      }
      if (userAgent.indexOf('/') >= 0) {
        // reject User-Agent set by default by frameworks, browsers etc
        throw UpstreamErrorResponse(s"User-Agent or $headerOrigin header rejected: $userAgent", 400, 400, Map())
      }
      userAgent
    }
  }

  protected final def badRequest(tag: String, data: (String, String)*): Result = {
    logEvent(tag, data: _*)
    BadRequest(keyVal(data.last))
  }

  protected final def logEvent(tag: String, data: (String, String)*): Unit = {
    val formatted = data.map(keyVal).mkString(" ")
    logger.info(s"$tag $formatted")
  }

  protected final def logEvent(tag: String, origin: String, matches: Int, data: List[(String, String)]): Unit = {
    logEvent(tag, ("origin" -> origin) :: ("matches" -> matches.toString) :: data: _*)
  }

  private def keyVal(item: (String, String)) = s"${item._1}=${item._2}"
}
