/*
 * Copyright 2017 HM Revenue & Customs
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

package osgb.outmodel.v2

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.address.v2._

object AddressWriteable {

  // https://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators

  implicit val CountryWrites: Writes[Country] = Json.writes[Country]

  implicit val AddressWrites: Writes[Address] = (
    (JsPath \ "lines").write[Seq[String]] and
      (JsPath \ "town").writeNullable[String] and
      (JsPath \ "county").writeNullable[String] and
      (JsPath \ "postcode").write[String] and
      (JsPath \ "subdivision").writeNullable[Country] and
      (JsPath \ "country").write[Country]) (unlift(Address.unapply))

  implicit val LocalCustodianWrites: Writes[LocalCustodian] = (
    (JsPath \ "code").write[Int] and
      (JsPath \ "name").write[String]) (unlift(LocalCustodian.unapply))

  implicit val AddressRecordWrites: Writes[AddressRecord] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "uprn").writeNullable[Long] and
      (JsPath \ "address").write[Address] and
      (JsPath \ "language").write[String] and
      (JsPath \ "localCustodian").writeNullable[LocalCustodian] and
      (JsPath \ "location").writeNullable[Seq[BigDecimal]] and
      (JsPath \ "blpuState").writeNullable[String] and
      (JsPath \ "logicalState").writeNullable[String] and
      (JsPath \ "streetClassification").writeNullable[String]
  ) (unlift(AddressRecord.unapply))

}
