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

import cats.effect.IO
import model.address.{Outcode, Postcode}
import model.internal.DbAddress


trait AddressSearcher {

  def findID(id: String): IO[List[DbAddress]]

  def findUprn(uprn: String): IO[List[DbAddress]]

  def findPostcode(postcode: Postcode, filter: Option[String] = None): IO[List[DbAddress]]

  def findTown(town: String, filter: Option[String] = None): IO[List[DbAddress]]

  def findOutcode(outcode: Outcode, filter: String): IO[List[DbAddress]]
}
