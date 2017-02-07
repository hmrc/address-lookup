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
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.address.v2._

object AddressReadable {

  implicit val CountryReads: Reads[Country] = (
    (JsPath \ "code").read[String](minLength[String](2) keepAnd maxLength[String](6)) and
      (JsPath \ "name").read[String]) (Country.apply _)

  implicit val AddressReads: Reads[Address] = (
    (JsPath \ "lines").read[List[String]] and
      (JsPath \ "town").readNullable[String] and
      (JsPath \ "county").readNullable[String] and
      (JsPath \ "postcode").read[String] and
      (JsPath \ "subdivision").readNullable[Country] and
      (JsPath \ "country").read[Country]) (Address.apply _)

  implicit val LocalCustodianReads: Reads[LocalCustodian] = (
    (JsPath \ "code").read[Int] and
      (JsPath \ "name").read[String]) (LocalCustodian.apply _)

  implicit val AddressRecordReads: Reads[AddressRecord] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "uprn").readNullable[Long] and
      (JsPath \ "address").read[Address] and
      (JsPath \ "language").read[String] and
      (JsPath \ "localCustodian").readNullable[LocalCustodian] and
      (JsPath \ "location").readNullable[Seq[BigDecimal]] and
      (JsPath \ "blpuState").readNullable[String] and
      (JsPath \ "logicalState").readNullable[String] and
      (JsPath \ "streetClassification").readNullable[String]
    ) (AddressRecord.apply _)

}
