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

import controllers.services.AddressSearcher
import model.address.{Location, Outcode, Postcode}
import model.internal.DbAddress
import play.api.Environment

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class InMemoryAddressLookupRepository @Inject()(env: Environment, ec: ExecutionContext) extends AddressSearcher {
  import InMemoryAddressLookupRepository._

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

object InMemoryAddressLookupRepository {
  val singleAddresses: Seq[DbAddress] = Seq(
    DbAddress("GB11111", List("A HOUSE 27-45", "A STREET"), "LONDON", "FX9 9PY", Some("GB-ENG"), Some("GB"),
      Some(5840), Some("EN"), None, Some(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB33333", List("A HOUSE 5-7", "A BOULEVARD"), "NEWCASTLE UPON TYNE", "FX1 6JN", Some("GB-ENG"),
      Some("GB"), Some(4510), Some("EN"), None, Some(Location("12.345678", "-12.345678")
          .toString)),
    DbAddress("GB44444", List("AN ADDRESS WITH A VERY LONG FIRST LINE", "SECOND LINE OF ADDRESS IS JUST AS LONG MAYBE" +
        " LONGER", "THIRD LINE IS NOT THE LONGEST BUT IS STILL VERY LONG"), "LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH", "FX2 2TB", Some("GB-WLS"), Some("GB"), Some(915),
      Some("EN"), None, Some(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB55555", List("AN ADDRESS WITH A PO BOX"), "SOME-TOWN", "FX17 1TB", Some("GB-WLS"), Some("GB"), Some(666),
      Some("EN"), None, Some(Location("12.345678", "-12.345678").toString), Some("PO BOX " +
          "1234")),
    DbAddress("GB22222", List("11 A BOULEVARD"), "NEWCASTLE UPON TYNE", "FX1 6JN", Some("GB-ENG"), Some("GB"), Some(4510), Some("EN"), None, Some(Location("12.345678", "-12.345678").toString))
  )

  val apartmentAddresses: Seq[DbAddress] = (for (i <- 1 to 2517) yield {
    DbAddress(s"GB100$i", List(s"FLAT $i", "A APARTMENTS", "AROAD"), "ATOWN", "FX4 7AL", Some("GB-ENG"), Some("GB"), Some(3725), Some("EN"), None, None)
  })

  val boulevardAddresses: Seq[DbAddress] = (for (i <- 1 to 3000) yield {
    DbAddress(s"GB200$i", List(s"$i BANKSIDE"), "ATOWN", "FX4 7AJ",
      Some("GB-ENG"), Some("GB"), Some(3725), Some("EN"), None, None)
  })

  val extraAddresses: Seq[DbAddress] = singleAddresses ++ apartmentAddresses ++ boulevardAddresses

  val cannedData = "/data/testaddresses.csv"

  lazy val dbAddresses: Seq[DbAddress] = {
    val cannedDataFile = Environment.simple().resource(cannedData).get
    val splitter = new CsvLineSplitter(Source.fromURL(cannedDataFile).bufferedReader()).asScala.toSeq
    splitter.map(CSV.convertCsvLine)
  } ++ extraAddresses

  def dbsToFilterText(dbAddress: DbAddress): Set[String] =
    (dbAddress.lines.mkString(" ") + " " + dbAddress.town + " " + dbAddress.administrativeArea.getOrElse("") + " " + dbAddress.poBox.getOrElse("")).replaceAll("[\\p{Space},]+", " ").split(" ").map(_.toLowerCase).toSet

  def doFilter(filteredDbAddresses: Seq[DbAddress], filter: Option[String]): Seq[DbAddress] = {
    val filterTokens =
      filter.map(_.toLowerCase.split("[ ]+")).map(_.toSet.filterNot(_.isEmpty)).getOrElse(Set())
    filteredDbAddresses.filter { dba =>
      val dbAddString = dbsToFilterText(dba)
      filterTokens.subsetOf(dbAddString)
    }
  }
}
