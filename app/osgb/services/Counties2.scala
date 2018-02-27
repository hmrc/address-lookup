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

import uk.gov.hmrc.address.services.{Capitalisation, CsvParser}

import scala.collection.mutable

class Counties2(data: Map[String, String]) {
  def get(key: String) = data.get(key)
}

object Counties2 {
  def loadCounties(file: String): Counties2 = {
    val it = CsvParser.splitResource(file)
    val data = new mutable.HashMap[String, String]()
    while (it.hasNext) {
      val line = it.next()
      if (line.length >= 3) {
        if (!line(0).startsWith("LOCAL")) {
          val localCustodianCode = line(0)
          val county = line(2)
          data(localCustodianCode) = Capitalisation.normaliseAddressLine(county)
        }
      }
    }
    new Counties2(data.toMap)
  }
}
