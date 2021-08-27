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
import doobie._
import doobie.implicits._
import doobie.util.fragment.Fragment.{const => csql}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source


class PgSpec extends DbBaseSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  override val schemaName = "abp20210827T101010"
  override val sqlFiles: List[String] = List(
    "create_db_status_table.sql",
    "create_db_schema.sql",
    "create_db_schema_indexes.sql",
    "create_db_lookup_view_and_indexes.sql",
    "create_db_invoke_create_view_function.sql"
  )

  override def createTransactor(): IO[Transactor[IO]] = {
    val ptx = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = container.jdbcUrl,
      user = container.username,
      pass = container.password
    )

    IO(ptx)
  }

  override def createTestData(t: Transactor[IO]): IO[Transactor[IO]] = {
    val sqlMap = loadSqlFiles

    (for {
      _ <- csql(sqlMap("create_db_schema.sql")).update.run
      _ <- csql(sqlMap("create_db_status_table.sql")).update.run
      _ <- csql(sqlMap("create_db_schema_indexes.sql")).update.run
      _ <- csql(sqlMap("create_db_lookup_view_and_indexes.sql")).update.run
      x <- csql(sqlMap("create_db_invoke_create_view_function.sql")).update.run
    } yield (x)).transact(t).unsafeRunSync()

    IO(t)
  }

  override def loadTestData(t: Transactor[IO]): IO[Transactor[IO]] = {
    // Load some test data here
    IO(t)
  }

  "PG Test" when {
    "connect" should {
      "be able to select current date time" in {
        val now = sql"select now()".query[String].unique.transact(tx).unsafeRunSync()
        now should fullyMatch regex """\p{Digit}{4}-\p{Digit}{2}-\p{Digit}{2} \p{Digit}{2}:\p{Digit}{2}:\p{Digit}{2}\..*"""
      }

      "select from lookup view" in {
        val rows = csql(s"select record_identifier from ${schemaName}.abp_classification").query[Int].to[List].transact(tx).unsafeRunSync()
        rows shouldBe empty
      }
    }
  }
}