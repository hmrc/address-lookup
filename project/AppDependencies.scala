
import sbt.{ModuleID, _}

object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val doobieVersion = "0.13.4"
  private val bootstrapPlayVersion = "9.19.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion,
    "com.univocity" % "univocity-parsers" % "2.9.1",
    "com.github.tototoshi" %% "scala-csv" % "2.0.0",
    "org.tpolecat" %% "doobie-core" % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion,
    "org.tpolecat" %% "doobie-hikari" % doobieVersion,
    jdbc
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion,
    "org.jsoup" % "jsoup" % "1.21.1"
  ).map(_ % Test)
}
