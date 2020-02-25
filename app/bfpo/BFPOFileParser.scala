/*
 * Copyright 2020 HM Revenue & Customs
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

package bfpo

import bfpo.outmodel.BFPO
import uk.gov.hmrc.address.services.CsvParser

object BFPOFileParser {

  def loadResource(file: String): List[BFPO] = {
    val it = CsvParser.splitResource(file)
    val data = for {
      line <- it
      if line.length > 2 && line(1).startsWith(expectedOutcode)
    } yield {
      val addressLines = line.drop(3).toList
      new BFPO(blankToOption(line(2)), addressLines, line(1), line(0))
    }
    data.toList
  }

  private def blankToOption(s: String): Option[String] = if (s == null || s.isEmpty) None else Some(s)

  val expectedOutcode = "BF1 "
}
