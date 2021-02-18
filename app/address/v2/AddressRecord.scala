/*
 * Copyright 2021 HM Revenue & Customs
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

package address.v2

import com.fasterxml.jackson.annotation.JsonIgnore
import address.v1


case class LocalCustodian(code: Int, name: String) {

  def asV1 = v1.LocalCustodian(code, name)
}


/**
  * Represents one address record. Arrays of these are returned from the address-lookup microservice.
  */
case class AddressRecord(
                          id: String,
                          uprn: Option[Long],
                          address: Address,
                          // ISO639-1 code, e.g. 'en' for English
                          // see https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
                          language: String,
                          localCustodian: Option[LocalCustodian],
                          location: Option[Seq[BigDecimal]],
                          blpuState: Option[String],
                          logicalState: Option[String],
                          streetClassification: Option[String],
                          administrativeArea: Option[String] = None) {

  require(location.isEmpty || location.get.size == 2, location.get)

  @JsonIgnore // needed because the name starts 'is...'
  def isValid: Boolean = address.isValid && language.length == 2

  def truncatedAddress(maxLen: Int = Address.maxLineLength): AddressRecord =
    if (address.longestLineLength <= maxLen) this
    else copy(address = address.truncatedAddress(maxLen))

  def withoutMetadata: AddressRecord = copy(blpuState = None, logicalState = None, streetClassification = None)

  def asV1 = v1.AddressRecord(id, uprn, address.asV1, localCustodian.map(_.asV1), language)

  def locationValue: Option[Location] = location.map(loc => Location(loc.head, loc(1)))

  def blpuStateValue: Option[BLPUState] = blpuState.flatMap(v => Option(BLPUState.valueOf(v)))

  def logicalStateValue: Option[LogicalState] = logicalState.flatMap(v => Option(LogicalState.valueOf(v)))

  def streetClassificationValue: Option[StreetClassification] = streetClassification.flatMap(v => Option(StreetClassification.valueOf(v)))
}
