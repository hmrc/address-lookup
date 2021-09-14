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

import address.uk.Postcode
import doobie.implicits._
import doobie.Fragment.{const => csql}
import repositories.{AddressLookupRepository, RdsQueryConfig}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


class DatabaseSchemaSpec extends DbBaseSpec {
  "PG Test" when {
    "connect" should {
      "be able to select current date time" in {
        val now = sql"SELECT NOW()".query[String].unique.transact(tx).unsafeRunSync()
        now should fullyMatch regex """\p{Digit}{4}-\p{Digit}{2}-\p{Digit}{2} \p{Digit}{2}:\p{Digit}{2}:\p{Digit}{2}\..*"""
      }
    }
  }

  "Address Lookup Repository" when {
    "configured" should {
      // Configure guice to create use the created transactor
      lazy val lookup = new AddressLookupRepository(tx, RdsQueryConfig(10000, 100))

      "retrieve the correct number of non-pobox addresses" in {
        val poBoxAddressCount = csql(s"""select count(*) from  ${schemaName}.address_lookup where poboxnumber is null""").query[Int].unique.transact(tx).unsafeRunSync()
        poBoxAddressCount shouldBe 11
      }

      "retrieve the correct number of pobox addresses" in {
        val poBoxAddressCount = csql(s"""select count(*) from  ${schemaName}.address_lookup where poboxnumber is not null""").query[Int].unique.transact(tx).unsafeRunSync()
        poBoxAddressCount shouldBe 1
      }

      "retrieve records correctly from database" in {
        val result = Await.result(lookup.findPostcode(Postcode("JE2 6PZ")), 10.seconds)
        result should have length (1)
        val address = result.head
        address.poBox shouldBe None
        address.administrativeArea shouldBe Some("JERSEY")
        address.country shouldBe Some("JE")
        address.language shouldBe Some("en")
        address.lines shouldBe List("9 Princess Elizabeth Court Princess Place", "St. Clement")
        address.postcode shouldBe "JE2 6PZ"
        address.localCustodianCode shouldBe Some(8200)
        address.location shouldBe Some("49.1725976000000031,-2.08627170000000017")
        address.id shouldBe "GB10095037446"
        address.subdivision shouldBe None
        address.blpuClass shouldBe None
      }

      "retrieve pobox record correctly" in {
        val result = Await.result(lookup.findPostcode(Postcode("JE2 6QB")), 10.seconds)
        result should have length (1)
        val address = result.head
        address.poBox shouldBe Some("1234")
        address.administrativeArea shouldBe Some("JERSEY")
        address.country shouldBe Some("JE")
        address.language shouldBe Some("en")
        address.lines shouldBe List("Po Box 1234", "St. Clement")
        address.postcode shouldBe "JE2 6QB"
        address.localCustodianCode shouldBe Some(8200)
        address.location shouldBe Some("49.176721299999997,-2.09082789999999985")
        address.id shouldBe "GB10095038062"
        address.subdivision shouldBe None
        address.blpuClass shouldBe None
      }
    }
  }
}