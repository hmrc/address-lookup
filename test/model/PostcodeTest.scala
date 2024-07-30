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

import model.address.Postcode
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PostcodeTest extends AnyFunSuite with Matchers {
  test("""Given valid postcode string should cleanup successfully""") {
    val pc = "FX11XX"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "FX1 1XX"
  }

  test("""Given valid short postcode string should cleanup successfully""") {
    val pc = "W12DN"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "W1 2DN"
  }

  test("""Given valid long postcode string should cleanup successfully""") {
    val pc = "DE128HJ"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "DE12 8HJ"
  }

  test(
    """Given valid edge case postcode string should cleanup successfully"""
  ) {
    val pc = "GIR0AA"
    val maybePostcode = Postcode.cleanupPostcode(pc)
    maybePostcode shouldBe defined
    maybePostcode.get.toString shouldBe "GIR 0AA"
  }
}
