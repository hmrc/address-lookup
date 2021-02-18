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

/*
 * Copyright 2016 HM Revenue & Customs
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

package address.services

import java.io.PrintWriter

// This exists for the sake of simplicity. Although the Univocity tools include a CSV writer,
// the logic is simpler to be re-implemented, rather than dealing with all the options available
// in Univocity which don't concern us.

class CsvWriter(w: PrintWriter) {

  def println(row: Seq[String]): CsvWriter = {
    if (row.isEmpty) w.println()
    else {
      csvCell(row.head)
      // using 'while' to avoid 'for' overheads here because this will be used in nested loops
      val it = row.tail.iterator
      while (it.hasNext) {
        w.print(',')
        csvCell(it.next)
      }
      w.println()
    }
    this
  }

  private def csvCell(value: String) {
    val quote = value.indexOf('"')
    val v = if (quote < 0) value
    else value.replace("\"", "\"\"")

    val comma = v.indexOf(',')
    if (comma >= 0 || v.startsWith(" ") || v.endsWith(" ")) {
      w.print('"')
      w.print(v)
      w.print('"')
    } else {
      w.print(v)
    }
  }

}
