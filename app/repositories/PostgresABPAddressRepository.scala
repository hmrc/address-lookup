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

package repositories

import cats.effect.IO
import config.Capitalisation._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment
import model.address.{Outcode, Postcode}
import model.internal.{DbAddress, SqlDbAddress}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostgresABPAddressRepository @Inject()(transactor: Transactor[IO], queryConfig: RdsQueryConfig)(implicit ec: ExecutionContext) extends ABPAddressRepository {

  import PostgresABPAddressRepository._

  override def findID(id: String): Future[List[DbAddress]] = findUprn(cleanUprn(id))

  override def findUprn(uprn: String): Future[List[DbAddress]] = {
    val queryFragment = baseQuery ++
      sql""" WHERE uprn = ${uprn.toLong}"""

    queryFragment.query[SqlDbAddress].to[List].transact(transactor).unsafeToFuture()
      .map(l => l.map(mapToDbAddress))
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

  override def findTown(town: String, filter: Option[String]): Future[List[DbAddress]] = {
    val queryFragment = baseQuery ++ sql""" WHERE posttown = ${town.toUpperCase}"""
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

  private def cleanUprn(uprn: String): String = uprn.replaceFirst("^[Gg][Bb]", "")

  private def filterOptToTsQueryOpt(filterOpt: Option[String]): Option[fragment.Fragment] =
    filterOpt.map(filterToTsQuery)

  private def filterToTsQuery(filter: String): fragment.Fragment = {
    sql"""address_lookup_ft_col @@ plainto_tsquery('english', ${filter})"""
  }

  private def mapToDbAddress(sqlDbAddress: SqlDbAddress): DbAddress = {
    DbAddress("GB" + sqlDbAddress.uprn,
      sqlDbAddress.uprn.toLong,
      sqlDbAddress.parentUprn.map(_.toLong),
      sqlDbAddress.usrn.map(_.toLong),
      sqlDbAddress.orgnisationName,
      Seq(sqlDbAddress.line1.map(normaliseAddressLine(_)),
            sqlDbAddress.line2.map(normaliseAddressLine(_)),
            sqlDbAddress.line3.map(normaliseAddressLine(_))
      ).collect {
        case l if l.isDefined & l.get.nonEmpty => l.get }
       .toList,
      sqlDbAddress.posttown.map(normaliseAddressLine(_)).getOrElse(""),
      sqlDbAddress.postcode.getOrElse(""),
      sqlDbAddress.subdivision,
      sqlDbAddress.countryCode,
      sqlDbAddress.localCustodianCode.map(_.toInt),
      sqlDbAddress.language,
      None,
      sqlDbAddress.location,
      sqlDbAddress.poBoxNumber,
      sqlDbAddress.localAuthority)
  }
}

object PostgresABPAddressRepository {
  private val baseQuery =
    sql"""SELECT
         |uprn,
         |parent_uprn,
         |usrn,
         |organisation_name,
         |line1,
         |line2,
         |line3,
         |subdivision,
         |country_code,
         |local_custodian_code,
         |language,
         |location,
         |posttown,
         |postcode,
         |pobox_number,
         |local_authority
         |FROM address_lookup """.stripMargin

}
