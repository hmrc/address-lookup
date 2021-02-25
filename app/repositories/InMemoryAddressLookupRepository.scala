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

import address.osgb.DbAddress
import address.services.CsvLineSplitter
import address.uk.{Outcode, Postcode}
import address.v2.Location
import osgb.SearchParameters
import osgb.services.{AddressSearcher, CSV}
import play.api.Environment

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class InMemoryAddressLookupRepository @Inject()(env: Environment, ec: ExecutionContext) extends AddressSearcher {
  val cannedData = "conf/data/testaddresses.csv"

  lazy val dbAddresses = {
    val file = env.getExistingFile(cannedData).getOrElse {
      throw new Exception("Missing " + cannedData)
    }

    val splitter = new CsvLineSplitter(Source.fromFile(file).bufferedReader()).asScala.toSeq
    splitter.map(CSV.convertCsvLine)
  } ++ Seq(
    DbAddress("GB11111", List("A House 27-45", "A Street"), Some("London"), "FX9 9PY", Some("GB-ENG"), Some("UK"),
      Some(5840), Some("en"), None, Some(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB33333", List("A House 5-7", "A Boulevard"), Some("Newcastle upon Tyne"), "FX1 6JN", Some("GB-ENG"),
      Some("UK"), Some(4510), Some("en"), None, Some(Location("12.345678", "-12.345678")
        .toString)),
    DbAddress("GB44444", List("An address with a very long first line", "Second line of address is just as long maybe" +
      " longer", "Third line is not the longest but is still very long"), Some
    ("Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch"), "FX2 2TB", Some("GB-WLS"), Some("UK"), Some(915),
      Some("en"), None, Some(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB55555", List("An address with a PO Box"), None, "FX17 1TB", Some("GB-WLS"), Some("UK"), Some(666),
      Some("en"), None, Some(Location("12.345678", "-12.345678").toString), Some("PO Box " +
        "1234")),
    DbAddress("GB22222", List("11 A Boulevard"), Some("Newcastle upon Tyne"), "FX1 6JN", Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), None, Some(Location("12.345678", "-12.345678").toString))) ++ (for (i <- 1 to 2517) yield {
    DbAddress(s"GB100$i", List(s"Flat $i", "A Apartments", "ARoad"), Some("ATown"), "FX4 7AL", Some("GB-ENG"), Some("UK"), Some(3725), Some("en"), None, None)
  }) ++ (for (i <- 1 to 3001) yield {
    DbAddress(s"GB200$i", List(s"$i Bankside"), Some("ATown"), "FX4 7AJ",
      Some("GB-ENG"), Some("UK"), Some(3725), Some("en"), None, None)
  })


  private def dbsToFilterText(dbAddress: DbAddress): Set[String] =
    (dbAddress.lines.mkString(" ") + " " + dbAddress.town.getOrElse("")).replaceAll("[\\p{Space},]+", " ").split(" ").map(_.toLowerCase).toSet

  override def findID(id: String): Future[Option[DbAddress]] =
    Future.successful(dbAddresses.find(_.id == id))

  override def findUprn(uprn: String): Future[List[DbAddress]] =
    Future.successful(dbAddresses.filter(_.uprn == uprn.toLong).toList.take(3000))

  override def findPostcode(postcode: Postcode, filter: Option[String]): Future[List[DbAddress]] =
    Future.successful(doFilter(dbAddresses.filter(_.postcode == postcode.toString), filter).toList.take(3000))

  override def findOutcode(outcode: Outcode, filter: String): Future[List[DbAddress]] =
    Future.successful(doFilter(dbAddresses.filter(_.postcode.startsWith(outcode.toString)), Some(filter)).toList)

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
      Future.successful { doFilter(dbAddresses, filter).toList.take(3000) }
  }

  private def doFilter(filteredDbAddressStr: Seq[DbAddress], filter: Option[String]): Seq[DbAddress] = {
    val filterTokens =
      filter.map(_.toLowerCase.split("[ ]+")).map(_.toSet.filterNot(_.isEmpty)).getOrElse(Set())
    filteredDbAddressStr.filter { dba =>
      val dbAddString = dbsToFilterText(dba)
      filterTokens.subsetOf(dbAddString)
    }
  }
}
