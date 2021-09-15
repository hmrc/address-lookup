/*
 * Copyright 2017 HM Revenue & Customs
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

package it.suites

import address.osgb.DbAddress
import address.model._

// Please keep FixturesV1 and FixturesV2 as similar as possible.

object FixturesV2 {

  import Countries.UK
  import Countries.England
  import Countries.Wales

  private val lc4510 = Some(LocalCustodian(4510, "Newcastle upon Tyne"))
  private val lc5840 = Some(LocalCustodian(5840, "Southwark"))
  private val lc915 = Some(LocalCustodian(915, "Carlisle"))

  private val en = "en"


  private val TownName1 = "Newcastle upon Tyne"
  private val Postcode1 = "FX1 6JN"

  val location1: Location = Location("12.345678", "-12.345678")
  val id1 = "GB11111"
  val urpn1: Some[Long] = Some(11111L)
  val db_fx9_9py: DbAddress = DbAddress(id1, List("A House 27-45", "A Street"), "London", "FX9 9PY",
    Some("GB-ENG"), Some("UK"), Some(5840), Some("en"), None, Some(location1.toString))
  val fx9_9py_terse: AddressRecord = AddressRecord(id1, urpn1, Address(List("A House 27-45", "A Street"), "London", "FX9 9PY", Some(England), UK),
    en, lc5840, Some(location1.toSeq))
  val fx9_9py_augmented: AddressRecord = AddressRecord(id1, urpn1, Address(List("A House 27-45", "A Street"), "London", "FX9 9PY", Some(England), UK),
    en, lc5840, Some(location1.toSeq))

  // This sample is a length-2 postcode
  val id2 = "GB22222"
  val uprn2: Some[Long] = Some(22222L)

  val id3 = "GB33333"
  val uprn3: Some[Long] = Some(33333L)

  val location2: Location = Location("12.345678", "-12.345678")
  val db_fx1_6jn_a: DbAddress = DbAddress(id2, List("11 A Street"), TownName1, Postcode1,
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), None, Some(location2.toString))
  val aHouseLocation2: Location = Location("12.345678", "-12.345678")
  val db_fx1_6jn_b: DbAddress = DbAddress(id3, List("A House 5-7", "A Street"), TownName1, Postcode1,
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), None, Some(aHouseLocation2.toString))

  val fx1_6jn_a_augmented: AddressRecord = AddressRecord(id2, uprn2, Address(List("11 A Street"), TownName1, Postcode1, Some(England), UK),
    en, lc4510, Some(location2.toSeq))
  val fx1_6jn_a_terse: AddressRecord = AddressRecord(id2, uprn2, Address(List("11 A Boulevard"), TownName1, Postcode1, Some(England), UK),
    en, lc4510, Some(location2.toSeq))


  val fx1_6jn_b_augmented: AddressRecord = AddressRecord(id3, uprn3, Address(List("A House 5-7", "A Street"), TownName1, Postcode1, Some(England), UK),
    en, lc4510, Some(aHouseLocation2.toSeq))
  val fx1_6jn_b_terse: AddressRecord = AddressRecord(id3, uprn3, Address(List("A House 5-7", "A Boulevard"), TownName1, Postcode1, Some(England), UK),
    en, lc4510, Some(aHouseLocation2.toSeq))


  val id4 = "GB44444"
  val uprn4: Some[Long] = Some(44444L)

  // address with very long lines
  val db_fx2_2tb: DbAddress = DbAddress(id4, List("An address with a very long first line",
    "Second line of address is just as long maybe longer",
    "Third line is not the longest but is still very long"),
    "Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch", "FX2 2TB", Some("GB-WLS"), Some("UK"),
    Some(915), Some("en"), None, Some(aHouseLocation2.toString))

  val fx2_2tb: AddressRecord = AddressRecord(id4, uprn4, Address(List("An address with a very long first line",
    "Second line of address is just as long maybe longer",
    "Wavell Drive, Rosehill Industrial Estate"),
    "Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch", "FX2 2TB", Some(Wales), UK),
    en, lc915, Some(aHouseLocation2.toSeq))

  val id5 = "GB55555"
  val db_fx17_1tb: DbAddress = DbAddress(id5, List("An address with a PO Box"),
    "some-town", "FX17 1TB", Some("GB-WLS"), Some("UK"),
    Some(666), Some("en"), None, Some(aHouseLocation2.toString), Some("PO Box 1234"))

  val fx11pgText:String =
    """
      |10091818954, "R1Test, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |10091818956, "M1Test, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |10091818959, "KEY TRAINING, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |10091818958, "B2Test, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |10091818955, "C1Test, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |10091818957, "A1Test, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |4510133795, "FX4Test FLOOR 5, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510133783, "A2Test FLOOR 1, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510015545, "", 46 AStreet, "", ACity, FX1 1PG
      |4510147310, "FLAT 1", 44 AStreet, "", ACity, FX1 1PG
      |4510731398, "H2Test", 38 AStreet, "", ACity, FX1 1PG
      |4510141279, "", 54 AStreet, "", ACity, FX1 1PG
      |4510716064, "FLOOR 1", 52 AStreet, "", ACity, FX1 1PG
      |4510133793, "FLOOR 5, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510133794, "R2Test, FLOOR 5, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510115932, "", 6 AStreet, "", ACity, FX1 1PG
      |4510147317, "T1Test", 44 AStreet, "", ACity, FX1 1PG
      |4510147311, "FLAT 2", 44 AStreet, "", ACity, FX1 1PG
      |4510147312, "FLAT 3", 44 AStreet, "", ACity, FX1 1PG
      |4510731397, "FX1Test", 38 AStreet, "", ACity, FX1 1PG
      |4510130163, "FLAT 1", 48 AStreet, "", ACity, FX1 1PG
      |4510716063, "FX2Test", 52 AStreet, "", ACity, FX1 1PG
      |4510730179, "FLOOR 1, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |4510729790, "FLOOR 3, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |4510729847, "FLOOR 2, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |4510728859, "FLOOR 4, ACOURT", 22-28 AStreet, "", ACity, FX1 1PG
      |4510147313, "FLAT 4", 44 AStreet, "", ACity, FX1 1PG
      |4510133785, "FLOOR 1, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510739358, "B1Test FLOOR 4, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510133788, "FLOOR 4, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510015558, "", 10 AStreet, "", ACity, FX1 1PG
      |4510067036, "", 42 AStreet, "", ACity, FX1 1PG
      |4510117750, "", 40 AStreet, "", ACity, FX1 1PG
      |4510133790, "FX5Test FLOOR 4, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510015564, "", 30-34 AStreet, "", ACity, FX1 1PG
      |4510733083, "FLOOR 1", 48 AStreet, "", ACity, FX1 1PG
      |4510720605, "", 12-16 AStreet, "", ACity, FX1 1PG
      |4510133792, "FX3Test FLOOR 3, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510133787, "FLOOR 3, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510133784, "H1Test FLOOR 1, ABuilding", 31 AStreet, "", ACity, FX1 1PG
      |4510115936, "", 18-20 AStreet, "", ACity, FX1 1PG
      |4510120682, "FLAT 2", 48 AStreet, "", ACity, FX1 1PG
      |4510720604, "", 12-20 AStreet, "", ACity, FX1 1PG
      |4510147314, "FLAT 5", 44 AStreet, "", ACity, FX1 1PG
      |4510732929, "GROUND FLOOR", 52 AStreet, "", ACity, FX1 1PG
      |9999999999, "GROUND FLOOR", 52 AStreet, "FLOOR 1", ACity, FX1 1PG
    """.stripMargin.trim
}