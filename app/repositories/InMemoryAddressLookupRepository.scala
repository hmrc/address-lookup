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
import controllers.services.AddressSearcher
import model.address.{Location, Outcode, Postcode}
import model.internal.DbAddress
import play.api.Environment

import javax.inject.Inject
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.io.Source

class InMemoryAddressLookupRepository @Inject()(env: Environment) extends AddressSearcher {
  import repositories.InMemoryAddressLookupRepository._

  override def findID(id: String): IO[List[DbAddress]] =
    IO(dbAddresses.filter(_.id == id).toList)

  override def findUprn(uprn: String): IO[List[DbAddress]] =
    IO(dbAddresses.filter(_.uprn == uprn.toLong).toList.take(3000))

  override def findPostcode(postcode: Postcode, filter: Option[String]): IO[List[DbAddress]] =
    IO(doFilter(dbAddresses.filter(_.postcode.equalsIgnoreCase(postcode.toString)), filter).toList.take(3000))

  override def findTown(town: String, filter: Option[String]): IO[List[DbAddress]] =
    IO(doFilter(dbAddresses.filter(_.town.equalsIgnoreCase(town)), filter).toList.take(3000))

  override def findOutcode(outcode: Outcode, filter: String): IO[List[DbAddress]] =
    IO(doFilter(dbAddresses.filter(_.postcode.toUpperCase.startsWith(outcode.toString.toUpperCase)), Option(filter)).toList)
}

object InMemoryAddressLookupRepository {
  val singleAddresses: Seq[DbAddress] = Seq(
    DbAddress("GB11111", List("A House 27-45", "A Street"), "London", "FX9 9PY", Option("GB-ENG"), Option("GB"),
      Option(5840), Option("en"), None, Option(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB33333", List("A House 5-7", "A Boulevard"), "Newcastle upon Tyne", "FX1 6JN", Option("GB-ENG"),
      Option("GB"), Option(4510), Option("en"), None, Option(Location("12.345678", "-12.345678")
          .toString)),
    DbAddress("GB44444", List("An address with a very long first line", "Second line of address is just as long maybe longer", "Third line is not the longest but is still very long"), "Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch", "FX2 2TB", Option("GB-WLS"), Option("GB"), Option(915),
      Option("en"), None, Option(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB55555", List("An address with a PO Box"), "some-town", "FX17 1TB", Option("GB-WLS"), Option("GB"),
      Option(666),
      Option("en"), None, Option(Location("12.345678", "-12.345678").toString), Option("PO Box 1234")),
    DbAddress("GB22222", List("11 A Boulevard"), "Newcastle upon Tyne", "FX1 6JN",
      Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(Location("12.345678", "-12.345678").toString))
  )

  val apartmentAddresses: Seq[DbAddress] = for (i <- 1 to 2517) yield {
    DbAddress(s"GB100$i", List(s"Flat $i", "A Apartments", "ARoad"), "ATown", "FX4 7AL", Option("GB-ENG"), Option("GB"), Option(3725), Option("en"), None, None)
  }

  val boulevardAddresses: Seq[DbAddress] = for (i <- 1 to 3000) yield {
    DbAddress(s"GB200$i", List(s"$i Bankside"), "ATown", "FX4 7AJ",
      Option("GB-ENG"), Option("GB"), Option(3725), Option("en"), None, None)
  }

  val extraAddresses: Seq[DbAddress] = singleAddresses ++ apartmentAddresses ++ boulevardAddresses

  val cannedData = "/data/testaddresses.csv"

  lazy val dbAddresses: Seq[DbAddress] = {
    val cannedDataFile = Environment.simple().resource(cannedData).get
    val splitter = new CsvLineSplitter(Source.fromURL(cannedDataFile).bufferedReader()).asScala.toSeq
    splitter.map(CSV.convertCsvLine)
  } ++ extraAddresses

  def dbsToFilterText(dbAddress: DbAddress): Set[String] =
    (s"${dbAddress.lines.mkString(" ")} ${dbAddress.town} ${dbAddress.administrativeArea.getOrElse("")} ${dbAddress.poBox.getOrElse("")}").replaceAll("[\\p{Space},]+", " ").split(" ").map(_.toLowerCase).toSet

  def doFilter(filteredDbAddresses: Seq[DbAddress], filter: Option[String]): Seq[DbAddress] = {
    val filterTokens =
      filter.map(_.toLowerCase.split("[ ]+")).map(_.toSet.filterNot(_.isEmpty)).getOrElse(Set())
    filteredDbAddresses.filter { dba =>
      val dbAddString = dbsToFilterText(dba)
      filterTokens.subsetOf(dbAddString)
    }
  }
}
