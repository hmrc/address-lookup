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

package osgb

package object outmodel {

  def blankToOption(s: String): Option[String] = if (s == null || s.isEmpty) None else Some(s)

  def trim(s: String): String = if (s == null) null else s.trim

  def removeTrailingCommaAndTrim(s: String): String = {
    val t = trim(s)
    if (t == null) null
    else if (t.endsWith(",")) t.init.trim
    else t
  }

//  def stripQuotes(s: String): String = {
//    if (s.startsWith("\"") && s.endsWith("\""))
//      s.substring(1, s.length - 1).trim
//    else
//      s.trim
//  }
}
