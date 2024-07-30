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

package model

import model.address.Address
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AddressTest extends AnyFunSuite with Matchers {
  import model.address.Country._

  test("""Given an address with only one line and a town
       then 'printable' with newline should generate the correct string""") {
    val a = Address(List("ATown"), "some-town", "FX1 1XX", Some(England), GB)
    a.printable("\n") shouldBe "ATown\nsome-town\nFX1 1XX"
  }

  test("""An address with only one line and a town is valid""") {
    val a = Address(List("ATown"), "some-town", "FX1 1XX", Some(England), GB)
    a.isValid shouldBe true
  }

  test("""Given an address with three lines, and a town,
       then 'printable' should generate the correct string""") {
    val a = Address(
      List("Line1", "Line2", "Line3"),
      "ATown",
      "FX1 1XX",
      Some(Wales),
      GB
    )
    a.printable shouldBe "Line1, Line2, Line3, ATown, FX1 1XX"
  }

  test("""Given an address with three lines and a town,
       when a truncated address is generated,
       then all of the lines should be no more than 35 characters,
       and the town should be no more than 35 characters
       and trailing whitespace should be removed""") {

    val a = Address(
      List(
        "This is Line1 and is very long so long that it is more than 35 chars",
        "This is Line2 and is very long so long that it is more than 35 chars",
        "This is Line3 and is very long so long that it is more than 35 chars"
      ),
      "Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch",
      "FX1 1XX",
      Some(England),
      GB
    ).truncatedAddress()
    val expected = List(
      // 23456789-123456789-123456789-12345
      "This is Line1 and is very long so l",
      "This is Line2 and is very long so l",
      "This is Line3 and is very long so l"
    )

    for (i <- 0 until 3) {
      a.lines(i).length <= 35 shouldBe true
      a.lines(i) shouldBe expected(i)
    }
    a.town.length shouldBe 35
  }

  test("""An address with three lines and a town is valid""") {
    val a = Address(
      List("Line1", "Line2", "Line3"),
      "ATown",
      "FX1 1XX",
      Some(Wales),
      GB
    )
    a.isValid shouldBe true
  }

  test("""An address with no lines and a town is not valid""") {
    val a = Address(Nil, "ATown", "FX1 1XX", Some(Wales), GB)
    !a.isValid shouldBe true
  }

  test("""An address with four lines and a town is not valid""") {
    val a =
      Address(List("a", "b", "c", "d"), "ATown", "FX1 1XX", Some(Wales), GB)
    !a.isValid shouldBe true
  }

  test("""An address with five lines is not valid""") {
    val a = Address(
      List("a", "b", "c", "d", "e"),
      "some-town",
      "FX1 1XX",
      Some(Wales),
      GB
    )
    !a.isValid shouldBe true
  }

  test("""Given a valid address in a record with a two-letter language,
       then the record should be valid""") {
    val a = Address(
      List("Line1", "Line2", "Line3"),
      "ATown",
      "FX1 1XX",
      Some(Wales),
      GB
    )
    val ar = address.AddressRecord(
      "abc123",
      None,
      None,
      None,
      None,
      a,
      "en",
      None,
      None
    )
    ar.isValid shouldBe true
  }

  test(
    """Given a valid address in a record that does not have a two-letter language,
       then the record should be invalid"""
  ) {
    val a = Address(
      List("Line1", "Line2", "Line3"),
      "ATown",
      "FX1 1XX",
      Some(Wales),
      GB
    )
    val ar =
      address.AddressRecord("abc123", None, None, None, None, a, "", None, None)
    !ar.isValid shouldBe true
  }
}
