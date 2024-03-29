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
import play.api.{Configuration, Environment}

import javax.inject.Singleton

@Singleton
class ConfigHelper @Inject() (val config: Configuration, env: Environment) {
  def mustGetConfigString(key: String): String = {
    getConfigString(key).getOrElse {
      throw new Exception("ERROR: Unable to find config item " + key)
    }
  }

  def mustGetConfigStringMode(key: String): String = {
    getConfigString(key).getOrElse {
      throw new Exception(s"ERROR: Unable to find config item $env.mode.$key or $key")
    }
  }

  def getConfigString(key: String): Option[String] = config.getOptional[String](key)

  def getConfigStringMode(key: String): Option[String] = {
    val modeKey = s"$env.mode.$key"
    config.getOptional[String](modeKey).orElse(config.getOptional[String](key))
  }

  def isCipPaasDbEnabled(): Boolean =
    getConfigString("cip-address-lookup-rds.enabled").getOrElse("false").toBoolean
}
