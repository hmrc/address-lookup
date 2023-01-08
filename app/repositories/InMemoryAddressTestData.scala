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

import model.address.Location
import model.internal.{DbAddress, NonUKAddress}
import play.api.Environment

import scala.io.Source
import scala.collection.JavaConverters.asScalaIteratorConverter

object InMemoryAddressTestData {
  val singleAddresses: Seq[DbAddress] = Seq(
    DbAddress("GB11111", 11111L, Some(111110L), Some(111100L), Some("some-organisation"), List("A HOUSE 27-45", "A STREET"), "LONDON", "FX9 9PY", Some("GB-ENG"), Some("GB"), Some(5840), Some("en"), None, Some(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB33333", 33333L, None, None, None, List("A HOUSE 5-7", "A BOULEVARD"), "NEWCASTLE UPON TYNE", "FX1 6JN", Some("GB-ENG"),
      Some("GB"), Some(4510), Some("en"), None, Some(Location("12.345678", "-12.345678")
        .toString)),
    DbAddress("GB44444", 44444L, None, None, None, List("AN ADDRESS WITH A VERY LONG FIRST LINE", "SECOND LINE OF ADDRESS IS JUST AS LONG MAYBE" +
      " LONGER", "THIRD LINE IS NOT THE LONGEST BUT IS STILL VERY LONG"), "LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH", "FX2 2TB", Some("GB-WLS"), Some("GB"), Some(915), Some("en"), None, Some(Location("12.345678", "-12.345678").toString)),
    DbAddress("GB55555", 55555L, None, None, None, List("AN ADDRESS WITH A PO BOX"), "SOME-TOWN", "FX17 1TB", Some("GB-WLS"), Some("GB"), Some(666), Some("en"), None, Some(Location("12.345678", "-12.345678").toString), Some("PO BOX " +
      "1234")),
    DbAddress("GB22222", 22222L, None, None, None, List("11 A BOULEVARD"), "NEWCASTLE UPON TYNE", "FX1 6JN", Some("GB-ENG"), Some("GB"), Some(4510), Some("en"), None, Some(Location("12.345678", "-12.345678").toString))
  )

  val apartmentAddresses: Seq[DbAddress] = (for (i <- 1 to 2517) yield {
    DbAddress(s"GB100$i", s"100$i".toLong, None, None, None, List(s"FLAT $i", "A APARTMENTS", "AROAD"), "ATOWN", "FX4 7AL", Some("GB-ENG"), Some("GB"), Some(3725), Some("en"), None, None)
  })

  val boulevardAddresses: Seq[DbAddress] = (for (i <- 1 to 3000) yield {
    DbAddress(s"GB200$i", s"200$i".toLong, None, None, None, List(s"$i BANKSIDE"), "ATOWN", "FX4 7AJ", Some("GB-ENG"), Some("GB"), Some(3725), Some("en"), None, None)
  })

  val extraAddresses: Seq[DbAddress] = singleAddresses ++ apartmentAddresses ++ boulevardAddresses

  val cannedData = "/data/testaddresses.csv"
  val cannedNonUKData = "/data/nonuk-testaddresses.csv"

  lazy val dbAddresses: Seq[DbAddress] = {
    val cannedDataFile = Environment.simple().resource(cannedData).get
    val splitter = new CsvLineSplitter(Source.fromURL(cannedDataFile).bufferedReader()).asScala.toSeq
    splitter.map(CSV.convertCsvLine)
  } ++ extraAddresses

  lazy val nonUKAddress: Map[String, List[NonUKAddress]] = {
    val cannedNonUKDataFile = Environment.simple().resource(cannedNonUKData).get
    val splitter = new CsvLineSplitter(Source.fromURL(cannedNonUKDataFile).bufferedReader()).asScala.toSeq
    splitter.map(CSV.convertNonUKCsvLine)
  }.groupBy(_._1).view.mapValues(_.map(_._2).toList).toMap

  def dbsToFilterText(dbAddress: DbAddress): Set[String] =
    (dbAddress.lines.mkString(" ") + " " + dbAddress.town + " " + dbAddress.administrativeArea.getOrElse("") + " " + dbAddress.poBox.getOrElse("") + " " + dbAddress.postcode).replaceAll("[\\p{Space},]+", " ").split(" ").map(_.toLowerCase).toSet

  def nonUkDbsToFilterText(nonUkAddress: NonUKAddress): Set[String] =
    (nonUkAddress.postcode.getOrElse("") + " " + nonUkAddress.city.getOrElse("") + " " + nonUkAddress.region.getOrElse("") + " " + nonUkAddress.unit.getOrElse("") + " " + nonUkAddress.district.getOrElse("") + " " + nonUkAddress.street.getOrElse("") + " " + nonUkAddress.number.getOrElse(""))
      .replaceAll("[\\p{Space},]+", " ").split(" ").map(_.toLowerCase).toSet

  def doFilter(filteredDbAddresses: Seq[DbAddress], filter: Option[String]): Seq[DbAddress] = {
    val filterTokens =
      filter.map(_.toLowerCase.split("[ ]+")).map(_.toSet.filterNot(_.isEmpty)).getOrElse(Set())
    filteredDbAddresses.filter { dba =>
      val dbAddString = dbsToFilterText(dba)
      filterTokens.subsetOf(dbAddString)
    }
  }

  def doNonUkFilter(filteredNonUkAddresses: Seq[NonUKAddress], filter: String): Seq[NonUKAddress] = {
    val filterTokens =
      filter.toLowerCase.split("[ ]+").toSet.filterNot(_.isEmpty)
    filteredNonUkAddresses.filter { dba =>
      val dbAddString = nonUkDbsToFilterText(dba)
      filterTokens.subsetOf(dbAddString)
    }
  }
}
