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

// Please keep FixturesV1 and FixturesV2 as similar as possible.

object FixturesV1 {

  val db_fx9_9py = FixturesV2.db_fx9_9py
  val fx9_9py = FixturesV2.fx9_9py_terse.asV1

  // This sample is a length-2 postcode
  val db_fx1_6jn_a = FixturesV2.db_fx1_6jn_a
  val db_fx1_6jn_b = FixturesV2.db_fx1_6jn_b

  val fx1_6jn_a = FixturesV2.fx1_6jn_a_terse.asV1
  val fx1_6jn_b = FixturesV2.fx1_6jn_b_terse.asV1

  // address with very long lines
  val db_fx1_2tb = FixturesV2.db_fx2_2tb

  val fx1_2tb = FixturesV2.fx2_2tb.asV1

  val fx11pgText: String = FixturesV2.fx11pgText
}
