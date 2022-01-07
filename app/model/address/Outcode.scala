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

package model.address

case class Outcode(area: String, district: String) {

  override lazy val toString = area + district
}


object Outcode {
  /**
    * Performs normalisation and then checks the syntax, returning None if the string
    * cannot represent a well-formed outcode.
    */
  def cleanupOutcode(p: String): Option[Outcode] = {
    if (p == null) None
    else doCleanupOutcode(p)
  }

  private def doCleanupOutcode(p: String): Option[Outcode] = {
    val norm = p.trim.toUpperCase
    checkSyntax(norm)
  }

  private def checkSyntax(out: String): Option[Outcode] = {
    if (Postcode.oPattern.matcher(out).matches())
      Some(Outcode(out))
    else
      None
  }

  // outcode must be already cleaned up and normalised
  def apply(outcode: String): Outcode = {
    if (Character.isDigit(outcode(1)))
      Outcode(outcode.substring(0, 1), outcode.substring(1))
    else
      Outcode(outcode.substring(0, 2), outcode.substring(2))
  }
}