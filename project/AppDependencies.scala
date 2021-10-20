
import sbt.{ModuleID, _}

object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val scalaTestVersion  = "3.1.4"
  private val pegdownVersion    = "1.6.0"
  private val doobieVersion     = "0.7.1"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % "5.16.0",
    "com.univocity"                 %  "univocity-parsers"          % "2.9.1",
    "com.github.tototoshi"          %% "scala-csv"                  % "1.3.8",
    "org.tpolecat"                  %% "doobie-core"                % doobieVersion,
    "org.tpolecat"                  %% "doobie-postgres"            % doobieVersion,
    "org.tpolecat"                  %% "doobie-hikari"              % doobieVersion,
    jdbc
  )

  val test = Seq(
      "org.scalatest"           %% "scalatest"                        % scalaTestVersion    % "test, it",
      "com.vladsch.flexmark"    %  "flexmark-all"                     % "0.36.8"            % "test, it",
      "org.pegdown"             %  "pegdown"                          % pegdownVersion      % "test, it",
      "com.typesafe.play"       %% "play-test"                        % PlayVersion.current % "test, it",
      "org.scalatestplus.play"  %% "scalatestplus-play"               % "5.1.0"             % "test, it",
      "org.scalatestplus"       %% "mockito-3-4"                      % "3.1.4.0"           % "test, it",
      "org.jsoup"               %  "jsoup"                            % "1.14.3"            % "test, it",
    )

  def apply(): Seq[ModuleID] = compile ++ test
}