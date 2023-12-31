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

import play.api.Logger
import play.api.mvc.{ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

abstract class AddressController(cc: ControllerComponents) extends BackendController(cc) {

  private val logger = Logger(this.getClass.getSimpleName)

  protected final def badRequest(tag: String, data: (String, String)*): Result = {
    logEvent(tag, data: _*)
    BadRequest(keyVal(data.last))
  }

  protected final def notFound(tag: String, data: (String, String)*): Result = {
    logEvent(tag, data: _*)
    NotFound(keyVal(data.last))
  }

  protected final def logEvent(tag: String, data: (String, String)*): Unit = {
    val formatted = data.map(keyVal).mkString(" ")
    logger.info(s"$tag $formatted")
  }

  protected final def logEvent(tag: String, matches: Int, data: List[(String, String)]): Unit = {
    logEvent(tag, ("matches" -> matches.toString) :: data: _*)
  }

  private def keyVal(item: (String, String)) = item._1 + "=" + item._2
}
