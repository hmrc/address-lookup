
import sbt.{ModuleID, _}

object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val hmrcTestVersion   = "3.5.0-play-26"
  private val scalaTestVersion  = "3.0.8"
  private val pegdownVersion    = "1.6.0"
  private val jacksonVersion    = "2.8.9"
  private val doobieVersion     = "0.7.1"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-26"  % "3.0.0",
    "uk.gov.hmrc"                   %% "domain"                     % "5.10.0-play-26",
    "uk.gov.hmrc"                   %% "logging"                    % "0.7.0" withSources(),
    "uk.gov.hmrc"                   %% "address-reputation-store"   % "2.41.0" withSources(),
    "com.univocity"                 %  "univocity-parsers"          % "1.5.6",
    "com.fasterxml.jackson.core"    %  "jackson-core"               % jacksonVersion,
    "com.fasterxml.jackson.core"    %  "jackson-databind"           % jacksonVersion,
    "com.fasterxml.jackson.core"    %  "jackson-annotations"        % jacksonVersion,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % jacksonVersion,
    "com.github.tototoshi"          %% "scala-csv"                  % "1.3.6",
    "org.tpolecat"                  %% "doobie-core"                % doobieVersion,
    "org.tpolecat"                  %% "doobie-hikari"              % doobieVersion,
    "org.tpolecat"                  %% "doobie-postgres"            % doobieVersion,
    "org.tpolecat"                  %% "doobie-hikari"              % doobieVersion,
    jdbc
  )

  object Test {
    def apply(): Seq[ModuleID] = Seq(
      "uk.gov.hmrc"             %% "hmrctest"                     % hmrcTestVersion     % "test, it",
      "org.scalatest"           %% "scalatest"                    % scalaTestVersion    % "test, it",
      "org.pegdown"             %  "pegdown"                      % pegdownVersion      % "test, it",
      "com.typesafe.play"       %% "play-test"                    % PlayVersion.current % "test, it",
      "org.scalatestplus.play"  %% "scalatestplus-play"           % "3.1.3"             % "test, it",
      "org.jsoup"               %  "jsoup"                        % "1.7.3"             % "test, it",
      "org.mockito"             %  "mockito-all"                  % "1.10.19"           % "test, it",
      "org.tpolecat"            %% "doobie-scalatest"             % doobieVersion       % "test, it"
    )
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}

