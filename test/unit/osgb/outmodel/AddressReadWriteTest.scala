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

package osgb.outmodel

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import osgb.outmodel.v1.AddressReadable._
import osgb.outmodel.v1.AddressWriteable._
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.address.v1.{Address, AddressRecord, Countries, LocalCustodian}

@RunWith(classOf[JUnitRunner])
class AddressReadWriteTest extends WordSpec {
  import Countries.UK

  val addrLike = AddressRecord("GB0123456789", Some(12345678L),
    Address(List("AHouse", "AStreet", "ARoad"),
      Some("ATown"), Some("ACounty"), "FX1 5XD", Some("GB-ENG"), UK),
    Some(LocalCustodian(4510, "Tyne & Wear")),
    "en")

  "Given a populated address record" when {
    "it is written to JSON then read back again" should {
      "yield the original value" in {
        val json = Json.toJson(addrLike)
        json.validate[AddressRecord] match {
          case s: JsSuccess[AddressRecord] =>
            assert(s.get === addrLike)

          case e: JsError => fail(e.toString)
        }
      }
    }
  }
}
