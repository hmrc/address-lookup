/*
 * Copyright 2024 HM Revenue & Customs
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

///*
// * Copyright 2017 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package it.suites
//
//import model.address.Country._
//import model.address.{Address, AddressRecord, LocalCustodian, Location}
//
//object Fixtures {
//  private val lc4510 = Some(LocalCustodian(4510, "NEWCASTLE UPON TYNE"))
//  private val lc5840 = Some(LocalCustodian(5840, "SOUTHWARK"))
//  private val lc915 = Some(LocalCustodian(915, "CARLISLE"))
//
//  private val en = "en"
//
//  private val TownName1 = "NEWCASTLE UPON TYNE"
//  private val Postcode1 = "FX1 6JN"
//
//  val location1: Location = Location("12.345678", "-12.345678")
//  val id1 = "GB11111"
//  val urpn1: Some[Long] = Some(11111L)
//  val parentUprn1 = Some(111110L)
//  val usrn1 = Some(111100L)
//  val organisation1 = Some("some-organisation")
//  val fx9_9py_terse: AddressRecord = AddressRecord(id1, urpn1, parentUprn1, usrn1, organisation1, Address(List("A HOUSE 27-45", "A STREET"), "LONDON", "FX9 9PY", Some(England), GB),
//    en, lc5840, Some(location1.toSeq))
//
//  // This sample is a length-2 postcode
//  val id2 = "GB22222"
//  val uprn2: Some[Long] = Some(22222L)
//
//  val id3 = "GB33333"
//  val uprn3: Some[Long] = Some(33333L)
//
//  val location2: Location = Location("12.345678", "-12.345678")
//  val aHouseLocation2: Location = Location("12.345678", "-12.345678")
//
//  val fx1_6jn_a_terse: AddressRecord = AddressRecord(id2, uprn2, None, None, None, Address(List("11 A BOULEVARD"), TownName1, Postcode1, Some(England), GB),
//    en, lc4510, Some(location2.toSeq))
//
//  val fx1_6jn_b_terse: AddressRecord = AddressRecord(id3, uprn3, None, None, None, Address(List("A HOUSE 5-7", "A BOULEVARD"), TownName1, Postcode1, Some(England), GB),
//    en, lc4510, Some(aHouseLocation2.toSeq))
//
//  val id4 = "GB44444"
//  val uprn4: Some[Long] = Some(44444L)
//
//  // address with very long lines
//  val db_fx2_2tb: DbAddress = DbAddress(id4, uprn4.get, None, None, None, List("AN ADDRESS WITH A VERY LONG FIRST LINE",
//    "SECOND LINE OF ADDRESS IS JUST AS LONG MAYBE LONGER",
//    "THIRD LINE IS NOT THE LONGEST BUT IS STILL VERY LONG"), "LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH", "FX2 2TB", Some("GB-WLS"), Some("GB"), Some(915), Some("en"), None, Some(aHouseLocation2.toString))
//
//  val fx2_2tb: AddressRecord = AddressRecord(id4, uprn4, None, None, None, Address(List("AN ADDRESS WITH A VERY LONG FIRST LINE",
//    "SECOND LINE OF ADDRESS IS JUST AS LONG MAYBE LONGER",
//    "WAVELL DRIVE, ROSEHILL INDUSTRIAL ESTATE"),
//    "LLANFAIRPWLLGWYNGYLLGOGERYCHWYRNDROBWLLLLANTYSILIOGOGOGOCH", "FX2 2TB", Some(Wales), GB),
//    en, lc915, Some(aHouseLocation2.toSeq))
//
//  val id5 = "GB55555"
//
//  // non-uk samples used in tests
//  val nukdb_fx1 = NonUKAddress(Some("BMdef91be0039f9abd"), Some("15"), Some("Allspice Gardens"), None, None, Some("Warwick"), None, Some("WK04"))
//  val nukdb_fx2 = NonUKAddress(Some("BM43eb7d6f6d8061d0"), Some("14"), Some("Bayfield Road"), None, None, Some("Warwick"), None, Some("WK04"))
//
//}
