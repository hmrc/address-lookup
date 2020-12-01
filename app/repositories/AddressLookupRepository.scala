/*
 * Copyright 2020 HM Revenue & Customs
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

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import javax.inject.Inject
import osgb.SearchParameters
import osgb.services.AddressSearcher
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.uk.{Outcode, Postcode}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressLookupRepository @Inject()(transactor: Transactor[IO]) extends AddressSearcher {
  override def findID(id: String): Future[Option[DbAddress]] = ???

  override def findUprn(uprn: String): Future[List[DbAddress]] = ???

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] = {
    val query = sql"SELECT postcode FROM address_lookup where postcode = $postcode".query[SqlDbAddress]

    query.to[List].transact(transactor).unsafeToFuture().map {
      l => l.map(a => DbAddress(
        a.uprn,
        Seq(a.line1, a.line2, a.line3).flatten.toList,
        a.posttown,
        a.postcode,
        a.subivision,
        a.countrycode,
        a.localcustodiancode.map(_.toInt),
        a.language,
        a.blpustate.map(_.toInt),
        a.logicalstatus.map(_.toInt),
        None,
        None,
        a.location,
        a.poboxnumber))
    }
  }

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] = ???

  override def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = ???
}

case class SqlDbAddress(uprn: String, line1: Option[String], line2: Option[String], line3: Option[String],
                        subivision: Option[String], countrycode: Option[String], localcustodiancode: Option[String],
                        language: Option[String], blpustate: Option[String], logicalstatus: Option[String],
                        posttown: Option[String], postcode: String, location: Option[String],
                        poboxnumber: Option[String], localAuthority: Option[String])

