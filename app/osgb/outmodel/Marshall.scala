/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.address.v2.AddressRecord

object Marshall {

  def marshallV1Address(address: AddressRecord): JsValue = {
    import osgb.outmodel.v1.AddressWriteable._
    Json.toJson(address.asV1)
  }

  def marshallV2Address(address: AddressRecord): JsValue = {
    import osgb.outmodel.v2.AddressWriteable._
    Json.toJson(address)
  }

  def marshallV1List(addresses: List[AddressRecord]): JsValue = {
    import osgb.outmodel.v1.AddressWriteable._
    Json.toJson(addresses.map(_.asV1))
  }

  def marshallV2List(addresses: List[AddressRecord]): JsValue = {
    import osgb.outmodel.v2.AddressWriteable._
    Json.toJson(addresses)
  }
}
