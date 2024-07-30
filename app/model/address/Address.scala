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

import com.fasterxml.jackson.annotation.JsonIgnore
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

import java.util.regex.Pattern

/** Address typically represents a postal address. For UK addresses, 'town' will
  * always be present. For non-UK addresses, 'town' may be absent and there may
  * be an extra line instead.
  */
case class Address(
    lines: List[String],
    town: String,
    postcode: String,
    subdivision: Option[Country],
    country: Country
) {

  import Address._

  @JsonIgnore // needed because the name starts 'is...'
  def isValid: Boolean =
    lines.nonEmpty && lines.size <= (if (town.isEmpty) 4 else 3)

  def nonEmptyFields: List[String] = lines ::: List(town) ::: List(postcode)

  /** Gets a conjoined representation, excluding the country. */
  def printable(separator: String): String = nonEmptyFields.mkString(separator)

  /** Gets a single-line representation, excluding the country. */
  @JsonIgnore // needed because it's a field
  lazy val printable: String = printable(", ")

  def line1: String = if (lines.nonEmpty) lines.head else ""

  def line2: String = if (lines.size > 1) lines(1) else ""

  def line3: String = if (lines.size > 2) lines(2) else ""

  def line4: String = if (lines.size > 3) lines(3) else ""

  def longestLineLength: Int = nonEmptyFields.map(_.length).max

  def truncatedAddress(maxLen: Int = maxLineLength): Address =
    Address(
      lines.map(limit(_, maxLen)),
      limit(town, maxLen),
      postcode,
      subdivision,
      country
    )
}

object Address {
  val maxLineLength = 35
  val danglingLetter: Pattern = Pattern.compile(".* [A-Z0-9]$")

  private[model] def limit(str: String, max: Int): String = {
    var s = str
    while (s.length > max && s.indexOf(", ") > 0) {
      s = s.replaceFirst(", ", ",")
    }
    if (s.length > max) {
      s = s.substring(0, max).trim
      if (Address.danglingLetter.matcher(s).matches()) {
        s = s.substring(0, s.length - 2)
      }
      s
    } else s
  }

  object formats {
    import Country.formats._

    implicit val addressReads: Reads[Address] =
      ((JsPath \ "lines").read[List[String]] and
        (JsPath \ "town").read[String] and
        (JsPath \ "postcode").read[String] and
        (JsPath \ "subdivision").readNullable[Country] and
        (JsPath \ "country").read[Country])(Address.apply _)

    implicit val addressWrites: Writes[Address] =
      ((JsPath \ "lines").write[Seq[String]] and
        (JsPath \ "town").write[String] and
        (JsPath \ "postcode").write[String] and
        (JsPath \ "subdivision").writeNullable[Country] and
        (JsPath \ "country").write[Country])(unlift(Address.unapply))

  }
}
