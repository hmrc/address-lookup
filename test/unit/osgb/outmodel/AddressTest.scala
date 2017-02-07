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

package osgb.outmodel

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import uk.gov.hmrc.address.v1.{Address, AddressRecord, Countries}

@RunWith(classOf[JUnitRunner])
class AddressTest extends FunSuite {
  import Countries.UK

  test(
    """Given an address with only one line and no town
       then 'printable' with newline should generate the correct string""") {
    val a = Address(List("ATown"), None, None, "FX1 1XX", Some("GB-ENG"), UK)
    assert(a.printable("\n") === "ATown\nFX1 1XX")
  }

  test(
    """An address with only one line and no town is valid""") {
    val a = Address(List("ATown"), None, None, "FX1 1XX", Some("GB-ENG"), UK)
    assert(a.isValid)
  }

  test(
    """Given an address with three lines, a town and a county,
       then 'printable' should generate the correct string""") {
    val a = Address(List("Line1", "Line2", "Line3"), Some("ATown"), Some("ACounty"), "FX1 1XX", Some("GB-WLS"), UK)
    assert(a.printable === "Line1, Line2, Line3, ATown, ACounty, FX1 1XX")
  }

  test(
    """Given an address with three lines and a town,
       when a truncated address is generated,
       then all of the lines should be no more than 35 characters,
       and the town should be no more than 35 characters
       and trailing whitespace should be removed""") {

    val a = Address(List(
      "This is Line1 and is very long so long that it is more than 35 chars",
      "This is Line2 and is very long so long that it is more than 35 chars",
      "This is Line3 and is very long so long that it is more than 35 chars"),
      Some("Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch"), Some("ACounty"), "FX1 1XX", Some("GB-ENG"), UK).truncatedAddress()
    val expected = List(
      //23456789-123456789-123456789-12345
      "This is Line1 and is very long so l",
      "This is Line2 and is very long so l",
      "This is Line3 and is very long so l")

    for (i <- 0 until 3) {
      assert(a.lines(i).length <= 35, a.lines(i))
      assert(a.lines(i) === expected(i))
    }
    assert(a.town.get.length === 35, a.town.get)
  }

  test(
    """An address with three lines and a town is valid""") {
    val a = Address(List("Line1", "Line2", "Line3"), Some("ATown"), Some("ACounty"), "FX1 1XX", Some("GB-WLS"), UK)
    assert(a.isValid)
  }

  test(
    """An address with no lines and a town is not valid""") {
    val a = Address(Nil, Some("ATown"), Some("ACounty"), "FX1 1XX", Some("GB-WLS"), UK)
    assert(!a.isValid)
  }

  test(
    """An address with four lines and a town is not valid""") {
    val a = Address(List("a", "b", "c", "d"), Some("ATown"), Some("ACounty"), "FX1 1XX", Some("GB-WLS"), UK)
    assert(!a.isValid)
  }

  test(
    """An address with five lines and no townor county is not valid""") {
    val a = Address(List("a", "b", "c", "d", "e"), None, None, "FX1 1XX", Some("GB-WLS"), UK)
    assert(!a.isValid)
  }

  test(
    """Given a valid address in a record with a two-letter language,
       then the record should be valid""") {
    val a = Address(List("Line1", "Line2", "Line3"), Some("ATown"), Some("ACounty"), "FX1 1XX", Some("GB-WLS"), UK)
    val ar = AddressRecord("abc123", None, a, None, "en")
    assert(ar.isValid)
  }

  test(
    """Given a valid address in a record that does not have a two-letter language,
       then the record should be invalid""") {
    val a = Address(List("Line1", "Line2", "Line3"), Some("ATown"), Some("ACounty"), "FX1 1XX", Some("GB-WLS"), UK)
    val ar = AddressRecord("abc123", None, a, None, "")
    assert(!ar.isValid)
  }
}
