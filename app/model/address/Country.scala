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

package model.address

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.{maxLength, minLength}
import play.api.libs.json.{JsPath, Json, Reads, Writes}

/** Represents a country as per ISO3166. */
case class Country(
                    // ISO3166-1 or ISO3166-2 code, e.g. "GB" or "GB-ENG" (note that "GB" is the official
                    // code for UK although "UK" is a reserved synonym and may be used instead)
                    // See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
                    // and https://en.wikipedia.org/wiki/ISO_3166-2:GB
                    code: String,
                    // The printable name for the country, e.g. "United Kingdom"
                    name: String) {
}


object Country {
  object formats {
    implicit val countryReads: Reads[Country] = (
        (JsPath \ "code").read[String](minLength[String](2) keepAnd maxLength[String](6)) and
            (JsPath \ "name").read[String]) (Country.apply _)

    implicit val countryWrites: Writes[Country] = Json.writes[Country]
  }
  // note that "GB" is the official ISO code for UK, although "UK" is a reserved synonym and is less confusing
  @deprecated("GB is the official ISO code for UK", "")
  val UK = Country("UK", "United Kingdom")

  val GB = Country("GB", "United Kingdom") // special case provided for in ISO-3166
  val GG = Country("GG", "Guernsey")
  val IM = Country("IM", "Isle of Man")
  val JE = Country("JE", "Jersey")

  val England = Country("GB-ENG", "England")
  val Scotland = Country("GB-SCT", "Scotland")
  val Wales = Country("GB-WLS", "Wales")
  val Cymru = Country("GB-CYM", "Cymru")
  val NorthernIreland = Country("GB-NIR", "Northern Ireland")

  private val all = List(UK, GB, GG, IM, JE, England, Scotland, Wales, Cymru, NorthernIreland)
  def supported: List[Country] = all

  def find(code: String): Option[Country] = all.find(_.code == code)

  def findByName(name: String): Option[Country] = all.find(_.name == name)

  // TODO this is possibly not good enough - should consult a reference HMG-approved list of countries
}
