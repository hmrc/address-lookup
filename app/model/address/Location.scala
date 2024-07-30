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

import util._

case class Location(latitude: BigDecimal, longitude: BigDecimal) {

  def toSeq: Seq[BigDecimal] = Seq(latitude, longitude)

  override def toString: String = s"${latitude},${longitude}"
}

object Location {
  def apply(lat: String, long: String): Location =
    new Location(BigDecimal(lat), BigDecimal(long))

  def apply(latlong: String): Location = {
    val seq = latlong.divide(',')
    apply(seq.head.trim, seq(1).trim)
  }
}
