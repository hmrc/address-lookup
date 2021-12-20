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
import config.Capitalisation._
import controllers.services.AddressSearcher
import model.internal.{DbAddress, SqlDbAddress}
import model.address.{Outcode, Postcode}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressLookupRepository @Inject()(transactor: Transactor[IO]) extends AddressSearcher {

  import repositories.AddressLookupRepository._

  override def findID(id: String): IO[List[DbAddress]] = findUprn(cleanUprn(id))

  override def findUprn(uprn: String): IO[List[DbAddress]] =
    for {
      qf <- baseQueryWithWhere(("uprn", "=", uprn))
      l <- qf.query[SqlDbAddress].to[List].transact(transactor)
    } yield l.map(mapToDbAddress)

  override def findPostcode(postcode: Postcode, filter: Option[String]): IO[List[DbAddress]] =
    for {
      qf <- baseQueryWithWhere(("postcode", "=", postcode.toString))
      ff = filterOptToTsQueryOpt(filter).foldLeft(qf) {
        case (a, f) => a ++ sql" AND " ++ f
      }
      l <- ff.query[SqlDbAddress].to[List].transact(transactor)
    } yield l.map(mapToDbAddress)

  override def findTown(town: String, filter: Option[String]): IO[List[DbAddress]] = {
    for {
      qf <- baseQueryWithWhere(("posttown", "=", town.toUpperCase))
      ff = filterOptToTsQueryOpt(filter).foldLeft(qf) {
        case (a, f) => a ++ sql" AND " ++ f
      }
      l <- ff.query[SqlDbAddress].to[List].transact(transactor)
    } yield l.map(mapToDbAddress)
  }

  override def findOutcode(outcode: Outcode, filter: String): IO[List[DbAddress]] = {
    for {
      qf <- baseQueryWithWhere(("postcode", "like", s"${outcode.toString}%"))
      ff = filterToTsQuery(filter)
      l <- ff.query[SqlDbAddress].to[List].transact(transactor)
    } yield l.map(mapToDbAddress)
  }

  private def baseQueryWithWhere(whereKeyValues: (String, String, String)) = for {
    bq <- IO(baseQuery)
    sq <- IO(Fragment.const(s""" WHERE ${whereKeyValues._1} ${whereKeyValues._2} "${whereKeyValues._3}""""))
    qf <- IO(bq ++ sq)
  } yield qf

  private def cleanUprn(uprn: String): String = uprn.replaceFirst("^[Gg][Bb]", "")

  private def filterOptToTsQueryOpt(filterOpt: Option[String]): Option[fragment.Fragment] =
    filterOpt.map(filterToTsQuery)

  private def filterToTsQuery(filter: String): fragment.Fragment = {
    sql"""address_lookup_ft_col @@ plainto_tsquery('english', $filter)"""
  }

  private def mapToDbAddress(sqlDbAddress: SqlDbAddress): DbAddress = {
    DbAddress(
      s"GB${sqlDbAddress.uprn}", //To keep things in line with current output.
      Seq(
        sqlDbAddress.line1.map(normaliseAddressLine(_)),
        sqlDbAddress.line2.map(normaliseAddressLine(_)),
        sqlDbAddress.line3.map(normaliseAddressLine(_))
      ).collect { case l if l.isDefined & l.get.nonEmpty => l.get }.toList,
      sqlDbAddress.posttown.fold("")(normaliseAddressLine(_)),
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
