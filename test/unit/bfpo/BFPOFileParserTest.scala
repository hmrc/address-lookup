/*
 * Copyright 2020 HM Revenue & Customs
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

package bfpo

import bfpo.outmodel.BFPO
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BFPOFileParserTest extends FunSuite {

  val exp = List(
    BFPO(None, List("Washington", "USA"), "BF1 3AA", "2"),
    BFPO(None, List("Kathmandu", "Nepal"), "BF1 3AD", "4"),
    BFPO(None, List("Box 589", "British Alpine Centre (Bavaria)"), "BF1 0AX", "105"),
    BFPO(None, List("Box 2003", "NATO School Oberammergau"), "BF1 0AX", "105"),
    BFPO(None, List("HMS Albion"), "BF1 4AF", "204"),
    BFPO(None, List("HMS Ambush"), "BF1 4AG", "205"),
    BFPO(None, List("HMS Test"), "BF1 9ZZ", "999"),
    BFPO(None, List("HMS Test2"), "BF1 9ZZ", "999"),
    BFPO(None, List("NP 1002", "Diego Garcia (BIOT)"), "BF1 4TG", "485"),
    BFPO(None, List("NP 1011 - Mine Warfare Centre (MWC)/Autec", "Portsmouth", "UK / Miami"), "BF1 4TL", "488"),
    BFPO(None, List("NP 1005", "Den Helder", "Holland"), "BF1 4TU", "495"),
    BFPO(Some("Op ATALANTA"), List("Djibouti"), "BF1 4TW", "496"),
    BFPO(Some("Op ATALANTA"), List("Attached to Foreign Ships"), "BF1 4TX", "497"),
    BFPO(None, List("HMS Saker", "Washington", "USA"), "BF1 3AA", "c/o 2"),
    BFPO(Some("Op ELGAN"), Nil, "BF1 5AB", "501"),
    BFPO(Some("Op SHADER"), Nil, "BF1 5DP", "550"),
    BFPO(None, List("Ex CLOCKWORK", "05 Nov – 31 Mar"), "BF1 5AP", "510"),
    BFPO(None, List("Ex CETUS", "02 Jan – 13 Mar"), "BF1 5BB", "521")
  )

  test("parse sample") {
    val list = BFPOFileParser.loadResource("bfpo-test-sample.txt")
    assert(list.size === exp.size)
    for (i <- list.indices) {
      assert(list(i) === exp(i))
    }
  }
}
