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

package services

import address.osgb.DbAddress
import address.uk.{Outcode, Postcode}
import osgb.SearchParameters
import osgb.services.AddressSearcher

import javax.inject.Inject
import scala.concurrent.Future

class AddressLookupService @Inject()(addressSearcher: AddressSearcher) extends AddressSearcher {
  override def findID(id: String): Future[Option[DbAddress]] = addressSearcher.findID(id)

  override def findUprn(uprn: String): Future[List[DbAddress]] = addressSearcher.findUprn(uprn)

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] = addressSearcher.findPostcode(postcode)

  override def findTown(town: String, filter: Option[String]): Future[List[DbAddress]] = addressSearcher.findTown(town, filter)

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] = addressSearcher.findOutcode(outcode, filter)

  override def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = addressSearcher.searchFuzzy(sp)
}