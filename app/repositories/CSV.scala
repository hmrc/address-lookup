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

package repositories

import config.Capitalisation.normaliseAddressLine
import model.address.Postcode
import model.internal.{DbAddress, NonUKAddress}
import util._

object CSV {
  def convertCsvLine(line: Array[String]): DbAddress = {
    if (line.length < 9) {
      throw new RuntimeException("Short input line: " + line.mkString)
    } else if (line.length > 12) {
      throw new RuntimeException("Excessive input line: " + line.mkString)
    } else {
      val uprn = trim(line(0)).toLong
      val id = prefixedId(s"$uprn")
      val parentUprn = Option(trim(line(1))).flatMap(pu => if(pu.isEmpty) None else Some(pu)).map(_.toLong)
      val usrn = Option(trim(line(2))).flatMap(pu => if(pu.isEmpty) None else Some(pu)).map(_.toLong)
      val organisationName = Option(trim(line(3))).flatMap(pu => if(pu.isEmpty) None else Some(pu))
      val line1 = normaliseAddressLine(removeTrailingCommaAndTrim(line(4)))
      val line2 = normaliseAddressLine(removeTrailingCommaAndTrim(line(5)))
      val line3 = normaliseAddressLine(removeTrailingCommaAndTrim(line(6)))
      val line4 = normaliseAddressLine(removeTrailingCommaAndTrim(line(7)))
      val lines = List(line1, line2, line3).filterNot(_.isEmpty)
      val postcode = Postcode.normalisePostcode(trim(line(8)))
      val subdivision = if (line.length > 9) blankToOption(trim(line(9))) else None
      val lcc = if (line.length > 10) blankToOption(trim(line(10))).map(_.toInt) else None
      val poBox = if (line.length > 11) blankToOption(trim(line(11))) else None
      DbAddress(id,uprn,parentUprn,usrn,organisationName, lines, line4, postcode, subdivision, Some("GB"), lcc, Some("en"), None, None, poBox)
    }
  }

  def convertNonUKCsvLine(line: Array[String]): (String, NonUKAddress) = {
    if (line.length != 10) {
      throw new RuntimeException("Incorrect input line: " + line.mkString)
    } else {
      val country = line(9)
      val id: String = line(8)
      val number: Option[String] = line(1).emptyToNone
      val street: Option[String] = line(2).emptyToNone
      val unit: Option[String] = line(3).emptyToNone
      val city: Option[String] = line(4).emptyToNone
      val district: Option[String] = line(5).emptyToNone
      val region: Option[String] = line(6).emptyToNone
      val postcode: Option[String] = line(7).emptyToNone
      (country, NonUKAddress(id, number, street, unit, city, district, region, postcode))
    }
  }

  private def prefixedId(id: String) = if (id.startsWith("GB")) id else "GB" + id

  def blankToOption(s: String): Option[String] = if (s == null || s.isEmpty) None else Some(s)

  def trim(s: String): String = if (s == null) null else s.trim

  def removeTrailingCommaAndTrim(s: String): String = {
    val t = trim(s)
    if (t == null) null
    else if (t.endsWith(",")) t.init.trim
    else t
  }
}
