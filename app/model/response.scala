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

package model

import com.typesafe.config.Config
import play.api.ConfigLoader
import play.api.libs.json._

object response {

  import scala.collection.JavaConverters._

  case class SupportedCountryCodes(abp: List[String], nonAbp: List[String])

  object SupportedCountryCodes {

    implicit val configLoader: ConfigLoader[SupportedCountryCodes] = new ConfigLoader[SupportedCountryCodes] {
      override def load(config: Config, path: String): SupportedCountryCodes = {
        val cfg = config.getConfig(path)
        val abp = cfg.getStringList("abp").asScala.toList
        val nonAbp = cfg.getStringList("nonAbp").asScala.toList
        SupportedCountryCodes(abp = abp, nonAbp = nonAbp)
      }
    }

    implicit val writes: Writes[SupportedCountryCodes] = Json.writes[SupportedCountryCodes]
    implicit val reads: Reads[SupportedCountryCodes] = Json.reads[SupportedCountryCodes]
  }
}
