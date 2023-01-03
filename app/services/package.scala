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

import scala.collection.mutable

package object services {
  implicit class Divider(c: Char) {
    def qsplit(s: String): List[String] = {
      val buf = new mutable.ListBuffer[String]
      val chars = s.toCharArray
      // n.b. hand-optimised while loop
      var i = 0
      var j = 0
      while (i < chars.length) {
        if (chars(i) == c) {
          buf += s.substring(j, i)
          i += 1
          j = i
        } else {
          i += 1
        }
      }
      buf += s.substring(j)
      buf.toList
    }
  }
}
