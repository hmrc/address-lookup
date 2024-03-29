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

import model.internal.DbAddress


case class ReferenceItem(code: Int, localCustodian: String)


object AddressRecordConverter {

  def convert(d: DbAddress, refItem: Option[ReferenceItem]): AddressRecord = {
    val optLC = refItem.map(it => LocalCustodian(it.code, it.localCustodian))

    val optSubdivision = d.subdivision.flatMap(code => Country.find(code))

    val country = if (d.country.isDefined) Country.find(d.country.get).getOrElse(Country.GB) else Country.GB

    val language = d.language.getOrElse(English)

    val location = d.location.map(latlong => Location(latlong).toSeq)

    val a = new Address(d.lines, d.town, d.postcode, optSubdivision, country)

    AddressRecord(d.id, Some(d.uprn), d.parentUprn, d.usrn, d.organisationName, a, language, optLC, location, d.administrativeArea, d.poBox)
  }

  final val English = "en"
}
