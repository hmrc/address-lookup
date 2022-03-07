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

import play.api.libs.json._
import play.api.libs.json.Json

case class NonUKAddressSearchAuditEventMatchedAddress(id: String,
                                                      number: Option[String],
                                                      street: Option[String],
                                                      unit: Option[String],
                                                      city: Option[String],
                                                      district: Option[String],
                                                      region: Option[String],
                                                      postCode: Option[String],
                                                      country: String)

case class NonUKAddressSearchAuditEventRequestDetails(filter: Option[String])

case class NonUKAddressSearchAuditEvent(userAgent: Option[String],
                                        request: NonUKAddressSearchAuditEventRequestDetails,
                                        numberOfAddressFound: Int,
                                        matchedAddresses: Seq[NonUKAddressSearchAuditEventMatchedAddress])

object NonUKAddressSearchAuditEvent {

  implicit def requestDetailsWrites: Writes[NonUKAddressSearchAuditEventRequestDetails] = Json.writes[NonUKAddressSearchAuditEventRequestDetails]

  implicit def addressWrites: Writes[NonUKAddressSearchAuditEventMatchedAddress] = Json.writes[NonUKAddressSearchAuditEventMatchedAddress]

  implicit def writes: Writes[NonUKAddressSearchAuditEvent] = Json.writes[NonUKAddressSearchAuditEvent]
}
