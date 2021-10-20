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

package controllers

import model.address.Postcode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SearchParametersTest extends AnyWordSpec with Matchers {

  "SearchParameters" when {
    "built from a request with valid parameters" should {
      "succeed" in {
        val sp = SearchParameters.fromQueryParameters(Map(
          "uprn" -> Seq("12345"),
          "postcode" -> Seq("FX1 1ZZ"),
          "filter" -> Seq("Quayside"),
          "fuzzy" -> Seq("Platform"),
          "town" -> Seq("Someton"),
          "line1" -> Seq("A House"),
          "line2" -> Seq("A Street"),
          "line3" -> Seq("A Town"),
          "line4" -> Seq("A City")))

        sp shouldBe SearchParameters(
          uprn = Some("12345"),
          postcode = Postcode.cleanupPostcode("FX1 1ZZ"),
          fuzzy = Some("Platform"),
          filter = Some("Quayside"),
          town = Some("Someton"),
          lines = List("A House", "A Street", "A Town", "A City")
        )
      }
    }

    "built with a valid postcode" should {
      "succeed" in {
        val sp1 = SearchParameters(postcode = Postcode.cleanupPostcode("FX11 2EF"), uprn = None, fuzzy = None, filter = None, town = None, lines = Nil)
        sp1.postcode shouldBe Some(Postcode("FX11 2EF"))

        val sp2 = SearchParameters(postcode = None, uprn = None, fuzzy = None, filter = None, town = None, lines = Nil)
        sp2.postcode should not be defined
      }
    }

    "cleaned" should {
      "produce the same as the input when a valid SearchParameter instance is used" in {
        val sp1 = SearchParameters(
          uprn = Some("12345"),
          postcode = Postcode.cleanupPostcode("FX1 1ZZ"),
          fuzzy = Some("Platform"),
          filter = Some("Quayside"),
          town = Some("Someton"),
          lines = List("A House", "A Street", "A Town", "A City")
        )
        sp1.clean shouldBe sp1
      }

      "produce a SearchParameter instance that is empty when an instance with all empty fields is used" in {
        val sp2 = SearchParameters(
          uprn = Some(""),
          postcode = Postcode.cleanupPostcode(""),
          fuzzy = Some(""),
          filter = Some(""),
          town = Some(""),
          lines = List("", "", "", "")
        )
        sp2.clean shouldBe SearchParameters()

        val sp3 = SearchParameters()
        sp3.clean shouldBe sp3
      }
    }

    "tupled" should {
      "produce tupled values that match the field values in the SearchParameters instance provided" in {
        val sp1 = SearchParameters(
          uprn = Some("12345"),
          postcode = Postcode.cleanupPostcode("FX1 1ZZ"),
          fuzzy = Some("Platform"),
          filter = Some("Quayside"),
          town = Some("Someton"),
          lines = List("A House", "A Street", "A Town", "A City")
        )
        val t = sp1.tupled
        t shouldBe List("uprn" -> "12345", "postcode" -> "FX1+1ZZ", "town" -> "Someton", "fuzzy" -> "Platform", "filter" -> "Quayside",
          "line1" -> "A House", "line2" -> "A Street", "line3" -> "A Town", "line4" -> "A City")
      }
    }
  }
}