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

package access

import access.AccessChecker.{accessControlAllowListAbsoluteKey, accessControlAllowListKey, accessControlEnabledKey, accessRequestFormUrlKey}
import config.ConfigHelper
import org.slf4j.LoggerFactory
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
trait AccessChecker {
  this: BaseController =>

  val configHelper: ConfigHelper

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val accessRequestFormUrl: String = configHelper.mustGetConfigString(accessRequestFormUrlKey)

  private val checkAllowList: Boolean = configHelper.mustGetConfigString(accessControlEnabledKey).toBoolean
  private val allowedClients: Set[String] = configHelper.config.getOptional[Seq[String]](accessControlAllowListKey).getOrElse(
    if (checkAllowList) throw new RuntimeException(s"Could not find config $accessControlAllowListAbsoluteKey") else Seq()).toSet

  def isClientAllowed(client: Option[String]): Boolean =
    !checkAllowList || client.fold(false)(allowedClients.contains)

  def forbiddenResponse(client: Option[String]): String =
    s"""{
       |"code": 403,
       |"description": "'${client.getOrElse("Unknown Client")}' is not authorized to use BARS. Please complete '${accessRequestFormUrl}' to request access."
       |}""".stripMargin

  def getClientFromUserAgent[T](req: Request[T]): Option[String] = {
    req.headers.get("OriginatorId") match {
      case Some(oId) => logger.warn(s"An OriginatorId was provided: $oId")
      case _ =>
    }

    req.headers.get(HeaderNames.USER_AGENT)
      .flatMap(userAgent => userAgent.split(",").find(ua => ua != "bank-account-gateway"))
  }

  def accessCheckedAction[A](bodyParser: BodyParser[A])(block: Request[A] => Future[Result]): Action[A] = {
    Action.async(bodyParser) {
      request =>
        val callingClient = getClientFromUserAgent(request)
        if (!isClientAllowed(callingClient)) {
          Future.successful(Forbidden(Json.parse(forbiddenResponse(callingClient))))
        }
        else {
          block(request)
        }
    }
  }

}

object AccessChecker {
  val accessRequestFormUrlKey = "access-control.request.formUrl"
  val accessRequestFormUrlAbsoluteKey = s"microservice.services.$accessRequestFormUrlKey"
  val accessControlEnabledKey = "access-control.enabled"
  val accessControlEnabledAbsoluteKey = s"microservice.services.$accessControlEnabledKey"
  val accessControlAllowListKey = "access-control.allow-list"
  val accessControlAllowListAbsoluteKey = s"microservice.services.$accessControlAllowListKey"
}
