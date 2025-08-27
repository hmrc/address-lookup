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

package config

import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Singleton

@Singleton
class AppConfig @Inject() (
    val config: Configuration,
    servicesConfig: ServicesConfig
) {
  val appName = config.get[String]("appName")

  val addressSearchApiBaseUrl = servicesConfig.baseUrl("address-search-api")
  val addressSearchApiAuthToken = s"Basic $createAuth"

  def mustGetConfigString(key: String): String = {
    config.getOptional[String](key).getOrElse {
      throw new Exception("ERROR: Unable to find config item " + key)
    }
  }

  private def createAuth =
    AppConfig.createAuth(
      config.get[String]("appName"),
      servicesConfig.getConfString("addressSearchApiAuthToken", "invalid-token")
    )

  def isCipPaasDbEnabled: Boolean =
    config
      .getOptional[String]("cip-address-lookup-rds.enabled")
      .getOrElse("false")
      .toBoolean
}

object AppConfig {
  def createAuth(appName: String, authToken: String): String =
    Base64.getEncoder.encodeToString(
      s"$appName:$authToken".getBytes(StandardCharsets.UTF_8)
    )
}
