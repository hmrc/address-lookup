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

package model

import model.address.{Country, LocalCustodian}
import play.api.libs.json._
import play.api.libs.json.Json

case class AddressSearchAuditEventMatchedAddress(uprn: String,
                                                 parentUprn: Option[Long],
                                                 usrn: Option[Long],
                                                 organisation: Option[String],
                                                 lines: Seq[String],
                                                 town: String,
                                                 localCustodian: Option[LocalCustodian],
                                                 location: Option[Seq[BigDecimal]],
                                                 administrativeArea: Option[String],
                                                 poBox: Option[String],
                                                 postCode: String,
                                                 subDivision: Option[Country],
                                                 country: Country)

case class AddressSearchAuditEventRequestDetails(postcode: Option[String] = None, postTown: Option[String] = None,
                                                 filter: Option[String] = None)

case class AddressSearchAuditEvent(userAgent: Option[String],
                                   request: AddressSearchAuditEventRequestDetails,
                                   numberOfAddressFound: Int,
                                   matchedAddresses: Seq[AddressSearchAuditEventMatchedAddress])

object AddressSearchAuditEvent {
  import Country.formats._
  import LocalCustodian.formats._

  implicit def requestDetailsWrites = Json.writes[AddressSearchAuditEventRequestDetails]
  implicit def addressWrites = Json.writes[AddressSearchAuditEventMatchedAddress]
  implicit def writes = Json.writes[AddressSearchAuditEvent]
}
