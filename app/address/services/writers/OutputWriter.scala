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

import java.util.Date

import address.osgb.DbAddress

trait OutputWriter {
  def existingTargetThatIsNewerThan(date: Date): Option[String]

  def begin()

  def output(a: DbAddress)

  // result is 'hasFailed' flag
  def end(completed: Boolean): Boolean
}


case class WriterSettings(bulkSize: Int, loopDelay: Int, algorithm: Algorithm)

object WriterSettings {
  val default = WriterSettings(1, 0, Algorithm.default)
}
