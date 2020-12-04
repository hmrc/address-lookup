
import sbt.{ModuleID, _}

object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val hmrcTestVersion = "3.5.0-play-26"
  private val scalaTestVersion = "3.0.8"
  private val pegdownVersion = "1.6.0"
  private val jacksonVersion = "2.8.9"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-26" % "3.0.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "logging" % "0.7.0" withSources(),
    "uk.gov.hmrc" %% "address-reputation-store" % "2.40.0" withSources(),
    "com.univocity" % "univocity-parsers" % "1.5.6",
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.4.0",
    "com.github.tototoshi" %% "scala-csv" % "1.3.6",

    "org.tpolecat" %% "doobie-core"       % "0.7.1",
    "org.tpolecat" %% "doobie-hikari"     % "0.7.1",
    "org.tpolecat" %% "doobie-postgres"   % "0.7.1",
    "org.tpolecat" %% "doobie-hikari"     % "0.7.1",
    jdbc
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "org.elasticsearch" % "elasticsearch" % "2.4.1" % scope,

        "org.tpolecat"           %% "doobie-scalatest"            % "0.7.1"   % "test, it"
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

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

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}

