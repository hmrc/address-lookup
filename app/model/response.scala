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
        val nonAbp = cfg.getStringList("non-abp").asScala.toList
        SupportedCountryCodes(abp = abp, nonAbp = nonAbp)
      }
    }

    implicit val config = JsonConfiguration(KebabCase)
    implicit val writes: Writes[SupportedCountryCodes] = Json.writes[SupportedCountryCodes]
  }

  object KebabCase extends JsonNaming {
    def apply(property: String): String = {
      val length            = property.length
      val result            = new StringBuilder(length * 2)
      var resultLength      = 0
      var wasPrevTranslated = false

      for (i <- 0.until(length)) {
        var c = property.charAt(i)
        if (i > 0 || c != '_') {
          if (Character.isUpperCase(c)) {
            // append a underscore if the previous result wasn't translated
            if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '-') {
              result.append('-')
              resultLength += 1
            }
            c = Character.toLowerCase(c)
            wasPrevTranslated = true
          } else {
            wasPrevTranslated = false
          }
          result.append(c)
          resultLength += 1
        }
      }

      // builds the final string
      result.toString()
    }

    override val toString = "KebabCase"
  }

}
