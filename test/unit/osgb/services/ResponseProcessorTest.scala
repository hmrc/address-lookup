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

package osgb.services

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import address.osgb.DbAddress
import address.v2._

@RunWith(classOf[JUnitRunner])
class ResponseProcessorTest extends FunSuite {

  import Countries._

  private val en = "en"
  private val TyneAndWear = Some("Tyne & Wear")
  private val InUse = Some("In_Use")
  private val Approved = Some("Approved")
  private val AllVehicles = Some("All_Vehicles")

  private val lc4510 = Some(LocalCustodian(4510, "Newcastle upon Tyne"))
  private val lc8132 = Some(LocalCustodian(8132, "Belfast"))

  val loc1: Location = Location("12.345678", "-12.345678")
  val loc2: Location = Location("12.345678", "-12.345678")
  val loc3: Location = Location("12.345678", "-12.345678")
  val loc10: Location = Location("12.345678", "-12.345678")
  val loc11: Location = Location("12.345678", "-12.345678")
  val locZ: Location = Location("12.345678", "-12.345678")

  val dbGB1: DbAddress = DbAddress("GB10001", List("1 Line", "Line2", "Line3"), Some("ATown"), "FX11 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), Some(2), Some(1), Some(8), None, Some(loc1.toString))
  val dbGB2: DbAddress = DbAddress("GB10002", List("2 Line", "Line2", "Line3"), Some("ATown"), "FX11 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), Some(2), Some(1), Some(8), None, Some(loc2.toString))
  val dbGB3: DbAddress = DbAddress("GB10003", List("3 Line", "Line2", "Line3"), Some("ATown"), "FX11 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), Some(2), Some(1), Some(8), None, Some(loc3.toString))
  val dbGB10: DbAddress = DbAddress("GB10004", List("10 Line", "Line2", "Line3"), Some("ATown"), "FX11 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), Some(2), Some(1), Some(8), None, Some(loc10.toString))
  val dbGB11: DbAddress = DbAddress("GB10005", List("11 Line", "Line2", "Line3"), Some("ATown"), "FX11 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), Some(2), Some(1), Some(8), None, Some(loc11.toString))
  val dbGBZ: DbAddress = DbAddress("GB10006", List("wxyz1", "wxyz2", "wxyz3"), Some("ATown"), "FX11 4HG",
    Some("GB-ENG"), Some("UK"), Some(4510), Some("en"), Some(2), Some(1), Some(8), None, Some(locZ.toString))

  val expGB1M: AddressRecord = AddressRecord("GB10001", Some(10001L), Address(List("1 Line", "Line2", "Line3"), Some("ATown"), TyneAndWear, "FX11 4HG", Some(England), UK), en, lc4510, Some(loc1.toSeq), InUse, Approved, AllVehicles)
  val expGB1X: AddressRecord = expGB1M.withoutMetadata

  val expGB2M: AddressRecord = AddressRecord("GB10002", Some(10002L), Address(List("2 Line", "Line2", "Line3"), Some("ATown"), TyneAndWear, "FX11 4HG", Some(England), UK), en, lc4510, Some(loc2.toSeq), InUse, Approved, AllVehicles)
  val expGB2X: AddressRecord = expGB2M.withoutMetadata

  val expGB3M: AddressRecord = AddressRecord("GB10003", Some(10003L), Address(List("3 Line", "Line2", "Line3"), Some("ATown"), TyneAndWear, "FX11 4HG", Some(England), UK), en, lc4510, Some(loc3.toSeq), InUse, Approved, AllVehicles)
  val expGB3X: AddressRecord = expGB3M.withoutMetadata

  val expGB10M: AddressRecord = AddressRecord("GB10004", Some(10004L), Address(List("10 Line", "Line2", "Line3"), Some("ATown"), TyneAndWear, "FX11 4HG", Some(England), UK), en, lc4510, Some(loc10.toSeq), InUse, Approved, AllVehicles)
  val expGB10X: AddressRecord = expGB10M.withoutMetadata

  val expGB11M: AddressRecord = AddressRecord("GB10005", Some(10005L), Address(List("11 Line", "Line2", "Line3"), Some("ATown"), TyneAndWear, "FX11 4HG", Some(England), UK), en, lc4510, Some(loc11.toSeq), InUse, Approved, AllVehicles)
  val expGB11X: AddressRecord = expGB11M.withoutMetadata

  val expGBZM: AddressRecord = AddressRecord("GB10006", Some(10006L), Address(List("wxyz1", "wxyz2", "wxyz3"), Some("ATown"), TyneAndWear, "FX11 4HG", Some(England), UK), en, lc4510, Some(loc11.toSeq), InUse, Approved, AllVehicles)
  val expGBZX: AddressRecord = expGBZM.withoutMetadata

  val locNI1: Location = Location("12.345678", "-12.345678")
  val locNI2: Location = Location("12.345678", "-12.345678")

  val dbNI1: DbAddress = DbAddress("GB10007", List("Line1", "Line2", "Line3"), Some("ATown"), "FX11 4HG",
    Some("GB-NIR"), Some("UK"), Some(8132), Some("en"), Some(2), Some(1), Some(8), None, Some(locNI1.toString))
  val dbNI2: DbAddress = DbAddress("GB10008", List("wxyz1", "wxyz2", "wxyz3"), Some("ATown"), "FX11 4HG",
    Some("GB-NIR"), Some("UK"), Some(8132), Some("en"), Some(2), Some(1), Some(8), None, Some(locNI2.toString))

  val expNI1M: AddressRecord = AddressRecord("GB10007", Some(10007L), Address(List("Line1", "Line2", "Line3"), Some("ATown"), Some("County Antrim"), "FX11 4HG", Some(NorthernIreland), UK), en, lc8132, Some(locNI1.toSeq), InUse, Approved, AllVehicles)
  val expNI1X: AddressRecord = expNI1M.withoutMetadata

  val expNI2M: AddressRecord = AddressRecord("GB10008", Some(10008L), Address(List("wxyz1", "wxyz2", "wxyz3"), Some("ATown"), Some("County Antrim"), "FX11 4HG", Some(NorthernIreland), UK), en, lc8132, Some(locNI2.toSeq), InUse, Approved, AllVehicles)
  val expNI2X: AddressRecord = expNI2M.withoutMetadata

  val refData: ReferenceData = ReferenceData.load("sample_local_custodian_table.csv", "sample_local_custodian_ceremonial_counties.csv")


  test(
    """given a single DbAddress,
       convertAddressList will convert the data correctly
       and apply the local custodian from the reference data
       and include the metadata
    """) {
    val rp = new ResponseProcessor(refData)
    for (de <- List(dbGB1 -> expGB1M, dbGB2 -> expGB2M, dbGB10 -> expGB10M, dbGB11 -> expGB11M, dbGBZ -> expGBZM, dbNI1 -> expNI1M, dbNI2 -> expNI2M)) {
      val in = List(de._1)
      val exp = List(de._2)
      val adr = rp.convertAddressList(in, withMetadata = true)
      assert(adr === exp)
    }
  }

  test(
    """given a list of DbAddresses,
       convertAddressList will convert the data correctly
       and apply the local custodian from the reference data
    """) {
    val rp = new ResponseProcessor(refData)
    val adr = rp.convertAddressList(List(dbGB1, dbGB2, dbGB10, dbGB11, dbGBZ, dbNI1, dbNI2), withMetadata = false)
    assert(adr.toSet === Set(expGB1X, expGB2X, expGB10X, expGB11X, expGBZX, expNI1X, expNI2X))
  }

  test(
    """given a list of DbAddresses,
       convertAddressList will sort the data correctly - 1
    """) {
    val rp = new ResponseProcessor(refData)
    val adr1 = rp.convertAddressList(List(dbGB1, dbGB2, dbGB3, dbGBZ), withMetadata = false)
    val adr2 = rp.convertAddressList(List(dbGBZ, dbGB3, dbGB2, dbGB1), withMetadata = false)
    assert(adr1 === List(expGB1X, expGB2X, expGB3X, expGBZX))
    assert(adr2 === List(expGB1X, expGB2X, expGB3X, expGBZX))
  }

  ignore(
    """ *** micro-benchmark ***
        given a list of DbAddresses,
        convertAddressList will sort the data correctly - 2 verify correct numeric sorting
    """) {
    val rp = new ResponseProcessor(refData)

    val biggest = 9 // TODO use a number bigger than 9

    val addresses = for (i <- 1 to biggest;
                         c <- 'A' to 'Z';
                         d <- 'a' to 'z') yield {
      val l1 = if (i < 4) s"$i $c$d Street" else "Flat 1"
      val l2 = if (i < 4) "Uninteresting" else if (i < 8) s"$i $c$d Street" else "Floor 15"
      val l3 = if (i < 8) "District" else s"$i $c$d Street"
      DbAddress(s"G$c$d$i", List(l1, l2, l3), Some("Town"), "FX1 1ZZ", None, Some("UK"), None, Some("en"), None, None, None, None, None)
    }

    val expected = for (i <- 1 to biggest;
                        c <- 'A' to 'Z';
                        d <- 'a' to 'z') yield {
      val l1 = if (i < 4) s"$i $c$d Street" else "Flat 1"
      val l2 = if (i < 4) "Uninteresting" else if (i < 8) s"$i $c$d Street" else "Floor 15"
      val l3 = if (i < 8) "District" else s"$i $c$d Street"
      AddressRecord(s"G$c$d$i", Some(i.toLong), Address(List(l1, l2, l3), Some("Town"), None, "FX1 1ZZ", None, UK), en, None, None, None, None, None)
    }

    val shuffled = addresses.toSet.toList
    val reversed = addresses.toList.reverse

    val actual1 = rp.convertAddressList(shuffled, withMetadata = false)
    val actual2 = rp.convertAddressList(reversed, withMetadata = false)

    assert(actual1 === expected.toList)
    assert(actual2 === expected.toList)

    val start = System.currentTimeMillis
    for (_ <- 1 to 200) {
      rp.convertAddressList(shuffled, withMetadata = false)
      rp.convertAddressList(reversed, withMetadata = false)
    }
    val took = System.currentTimeMillis - start
    println(s"took ${took}ms")
  }
}
