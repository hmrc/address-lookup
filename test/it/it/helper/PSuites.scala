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

package it.helper

import org.scalatest.Suites

class PSuites(suitesToNest: org.scalatest.Suite*) extends Suites(suitesToNest: _*) {
  // expose this publicly
  override def runNestedSuites(args: org.scalatest.Args): org.scalatest.Status = super.runNestedSuites(args)
}
