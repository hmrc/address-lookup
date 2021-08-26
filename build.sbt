import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

lazy val root = Project("address-lookup", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .enablePlugins(Seq(PlayScala): _*)
  .settings(majorVersion := 4)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.12.12")
  .settings(scalacOptions ++= Seq("-Xlint:-missing-interpolator"))
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= AppDependencies(),
    parallelExecution in Test := false,
    fork in Test := true,
    retrieveManaged := true
  )
  .configs(Test)
  .settings(
    unmanagedSourceDirectories in Test := (baseDirectory in Test) (base => Seq(base / "test" / "unit")).value,
    addTestReportOption(Test, "test-reports"))
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in Test)(base => Seq(base / "test" / "it")).value,
    unmanagedResourceDirectories in IntegrationTest :=  (baseDirectory in Test) (base => Seq(base / "test" / "resources")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    parallelExecution in IntegrationTest := false)
  .settings(
    sources in doc in Compile := List(),
    sources in doc in Test := List(),
    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := true,
    publishArtifact in(Test, packageDoc) := false,
    publishArtifact in(Test, packageSrc) := true,
    publishArtifact in(Compile, packageBin) := true
  )
  .settings(resolvers ++= Seq(Resolver.jcenterRepo))
  .settings(evictionWarningOptions in update :=
    EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false).withWarnScalaVersionEviction(false))
  .enablePlugins(SbtDistributablesPlugin, SbtAutoBuildPlugin, SbtGitVersioning)