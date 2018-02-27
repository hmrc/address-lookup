import sbt.Keys._
import sbt._

object MicroServiceBuild extends Build with MicroService {
  import scala.util.Properties.envOrElse

  val appName = "address-lookup"
  val appVersionKey: String = appName.toUpperCase.replace('-', '_') + "_VERSION"
  val appVersion: String = envOrElse(version.toString, "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val hmrcTestVersion = "3.0.0"
  private val scalaTestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"
  private val jacksonVersion = "2.7.4"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "6.9.0",
    "uk.gov.hmrc" %% "play-ui" % "7.13.0",
    "uk.gov.hmrc" %% "domain" % "5.0.0",
    "uk.gov.hmrc" %% "logging" % "0.4.0" withSources(),
    "uk.gov.hmrc" %% "address-reputation-store" % "2.28.0" withSources(),
    "uk.gov.hmrc" %% "play-random-json-filter" % "0.3.0",
    "com.univocity" % "univocity-parsers" % "1.5.6",
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.4.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.elasticsearch" % "elasticsearch" % "2.4.1" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.elasticsearch" % "elasticsearch" % "2.4.1" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}

