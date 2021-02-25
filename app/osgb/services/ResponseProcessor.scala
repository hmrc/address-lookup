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

package osgb.services

import javax.inject.Inject

import address.osgb.DbAddress
import address.v2.{AddressRecord, AddressRecordConverter}

class ResponseProcessor @Inject()(referenceData: ReferenceData) {

  private def convertAddress(dbAddress: DbAddress): AddressRecord = {
    val refItem = referenceData.get(dbAddress.localCustodianCode)
    AddressRecordConverter.convert(dbAddress, refItem)
  }

  def convertAddressList(dbAddresses: Seq[DbAddress]): List[AddressRecord] = {
    val sorted = dbAddresses.sortWith {
      (a, b) => DbAddressOrderingByLines.compare(a, b) < 0
    }
    sorted.map(a => convertAddress(a).truncatedAddress()).toList
  }
}


object DbAddressOrderingByLines extends Ordering[DbAddress] {
  def compare(a: DbAddress, b: DbAddress): Int = {
    val l1 = a.line1 compare b.line1
    if (l1 != 0) l1 else {
      val l2 = a.line2 compare b.line2
      if (l2 != 0) l2
      else a.line3 compare b.line3
    }
  }
}
