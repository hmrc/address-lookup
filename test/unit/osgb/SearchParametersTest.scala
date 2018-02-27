/*
 * Copyright 2018 HM Revenue & Customs
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

package osgb

import org.scalatest.FunSuite
import uk.gov.hmrc.address.uk.Postcode

class SearchParametersTest extends FunSuite {

  test("fromRequest") {
    val sp = SearchParameters.fromRequest(Map(
      "uprn" -> Seq("12345"),
      "postcode" -> Seq("FX1 1ZZ"),
      "filter" -> Seq("Quayside"),
      "fuzzy" -> Seq("Platform"),
      "town" -> Seq("Someton"),
      "line1" -> Seq("A House"),
      "line2" -> Seq("A Street"),
      "line3" -> Seq("A Town"),
      "line4" -> Seq("A City")))

    assert(sp === SearchParameters(
      uprn = Some("12345"),
      postcode = Postcode.cleanupPostcode("FX1 1ZZ"),
      fuzzy = Some("Platform"),
      filter = Some("Quayside"),
      town = Some("Someton"),
      lines = List("A House", "A Street", "A Town", "A City")
    ))
  }

  test("asPostcode") {
    val sp1 = SearchParameters(postcode = Postcode.cleanupPostcode("FX11 2EF"), uprn = None, fuzzy = None, filter = None, town = None, lines = Nil)
    assert(sp1.postcode === Some(Postcode("FX11 2EF")))

    val sp2 = SearchParameters(postcode = None, uprn = None, fuzzy = None, filter = None, town = None, lines = Nil)
    assert(sp2.postcode === None)
  }

  test("clean") {
    val sp1 = SearchParameters(
      uprn = Some("12345"),
      postcode = Postcode.cleanupPostcode("FX1 1ZZ"),
      fuzzy = Some("Platform"),
      filter = Some("Quayside"),
      town = Some("Someton"),
      lines = List("A House", "A Street", "A Town", "A City")
    )
    assert(sp1.clean === sp1)

    val sp2 = SearchParameters(
      uprn = Some(""),
      postcode = Postcode.cleanupPostcode(""),
      fuzzy = Some(""),
      filter = Some(""),
      town = Some(""),
      lines = List("", "", "", "")
    )
    assert(sp2.clean === SearchParameters())

    val sp3 = SearchParameters()
    assert(sp3.clean === sp3)
  }

  test("tupled") {
    val sp1 = SearchParameters(
      uprn = Some("12345"),
      postcode = Postcode.cleanupPostcode("FX1 1ZZ"),
      fuzzy = Some("Platform"),
      filter = Some("Quayside"),
      town = Some("Someton"),
      lines = List("A House", "A Street", "A Town", "A City")
    )
    val t = sp1.tupled
    assert(t === List("uprn" -> "12345", "postcode" -> "FX1+1ZZ", "town" -> "Someton", "fuzzy" -> "Platform", "filter" -> "Quayside",
      "line1" -> "A House", "line2" -> "A Street", "line3" -> "A Town", "line4" -> "A City"))
  }
}
