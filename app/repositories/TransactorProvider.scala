/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO}
import doobie.Transactor
import doobie.hikari.HikariTransactor
import javax.inject.{Inject, Provider, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext

@Singleton
class TransactorProvider @Inject()(configuration: Configuration, ec: ExecutionContext, applicationLifecycle: ApplicationLifecycle)
    extends Provider[Transactor[IO]] {

  override lazy val get: Transactor[IO] = {
    implicit val cs: ContextShift[IO] = IO.contextShift(ec)

    val dbConfig = configuration.get[Configuration]("db.address-lookup")

    val hikariTransactorResource = HikariTransactor.newHikariTransactor[IO](
      dbConfig.get[String]("driver"),
      dbConfig.get[String]("url"),
      dbConfig.get[String]("username"),
      dbConfig.get[String]("password"),
      ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10)), // waiting for connections
      ExecutionContext.fromExecutor(Executors.newCachedThreadPool()) // executing db calls, bounded by hikari connection pool size
    )

    val (transactor, releaseResource) = hikariTransactorResource.allocated.unsafeRunSync()

    applicationLifecycle.addStopHook(() => releaseResource.unsafeToFuture())

    transactor
  }
}