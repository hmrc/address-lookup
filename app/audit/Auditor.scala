/*
 * Copyright 2024 HM Revenue & Customs
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

package audit

import model.{
  AddressSearchAuditEvent,
  AddressSearchAuditEventMatchedAddress,
  AddressSearchAuditEventRequestDetails,
  NonUKAddressSearchAuditEvent,
  NonUKAddressSearchAuditEventMatchedAddress,
  NonUKAddressSearchAuditEventRequestDetails
}
import model.address.{AddressRecord, NonUKAddress, Postcode}
import model.request.UserAgent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class Auditor @Inject() (auditConnector: AuditConnector)(implicit
    ec: ExecutionContext
) {
  def auditAddressSearch[A](
      userAgent: Option[UserAgent],
      addressRecords: List[AddressRecord],
      postcode: Option[Postcode] = None,
      posttown: Option[String] = None,
      uprn: Option[String] = None,
      filter: Option[String] = None
  )(implicit hc: HeaderCarrier): Unit = {

    if (addressRecords.nonEmpty) {
      val auditEventRequestDetails = AddressSearchAuditEventRequestDetails(
        postcode.map(_.toString),
        posttown,
        uprn,
        filter
      )
      val addressSearchAuditEventMatchedAddresses = addressRecords.map { ma =>
        AddressSearchAuditEventMatchedAddress(
          ma.uprn.map(_.toString).getOrElse(""),
          ma.parentUprn,
          ma.usrn,
          ma.organisation,
          ma.address.lines,
          ma.address.town,
          ma.localCustodian,
          ma.location,
          ma.administrativeArea,
          ma.poBox,
          ma.address.postcode,
          ma.address.subdivision,
          ma.address.country
        )
      }

      auditConnector.sendExplicitAudit(
        "AddressSearch",
        AddressSearchAuditEvent(
          userAgent.map(_.unwrap),
          auditEventRequestDetails,
          addressRecords.length,
          addressSearchAuditEventMatchedAddresses
        )
      )
    }
  }

  def auditNonUKAddressSearch[A](
      userAgent: Option[UserAgent],
      nonUKAddresses: List[NonUKAddress],
      country: String,
      filter: Option[String] = None
  )(implicit hc: HeaderCarrier): Unit = {

    if (nonUKAddresses.nonEmpty) {
      auditConnector.sendExplicitAudit(
        "NonUKAddressSearch",
        NonUKAddressSearchAuditEvent(
          userAgent.map(_.unwrap),
          NonUKAddressSearchAuditEventRequestDetails(filter),
          nonUKAddresses.length,
          nonUKAddresses.map { ma =>
            NonUKAddressSearchAuditEventMatchedAddress(
              ma.id,
              ma.number,
              ma.street,
              ma.unit,
              ma.city,
              ma.district,
              ma.region,
              ma.postcode,
              country
            )
          }
        )
      )
    }
  }
}
