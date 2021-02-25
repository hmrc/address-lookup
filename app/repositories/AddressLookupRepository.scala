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

package repositories

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment
import doobie.util.fragment.Fragment

import javax.inject.Inject
import osgb.SearchParameters
import osgb.services.AddressSearcher
import address.osgb.DbAddress
import address.services.Capitalisation._
import address.uk.{Outcode, Postcode}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressLookupRepository @Inject()(transactor: Transactor[IO], queryConfig: RdsQueryConfig) extends AddressSearcher {
  import AddressLookupRepository._

  override def findID(id: String): Future[Option[DbAddress]] = findUprn(cleanUprn(id)).map(_.headOption)

  override def findUprn(uprn: String): Future[List[DbAddress]] = {
    val queryFragment = baseQuery ++
      sql""" WHERE uprn = ${uprn.toLong}"""

    queryFragment.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture().map(l => l.map(mapToDbAddress))
  }

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] = {
    val queryFragment = baseQuery ++ sql""" WHERE postcode = ${postcode.toString}"""
    val queryFragmentWithFilter =
      filterOptToTsQueryOpt(filter).foldLeft(queryFragment) { case (a, f) =>
        a ++ sql" AND " ++ f
      }

    queryFragmentWithFilter.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture()
      .map(l => l.map(mapToDbAddress))
  }

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] = {
    val queryFragment =
      baseQuery ++ sql""" WHERE postcode like ${outcode.toString + "%"} AND """ ++ filterToTsQuery(filter)

    queryFragment.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture()
      .map(l => l.map(mapToDbAddress))
  }

  override def searchFuzzy(sp: SearchParameters): Future[List[DbAddress]] = {
    val filter = for {
      f <- Option(sp.lines).map(_.mkString(" ")).orElse(Some(""))
      t <- sp.town.orElse(Some(""))
      fz <- sp.fuzzy.orElse(Some(""))
      ff <- sp.filter.orElse(Some(""))
    } yield (f + " " + t + " " + fz + " " + ff).trim.replaceAll("\\p{Space}+", " ")

    if (sp.postcode.isDefined)
      findPostcode(sp.postcode.get, filter)
    else
      findWithOnlyFilter(filter)
  }

  private def findWithOnlyFilter(filter: Option[String]): Future[List[DbAddress]] = {
    val timeLimit = Fragment(s"SET statement_timeout=${queryConfig.queryTimeoutMillis}", List())
    val limitSql = Fragment(s" LIMIT ${queryConfig.queryResultsLimit}", List())

    val queryFragmentWithFilter =
      filterOptToTsQueryOpt(filter).foldLeft(baseQuery) { case (a, f) =>
        a ++ sql" WHERE " ++ f ++ limitSql
      }

    val toRun = for {
      _   <- timeLimit.update.run.transact(transactor)
      res <- queryFragmentWithFilter.query[SqlDbAddress].to[List].transact(transactor)
    } yield res
    toRun.unsafeToFuture().map(l => l.map(mapToDbAddress))
  }

  private def cleanUprn(uprn: String): String = uprn.replaceFirst("^[Gg][Bb]", "")

  private def filterOptToTsQueryOpt(filterOpt: Option[String]): Option[fragment.Fragment] =
    filterOpt.map(filterToTsQuery)

  private def filterToTsQuery(filter: String): fragment.Fragment = {
    sql"""address_lookup_ft_col @@ plainto_tsquery('english', ${filter})"""
  }

  private def mapToDbAddress(sqlDbAddress: SqlDbAddress): DbAddress = {
    DbAddress(
      "GB" + sqlDbAddress.uprn, //To keep things in line with current output.
      Seq(
        sqlDbAddress.line1.map(normaliseAddressLine(_)),
        sqlDbAddress.line2.map(normaliseAddressLine(_)),
        sqlDbAddress.line3.map(normaliseAddressLine(_))
      ).collect { case l if l.isDefined & !l.get.isEmpty => l.get }.toList,
      sqlDbAddress.posttown.map(normaliseAddressLine(_)),
      sqlDbAddress.postcode.getOrElse(""), //This should not be a problem as we are searching on a provided postcode so in practice this should exist.
      sqlDbAddress.subdivision,
      sqlDbAddress.countrycode,
      sqlDbAddress.localcustodiancode.map(_.toInt),
      sqlDbAddress.language,
      None,
      sqlDbAddress.location,
      sqlDbAddress.poboxnumber,
      sqlDbAddress.localauthority)
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
                        location: Option[String],
                        posttown: Option[String],
                        postcode: Option[String],
                        poboxnumber: Option[String],
                        localauthority: Option[String])

