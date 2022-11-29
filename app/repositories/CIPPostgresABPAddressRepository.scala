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

package repositories

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CIPPostgresABPAddressRepository @Inject()(transactor: Transactor[IO]) {

  private val logger = Logger(this.getClass.getSimpleName)

  def testConnection(implicit ec: ExecutionContext): Future[Int] = {
    val queryFragment =
      sql"""SELECT 1"""

    val f = queryFragment.query[Int].unique.transact(transactor).unsafeToFuture()
    f.onComplete {
      case Success(i) => logger.info("Connection to new CIP Paas DB succeeded!")
      case Failure(exception) => logger.error("Connection to new CIP Paas DB failed")
    }

    f
  }
}
