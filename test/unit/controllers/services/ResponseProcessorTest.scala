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

package controllers.services

import model.address.{Address, AddressRecord, LocalCustodian, Location}
import model.internal.DbAddress
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import util.Utils.tags.MicroBenchmark

class ResponseProcessorTest extends AnyWordSpec with Matchers {

  import model.address.Country._

  private val en = "en"

  private val lc4510 = Option(LocalCustodian(4510, "Newcastle upon Tyne"))
  private val lc8132 = Option(LocalCustodian(8132, "Belfast"))

  val loc1: Location = Location("12.345678", "-12.345678")
  val loc2: Location = Location("12.345678", "-12.345678")
  val loc3: Location = Location("12.345678", "-12.345678")
  val loc10: Location = Location("12.345678", "-12.345678")
  val loc11: Location = Location("12.345678", "-12.345678")
  val locZ: Location = Location("12.345678", "-12.345678")

  val dbGB1: DbAddress = DbAddress("GB10001", List("1 Line", "Line2", "Line3"), "ATown", "FX11 4HG",
    Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(loc1.toString))
  val dbGB2: DbAddress = DbAddress("GB10002", List("2 Line", "Line2", "Line3"), "ATown", "FX11 4HG",
    Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(loc2.toString))
  val dbGB3: DbAddress = DbAddress("GB10003", List("3 Line", "Line2", "Line3"), "ATown", "FX11 4HG",
    Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(loc3.toString))
  val dbGB10: DbAddress = DbAddress("GB10004", List("10 Line", "Line2", "Line3"), "ATown", "FX11 4HG",
    Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(loc10.toString))
  val dbGB11: DbAddress = DbAddress("GB10005", List("11 Line", "Line2", "Line3"), "ATown", "FX11 4HG",
    Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(loc11.toString))
  val dbGBZ: DbAddress = DbAddress("GB10006", List("wxyz1", "wxyz2", "wxyz3"), "ATown", "FX11 4HG",
    Option("GB-ENG"), Option("GB"), Option(4510), Option("en"), None, Option(locZ.toString))

  val expGB1M: AddressRecord = AddressRecord("GB10001", Option(10001L), Address(List("1 Line", "Line2", "Line3"), "ATown", "FX11 4HG", Option(England), GB), en, lc4510, Option(loc1.toSeq))

  val expGB2M: AddressRecord = AddressRecord("GB10002", Option(10002L), Address(List("2 Line", "Line2", "Line3"), "ATown", "FX11 4HG", Option(England), GB), en, lc4510, Option(loc2.toSeq))

  val expGB3M: AddressRecord = AddressRecord("GB10003", Option(10003L), Address(List("3 Line", "Line2", "Line3"), "ATown", "FX11 4HG", Option(England), GB), en, lc4510, Option(loc3.toSeq))

  val expGB10M: AddressRecord = AddressRecord("GB10004", Option(10004L), Address(List("10 Line", "Line2", "Line3"), "ATown", "FX11 4HG", Option(England), GB), en, lc4510, Option(loc10.toSeq))

  val expGB11M: AddressRecord = AddressRecord("GB10005", Option(10005L), Address(List("11 Line", "Line2", "Line3"), "ATown", "FX11 4HG", Option(England), GB), en, lc4510, Option(loc11.toSeq))

  val expGBZM: AddressRecord = AddressRecord("GB10006", Option(10006L), Address(List("wxyz1", "wxyz2", "wxyz3"), "ATown", "FX11 4HG", Option(England), GB), en, lc4510, Option(loc11.toSeq))

  val locNI1: Location = Location("12.345678", "-12.345678")
  val locNI2: Location = Location("12.345678", "-12.345678")

  val dbNI1: DbAddress = DbAddress("GB10007", List("Line1", "Line2", "Line3"), "ATown", "FX11 4HG",
    Option("GB-NIR"), Option("GB"), Option(8132), Option("en"), None, Option(locNI1.toString))
  val dbNI2: DbAddress = DbAddress("GB10008", List("wxyz1", "wxyz2", "wxyz3"), "ATown", "FX11 4HG",
    Option("GB-NIR"), Option("GB"), Option(8132), Option("en"), None, Option(locNI2.toString))

  val expNI1M: AddressRecord = AddressRecord("GB10007", Option(10007L), Address(List("Line1", "Line2", "Line3"),
    "ATown", "FX11 4HG", Option(NorthernIreland), GB), en, lc8132, Option(locNI1.toSeq))

  val expNI2M: AddressRecord = AddressRecord("GB10008", Option(10008L), Address(List("wxyz1", "wxyz2", "wxyz3"),
    "ATown", "FX11 4HG", Option(NorthernIreland), GB), en, lc8132, Option(locNI2.toSeq))

  val dbPoBox: DbAddress = DbAddress("GB10008", List("PO BOX 1234", "", ""), "", "PO1 1PO",
    Option("GB-NIR"), Option("GB"), Option(8132), Option("en"), None, Option(locNI2.toString), Option("1234"))
  val expPoBox: AddressRecord = AddressRecord("GB10008", Option(10008L), Address(List("PO BOX 1234", "", ""),
    "", "PO1 1PO", Option(NorthernIreland), GB), en, lc8132, Option(locNI2.toSeq), None, Option("1234"))

  val refData: ReferenceData = ReferenceData.load("sample_local_custodian_table.csv")

  "ResponseProcessor" when {

    """given a single DbAddress""" should {
      """convert the data correctlyand apply the local custodian from the reference data and include the metadata""" in {

        val rp = new ResponseProcessor(refData)
        for (de <- List(dbGB1 -> expGB1M, dbGB2 -> expGB2M, dbGB10 -> expGB10M, dbGB11 -> expGB11M, dbGBZ -> expGBZM,
          dbNI1 -> expNI1M, dbNI2 -> expNI2M, dbPoBox -> expPoBox)) {
          val in = List(de._1)
          val exp = List(de._2)
          val adr = rp.convertAddressList(in)
          adr shouldBe exp
        }
      }
    }

    """given a list of DbAddresses""" should {
      """convertAddressList will sort the data correctly - 2 verify correct numeric sorting""" taggedAs MicroBenchmark in {
        val rp = new ResponseProcessor(refData)

        val biggest = 9

        val addresses = for (i <- 1 to biggest;
                             c <- 'A' to 'Z';
                             d <- 'a' to 'z') yield {
          val l1 = if (i < 4) s"$i $c$d Street" else "Flat 1"
          val l2 = if (i < 4) "Uninteresting" else if (i < 8) s"$i $c$d Street" else "Floor 15"
          val l3 = if (i < 8) "District" else s"$i $c$d Street"
          DbAddress(s"G$c$d$i", List(l1, l2, l3), "Town", "FX1 1ZZ", None, Option("GB"), None, Option("en"), None, None)
        }

        val expected = for (i <- 1 to biggest;
                            c <- 'A' to 'Z';
                            d <- 'a' to 'z') yield {
          val l1 = if (i < 4) s"$i $c$d Street" else "Flat 1"
          val l2 = if (i < 4) "Uninteresting" else if (i < 8) s"$i $c$d Street" else "Floor 15"
          val l3 = if (i < 8) "District" else s"$i $c$d Street"
          AddressRecord(s"G$c$d$i", Option(i.toLong), Address(List(l1, l2, l3), "Town", "FX1 1ZZ", None, GB), en, None, None)
        }

        val shuffled = addresses.toSet.toList
        val reversed = addresses.toList.reverse

        val actual1 = rp.convertAddressList(shuffled)
        val actual2 = rp.convertAddressList(reversed)

        actual1 shouldBe expected.toList
        actual2 shouldBe expected.toList

        val start = System.currentTimeMillis
        for (_ <- 1 to 200) {
          rp.convertAddressList(shuffled)
          rp.convertAddressList(reversed)
        }
        val took = System.currentTimeMillis - start
        println(s"took ${took}ms")
      }
    }
  }
}
