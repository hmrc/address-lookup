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

package address.services.writers

case class Algorithm(
                      includeDPA: Boolean = true,
                      includeLPI: Boolean = true,
                      preferDPA: Boolean = true,
                      streetFilter: Int = 0,
                      containedPhrases: List[String] = Nil,
                      startingPhrases: List[String] = Nil
                    ) {

  def preferLPI: Boolean = !preferDPA

  def prefer: String = if (preferDPA) Algorithm.DPA else Algorithm.LPI
}


object Algorithm {
  val default = new Algorithm()
  val DPA = "DPA"
  val LPI = "LPI"
}
