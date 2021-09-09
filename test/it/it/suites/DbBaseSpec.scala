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


import cats.effect.{ContextShift, IO, Resource, Timer}
import com.codahale.metrics.SharedMetricRegistries
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie._
import doobie.implicits._
import doobie.postgres.{PFCM, PHC}
import doobie.util.fragment.Fragment.{const => csql}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.testcontainers.utility.DockerImageName
import play.api
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import java.io.File
import java.nio.file.{Files, StandardOpenOption}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait DbBaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ForAllTestContainer with GuiceOneAppPerSuite {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  SharedMetricRegistries.clear()

  protected val schemaName = "test_schema"

  override lazy val container: PostgreSQLContainer = PostgreSQLContainer(
    dockerImageNameOverride = DockerImageName.parse("postgres:10-alpine"),
    databaseName = "addressbasepremium",
    username = "root",
    password = "Passw0rd123"
  )

  private def createTransactor(): IO[Transactor[IO]] = {
    val ptx = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = container.jdbcUrl,
      user = container.username,
      pass = container.password
    )

//    val ptx = Transactor.fromDriverManager[IO](
//      driver = "org.postgresql.Driver",
//      url = "jdbc:postgresql://localhost:5433/addressbasepremium",
//      user = "root",
//      pass = "password"
//    )

    IO(ptx)
  }

  private def createTestData(t: Transactor[IO]): IO[Transactor[IO]] = {
    val sqlMap = loadSqlFiles

    val createDbObjects: IO[Int] = (for {
      _ <- csql(s"""CREATE SCHEMA ${schemaName}""").update.run
      _ =  println(s""">>> Created schemaName: ${schemaName}""")
      _ <- csql(sqlMap("createDbSchemaUrl")).update.run
      _ =  println(s""">>> Created schema objects""")
      _ <- csql("""CREATE TABLE IF NOT EXISTS public.address_lookup_status (
                   | schema_name VARCHAR(64) NOT NULL PRIMARY KEY,
                   | status      VARCHAR(32) NOT NULL,
                   | error_message VARCHAR NULL,
                   | timestamp   TIMESTAMP NOT NULL);""".stripMargin).update.run
      _ =  println(s""">>> Created public.address_lookup_status table""")
      _ <- csql(sqlMap("createDbSchemaIndexesUrl")).update.run
      _ =  println(s""">>> Created schema indexes""")
      x <- csql(sqlMap("createDbLookupViewAndIndexesUrl")).update.run
      _ =  println(s""">>> Created lookup view and indexes function""")
    } yield x).transact(t)

    val ingestFiles: IO[Long] = loadTestData(t)

    (for {
      _ <- createDbObjects
      _ <- ingestFiles
      _ =  println(s""">>> Ingested files""")
      _ <- csql(s"""BEGIN TRANSACTION;
                   |SELECT create_address_lookup_view('${schemaName}')
                   | INTO ${schemaName}.tmptbl;
                   |COMMIT;""".stripMargin).update.run.transact(t)
      _ =  println(s""">>> Created lookup view and indexes - function called""")
      _ <- csql(s"""BEGIN TRANSACTION;
                   |DROP VIEW IF EXISTS public.address_lookup;
                   |CREATE VIEW public.address_lookup AS SELECT * FROM ${schemaName}.address_lookup;
                   |COMMIT;""".stripMargin).update.run.transact(t)
      _ =  println(s""">>> Switched public.address_lookup view""")
    } yield ()).unsafeRunSync()

    IO(t)
  }

  private val fileDir: String = new File(classOf[DatabaseSchemaSpec].getResource("/data").toURI).getAbsolutePath

  private def loadTestData(t: Transactor[IO]): IO[Long] = for {
    u <- ingestFiles(schemaName, fileDir, t)
  } yield u

  protected lazy val tx: Transactor[IO] = {
    for {
      ptx <- createTransactor()
      _ <- createTestData(ptx)
      _ <- loadTestData(ptx)
    } yield ptx
  }.unsafeRunSync()

  private val sqlFiles: List[String] = List(
    "create_db_schema.sql",
    "create_db_status_table.sql",
    "create_db_schema_indexes.sql",
    "create_db_lookup_view_and_indexes.sql",
    "create_db_invoke_create_view_function.sql"
  )
  private val scriptFileUrls = {
    val scriptRootUrl = "https://raw.githubusercontent.com/hmrc/address-lookup-ingest-lambda-function/master/src/main/resources/"
    Map(
      "createDbSchemaUrl" -> s"${scriptRootUrl}create_db_schema.sql",
      "createDbSchemaIndexesUrl" -> s"${scriptRootUrl}create_db_schema_indexes.sql",
      "createDbLookupViewAndIndexesUrl" -> s"${scriptRootUrl}create_db_lookup_view_and_indexes.sql"
    )
  }

  private val loadSqlFiles: Map[String, String] = {
    for {
      config <- IO.pure(scriptFileUrls)
      createsOnly <- IO.pure(config.filter(_._1.startsWith("create")))
      creates <- IO.pure(createsOnly.map { case (k, v) => k -> Resource.make(IO(Source.fromURL(v)))(s => IO(s.close())) })
    } yield creates.map {
      case (k, v) =>
        k -> v.use(s => {
          val sqlStr = s.mkString.replaceAll("__schema__", schemaName)
//          println(s""">>> ${k} -> ${sqlStr}""")
          IO.pure(sqlStr)
        }).unsafeRunSync()
    }
  }.unsafeRunSync()

  private def _loadSqlFiles: Map[String, String] = {
    sqlFiles.map(f => f -> (if (!f.startsWith("/")) s"/$f" else f))
            .map { case (f, fcp) => f -> Resource.make(IO(Source.fromURL(classOf[DbBaseSpec].getResource(fcp))))(s => IO(s.close())) }
            .map { case (f, fr) => f -> fr.use(fs => IO(fs.mkString.replaceAll("__schema__", schemaName))) }
            .map { case (f, fr) => f -> fr.unsafeRunSync() }
            .toMap
  }

  private val recordToFileNames = Map(
    "abp_blpu" -> "ID21_BLPU_Records.csv",
    "abp_delivery_point" -> "ID28_DPA_Records.csv",
    "abp_lpi" -> "ID24_LPI_Records.csv",
    "abp_crossref" -> "ID23_XREF_Records.csv",
    "abp_classification" -> "ID32_Class_Records.csv",
    "abp_street" -> "ID11_Street_Records.csv",
    "abp_street_descriptor" -> "ID15_StreetDesc_Records.csv",
    "abp_organisation" -> "ID31_Org_Records.csv",
    "abp_successor" -> "ID30_Successor_Records.csv"
  )

  private def ingestFiles(schemaName: String, processDir: String, transactor: Transactor[IO]): IO[Long] = {
    recordToFileNames.map {
      case (t, f) => ingestFile(s"$schemaName.$t", s"$processDir/$f", transactor)
    }
  }.reduce((a, b) => for {
    av <- a
    bv <- b
  } yield av + bv)

  private def ingestFile(table: String, filePath: String, transactor: Transactor[IO]): IO[Long] = {
    val in = Files.newInputStream(new File(filePath).toPath, StandardOpenOption.READ)
    PHC.pgGetCopyAPI(
      PFCM.copyIn(s"""COPY $table FROM STDIN WITH (FORMAT CSV, HEADER, DELIMITER ',');""", in)
    ).transact(transactor)
  }
}