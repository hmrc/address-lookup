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

import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.Transactor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName

import scala.io.Source

trait DbBaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer {

  override lazy val container: PostgreSQLContainer = PostgreSQLContainer(
    dockerImageNameOverride = DockerImageName.parse("postgres:10-alpine"),
    databaseName = "addressbasepremium",
    username = "root",
    password = "Passw0rd123"
  )

  lazy val tx: Transactor[IO] = {
    for {
      ptx <- createTransactor()
      _   <- createTestData(ptx)
      _   <- loadTestData(ptx)
    } yield ptx
  }.unsafeRunSync()

  val schemaName: String

  def createTransactor(): IO[Transactor[IO]]
  def createTestData(t: Transactor[IO]): IO[Transactor[IO]]
  def loadTestData(t: Transactor[IO]): IO[Transactor[IO]]

  val sqlFiles: List[String]

  def loadSqlFiles: Map[String, String] = {
    sqlFiles.map(f => f -> (if (!f.startsWith("/")) s"/$f" else f))
            .map { case (f, fcp) => f -> Resource.make(IO(Source.fromURL(classOf[DbBaseSpec].getResource(fcp))))(s => IO(s.close())) }
            .map { case (f, fr) => f -> fr.use(fs => IO(fs.mkString.replaceAll("__schema__", schemaName))) }
            .map { case (f, fr) => f -> fr.unsafeRunSync() }
            .toMap
  }
}