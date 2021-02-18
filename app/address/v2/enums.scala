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

package address.v2

object BLPUStateHelper {

  def valueToEnum(value: String) = Option(BLPUState.valueOf(value))

  def codeToEnum(code: Int) = Option(BLPUState.lookup(code))

  def codeToString(code: Int): Option[String] = codeToEnum(code).map(_.name)
}


object LogicalStateHelper {

  def valueToEnum(value: String) = Option(LogicalState.valueOf(value))

  def codeToEnum(code: Int) = Option(LogicalState.lookup(code))

  def codeToString(code: Int): Option[String] = codeToEnum(code).map(_.name)
}


object StreetClassificationHelper {

  def valueToEnum(value: String) = Option(StreetClassification.valueOf(value))

  def codeToEnum(code: Int) = Option(StreetClassification.lookup(code))

  def codeToString(code: Int): Option[String] = codeToEnum(code).map(_.name)
}


object enums {}
