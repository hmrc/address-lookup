/*
 * Copyright 2018 HM Revenue & Customs
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

package osgb.services

import osgb.outmodel._
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.services.Capitalisation._
import uk.gov.hmrc.address.uk.Postcode

object CSV {
  def convertCsvLine(line: Array[String]): DbAddress = {
    if (line.length < 6) {
      throw new RuntimeException("Short input line: " + line.mkString)
    } else if (line.length > 9) {
      throw new RuntimeException("Excessive input line: " + line.mkString)
    } else {
      val uprn = trim(line(0))
      val line1 = normaliseAddressLine(removeTrailingCommaAndTrim(line(1)))
      val line2 = normaliseAddressLine(removeTrailingCommaAndTrim(line(2)))
      val line3 = normaliseAddressLine(removeTrailingCommaAndTrim(line(3)))
      val line4 = normaliseAddressLine(removeTrailingCommaAndTrim(line(4)))
      val lines = List(line1, line2, line3).filterNot(_.isEmpty)
      val postcode = Postcode.normalisePostcode(trim(line(5)))
      val subdivision = if (line.length > 6) blankToOption(trim(line(6))) else None
      val lcc = if (line.length > 7) blankToOption(trim(line(7))).map(_.toInt) else None
      val poBox = if (line.length > 8) blankToOption(trim(line(8))) else None
      DbAddress(prefixedId(uprn), lines, blankToOption(line4), postcode, subdivision, Some("UK"), lcc, Some("en"), None, None, None, None, None, poBox)
    }
  }

  private def prefixedId(id: String) = if (id.startsWith("GB")) id else "GB" + id
}
