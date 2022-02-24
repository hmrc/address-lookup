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

package repositories

import model.address.{Outcode, Postcode}
import model.internal.DbAddress

import javax.inject.Inject
import scala.concurrent.Future

class InMemoryABPAddressRepository @Inject()() extends ABPAddressRepository {

  import InMemoryAddressTestData._

  override def findID(id: String): Future[List[DbAddress]] =
    Future.successful(dbAddresses.filter(_.id == id).toList)

  override def findUprn(uprn: String): Future[List[DbAddress]] =
    Future.successful(dbAddresses.filter(_.uprn == uprn.toLong).toList.take(3000))

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] =
    Future.successful(doFilter(dbAddresses.filter(_.postcode.equalsIgnoreCase(postcode.toString)), filter).toList.take(3000))

  override def findTown(town: String, filter: Option[String]): Future[List[DbAddress]] =
    Future.successful(doFilter(dbAddresses.filter(_.town.equalsIgnoreCase(town.toString)), filter).toList.take(3000))

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] =
    Future.successful(doFilter(dbAddresses.filter(_.postcode.toUpperCase.startsWith(outcode.toString.toUpperCase)), Some(filter)).toList)
}




