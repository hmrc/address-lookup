
import sbt.{ModuleID, _}

object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val pegdownVersion = "1.6.0"
  private val doobieVersion = "0.13.4"
  private val bootstrapPlayVersion = "8.1.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "com.univocity" % "univocity-parsers" % "2.9.1",
    "com.github.tototoshi" %% "scala-csv" % "1.3.10",
    "org.tpolecat" %% "doobie-core" % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion,
    "org.tpolecat" %% "doobie-hikari" % doobieVersion,
    jdbc
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % Test,
    "org.pegdown" % "pegdown" % pegdownVersion % Test,
    "org.jsoup" % "jsoup" % "1.14.3" % Test,
  )
}
