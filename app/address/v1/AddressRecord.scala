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

package address.v1

case class LocalCustodian(code: Int, name: String)


/**
  * Represents one address record. Arrays of these are returned from the address-lookup microservice.
  */
case class AddressRecord(
                          id: String,
                          uprn: Option[Long],
                          address: Address,
                          localCustodian: Option[LocalCustodian],
                          // ISO639-1 code, e.g. 'en' for English
                          // see https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
                          language: String) {

  def isValid = address.isValid && language.length == 2

  def truncatedAddress(maxLen: Int = Address.maxLineLength) =
    if (address.longestLineLength <= maxLen) this
    else AddressRecord(id, uprn, address.truncatedAddress(maxLen), localCustodian, language)
}
