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

import cats.effect.unsafe.implicits.global

import model.address.{Location, Outcode, Postcode}
import model.internal.DbAddress
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import services.AddressLookupService

class AddressLookupRepositoryTest extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  "AddressLookupService" when {

    val lookupService = app.injector.instanceOf[AddressLookupService]

    "findID is called with an id but no filter" should {
      "return an address when a matching one is found" in {
        val expectedId = "GB11111"
        val expected = DbAddress(expectedId, List("A House 27-45", "A Street"), "London", "FX9 9PY", Option("GB-ENG"), Option("GB"), Option(5840), Option("en"), None, Option(Location("12.345678", "-12.345678").toString))
        val addressOption = await(lookupService.findID(expectedId).unsafeToFuture())
        addressOption match {
          case List(address) =>
            address shouldBe expected
        }
      }

      "returns no address when no matching one is found" in {
        val expectedId = "invalid-id"
        val addressOption = await(lookupService.findID(expectedId).unsafeToFuture())
        addressOption shouldBe List()
      }
    }

    "findTown is called with a town and no filter" should {
      "return addresses when matches are found" in {
        val town = "ATown"
        val addresses = await(lookupService.findTown(town).unsafeToFuture())
        addresses should not be empty
        addresses should have length 3000
      }

      "return no addresses when no matches are found" in {
        val town = "no-matching-town"
        val addresses = await(lookupService.findTown(town).unsafeToFuture())
        addresses shouldBe empty
      }
    }

    "findTown is called with an all lowercase town and no filter" should {
      "return addresses when matches are found" in {
        val town = "atown"
        val addresses = await(lookupService.findTown(town).unsafeToFuture())
        addresses should not be empty
        addresses should have length 3000
      }
    }

    "findTown is called with a town and a filter" should {
      "return addresses when matches are found" in {
        val town = "Newcastle upon Tyne"
        val filter = "Boulevard"
        val addresses = await(lookupService.findTown(town, Option(filter)).unsafeToFuture())
        addresses should not be empty
        addresses should have length 2
      }

      "return no addresses when no matches are found" in {
        val town = "Newcastle upon Tyne"
        val filter = "ANoRoad"
        val addresses = await(lookupService.findTown(town, Option(filter)).unsafeToFuture())
        addresses shouldBe empty
      }
    }

    "findUprn is called with a uprn" should {
      "return address when match is found" in {
        val uprn = "44444"
        val expected = List(DbAddress("GB44444", List("An address with a very long first line", "Second line of address is just as long maybe longer", "Third line is not the longest but is still very long"), "Llanfairpwllgwyngyllgogerychwyrndrobwllllantysiliogogogoch", "FX2 2TB", Option("GB-WLS"), Option("GB"), Option(915), Option("en"), None, Option(Location("12.345678", "-12.345678").toString)))
        val address = await(lookupService.findUprn(uprn).unsafeToFuture())
        address shouldBe expected
      }

      "return no address when no match is found" in {
        val uprn = "0000000000"
        val addresses = await(lookupService.findUprn(uprn).unsafeToFuture())
        addresses shouldBe empty
      }
    }

    "findPostcode is called with a postcode" should {
      "return matching addresses" in {
        val postcode = "FX4 7AJ"
        val addresses = await(lookupService.findPostcode(Postcode(postcode)).unsafeToFuture())
        addresses should not be empty
        addresses should have length 3000
      }

      "return no address when no match is found" in {
        val postcode = "AA1 1AA"
        val addresses = await(lookupService.findPostcode(Postcode(postcode)).unsafeToFuture())
        addresses shouldBe empty
      }
    }

    "findPostcode is called with an all lowercase postcode" should {
      "return matching addresses" in {
        val postcode = "fx4 7aj"
        val addresses = await(lookupService.findPostcode(Postcode(postcode)).unsafeToFuture())
        addresses should not be empty
        addresses should have length 3000
      }
    }

      "findPostcode is called with a postcode and a filter" should {
      "return matching addresses" in {
        val postcode = "FX4 7AJ"
        val filter = "Bankside"
        val addresses = await(lookupService.findPostcode(Postcode(postcode), Option(filter)).unsafeToFuture())
        addresses should not be empty
        addresses should have length 3000
      }

      "return no address when no match is found" in {
        val postcode = "FX4 7AJ"
        val filter = "NonBankside"
        val addresses = await(lookupService.findPostcode(Postcode(postcode), Option(filter)).unsafeToFuture())
        addresses shouldBe empty
      }
    }

    "findPostcode is called with all lowercase postcode and a filter" should {
      "return matching addresses" in {
        val postcode = "fx4 7aj"
        val filter = "bankside"
        val addresses = await(lookupService.findPostcode(Postcode(postcode), Option(filter)).unsafeToFuture())
        addresses should not be empty
        addresses should have length 3000
      }
    }

      "findOutcode is called with an outcode" should {
      "return matching addresses" in {
        val outcode = "FX4"
        val addresses = await(lookupService.findOutcode(Outcode(outcode), "").unsafeToFuture())
        addresses should not be empty
        addresses should have length 5517
      }

      "return no addresses" in {
        val outcode = "AA1"
        val addresses = await(lookupService.findOutcode(Outcode(outcode), "").unsafeToFuture())
        addresses shouldBe empty
      }
    }

    "findOutcode is called with an all lowercase outcode" should {
      "return matching addresses" in {
        val outcode = "fx4"
        val addresses = await(lookupService.findOutcode(Outcode(outcode), "").unsafeToFuture())
        addresses should not be empty
        addresses should have length 5517
      }
    }

      "findOutcode is called with an outcode and a filter" should {
      "return matching addresses" in {
        val outcode = "FX4"
        val filter = "Apartments"
        val addresses = await(lookupService.findOutcode(Outcode(outcode), filter).unsafeToFuture())
        addresses should not be empty
        addresses should have length 2517
      }

      "return no addresses" in {
        val outcode = "AA1"
        val filter = "NonApartments"
        val addresses = await(lookupService.findOutcode(Outcode(outcode), filter).unsafeToFuture())
        addresses shouldBe empty
      }
    }
   }
}
