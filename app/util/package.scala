/*
 * Copyright 2022 HM Revenue & Customs
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

package object util {
  implicit class Divider(s: String) {
    def divide(c: Char): List[String] = {
      val i = s.indexOf(c)
      if (i < 0) List(s)
      else {
        val w1 = s.substring(0, i)
        val rest = s.substring(i + 1)
        List(w1, rest)
      }
    }
  }
}