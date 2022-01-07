/*
 * Copyright 2022 HM Revenue & Customs
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

package apiplatform

import config.ConfigHelper
import controllers.Assets
import javax.inject.Inject
import play.api.http.{ContentTypes, MimeTypes}
import play.api.mvc.{Action, AnyContent, Codec, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

class DocumentationController @Inject()(assets: Assets, cc: ControllerComponents, configHelper: ConfigHelper) extends BackendController(cc) {
  private val apiStatus = configHelper.mustGetConfigString("api-platform.status")

  def definition(): Action[AnyContent] = Action.async {
    Future.successful(Ok(txt.definition(apiStatus)).as(ContentTypes.withCharset(MimeTypes.JSON)(Codec.utf_8)))
  }

  def raml(version: String, file: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", file)
  }
}