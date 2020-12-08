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
import doobie.util.fragment

import javax.inject.Inject
import osgb.SearchParameters
import osgb.services.AddressSearcher
import uk.gov.hmrc.address.osgb.DbAddress
import uk.gov.hmrc.address.uk.{Outcode, Postcode}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressLookupRepository @Inject()(transactor: Transactor[IO]) extends AddressSearcher {
  import AddressLookupRepository._

  override def findID(id: String): Future[Option[DbAddress]] = ???

  override def findUprn(uprn: String): Future[List[DbAddress]] = {
    val queryFragment = baseQuery ++
      sql""" WHERE uprn = ${cleanUprn(uprn).toInt}"""

    queryFragment.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture().map(l => l.map(mapToDbAddress))
  }

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] = {
    val queryFragment = baseQuery ++ sql""" WHERE postcode = ${postcode.toString}"""
    val queryFragmentWithFilter =
      filterOptToTsQueryOpt(filter).foldLeft(queryFragment) { case (a, f) => a ++ sql" AND " ++ f }

    println(queryFragmentWithFilter)

    queryFragmentWithFilter.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture()
      .map(l => l.map(mapToDbAddress))
  }

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] = {
    val queryFragment =
    baseQuery ++ sql""" WHERE postcode like ${outcode.toString + "%"} AND """ ++ filterToTsQuery(filter)

    queryFragment.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture()
      .map(l => l.map(mapToDbAddress))
  }

  override def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = ???

  private def cleanUprn(uprn: String): String = uprn.replaceFirst("^[Gg][Bb]", "")

  private def filterOptToTsQueryOpt(filterOpt: Option[String]): Option[fragment.Fragment] =
    filterOpt.map(filterToTsQuery)

  private def filterToTsQuery(filter: String): fragment.Fragment =
    sql"address_lookup_ft_col @@ to_tsquery(${filter.replace("""\p{Space}+""", " & ")})"

  private def mapToDbAddress(sqlDbAddress: SqlDbAddress): DbAddress = {
    DbAddress(
      sqlDbAddress.uprn,
      Seq(sqlDbAddress.line1, sqlDbAddress.line2, sqlDbAddress.line3)
        .collect { case l if l.isDefined & !l.get.isEmpty => l.get }.toList,
      sqlDbAddress.posttown,
      sqlDbAddress.postcode.getOrElse(""), //This should not be a problem as we are searching on a provided postcode so in practice this should exist.
      sqlDbAddress.subdivision,
      sqlDbAddress.countrycode,
      sqlDbAddress.localcustodiancode.map(_.toInt),
      sqlDbAddress.language,
      sqlDbAddress.blpustate.map(_.toInt),
      sqlDbAddress.logicalstatus.map(_.toInt),
      None,
      None,
      sqlDbAddress.location,
      sqlDbAddress.poboxnumber)
  }
}

object AddressLookupRepository {
  private val baseQuery =
    sql"""SELECT
         |uprn,
         |line1,
         |line2,
         |line3,
         |subdivision,
         |countrycode,
         |localcustodiancode,
         |language,
         |blpustate,
         |logicalstatus,
         |location,
         |posttown,
         |postcode,
         |poboxnumber,
         |localauthority
         |FROM address_lookup """.stripMargin

}

case class SqlDbAddress(uprn: String,
                        line1: Option[String],
                        line2: Option[String],
                        line3: Option[String],
                        subdivision: Option[String],
                        countrycode: Option[String],
                        localcustodiancode: Option[String],
                        language: Option[String],
                        blpustate: Option[String],
                        logicalstatus: Option[String],
                        location: Option[String],
                        posttown: Option[String],
                        postcode: Option[String],
                        poboxnumber: Option[String],
                        localauthority: Option[String])

