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

package it.suites

import cats.effect._
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie._
import doobie.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global


class PgSpec extends AnyWordSpec with Matchers with ForAllTestContainer {
  implicit val cs: ContextShift[IO] = IO.contextShift(implicitly[ExecutionContext])

  override val container: PostgreSQLContainer = PostgreSQLContainer(
    dockerImageNameOverride = DockerImageName.parse("postgres:10-alpine"),
    databaseName = "addressbasepremium",
    username = "root",
    password = "Passw0rd123"
  )

  "PG Test" when {
    "connect" should {
      "get conection" in {
        val t = Transactor.fromDriverManager[IO](
          "org.postgresql.Driver",
          container.jdbcUrl,
          container.username,
          container.password
        )

        val now = sql"select now()".query[String].unique.transact(t).unsafeRunSync()
        now should not be empty
      }
    }
  }
}