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

case class LocalCustodian(code: Int, name: String)

object LocalCustodian {
  object formats {
    implicit val localCustodianReads: Reads[LocalCustodian] =
      ((JsPath \ "code").read[Int] and
        (JsPath \ "name").read[String])(LocalCustodian.apply _)

    implicit val localCustodianWrites: Writes[LocalCustodian] =
      ((JsPath \ "code").write[Int] and
        (JsPath \ "name").write[String])(unlift(LocalCustodian.unapply))

  }
}

/** Represents one address record. Arrays of these are returned from the
  * address-lookup microservice.
  */
case class AddressRecord(
    id: String,
    uprn: Option[Long],
    parentUprn: Option[Long],
    usrn: Option[Long],
    organisation: Option[String],
    address: Address,
    // ISO639-1 code, e.g. 'en' for English
    // see https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
    language: String,
    localCustodian: Option[LocalCustodian],
    location: Option[Seq[BigDecimal]],
    administrativeArea: Option[String] = None,
    poBox: Option[String] = None
) {

  require(location.isEmpty || location.get.size == 2, location.get)

  @JsonIgnore // needed because the name starts 'is...'
  def isValid: Boolean = address.isValid && language.length == 2

  def truncatedAddress(maxLen: Int = Address.maxLineLength): AddressRecord =
    if (address.longestLineLength <= maxLen) this
    else copy(address = address.truncatedAddress(maxLen))
}

object AddressRecord {
  object formats {
    import Address.formats._
    import LocalCustodian.formats._

    implicit val addressRecordReads: Reads[AddressRecord] = (
      (JsPath \ "id").read[String] and
        (JsPath \ "uprn").readNullable[Long] and
        (JsPath \ "parentUprn").readNullable[Long] and
        (JsPath \ "usrn").readNullable[Long] and
        (JsPath \ "organisation").readNullable[String] and
        (JsPath \ "address").read[Address] and
        (JsPath \ "language").read[String] and
        (JsPath \ "localCustodian").readNullable[LocalCustodian] and
        (JsPath \ "location").readNullable[Seq[BigDecimal]] and
        (JsPath \ "administrativeArea").readNullable[String] and
        (JsPath \ "poBox").readNullable[String]
    )(AddressRecord.apply _)

    implicit val addressRecordWrites: Writes[AddressRecord] = (
      (JsPath \ "id").write[String] and
        (JsPath \ "uprn").writeNullable[Long] and
        (JsPath \ "parentUprn").writeNullable[Long] and
        (JsPath \ "usrn").writeNullable[Long] and
        (JsPath \ "organisation").writeNullable[String] and
        (JsPath \ "address").write[Address] and
        (JsPath \ "language").write[String] and
        (JsPath \ "localCustodian").writeNullable[LocalCustodian] and
        (JsPath \ "location").writeNullable[Seq[BigDecimal]] and
        (JsPath \ "administrativeArea").writeNullable[String] and
        (JsPath \ "poBox").writeNullable[String]
    )(unlift(AddressRecord.unapply))

  }
}
