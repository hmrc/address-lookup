import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

lazy val root = Project("address-lookup", file("."))
    .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
    .settings(majorVersion := 4)
    .settings(scalaVersion := "2.12.15")
    .settings(publishingSettings)
    .settings(
      targetJvm := "jvm-1.8",
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      Compile / doc / sources := List(),
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := true,
      Compile / packageBin / publishArtifact := true
    )
    .settings(
      Test / parallelExecution := false,
      Test / fork := false,
      Test / unmanagedSourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "unit")).value,
      addTestReportOption(Test, "test-reports"),
      Test / doc / sources := List(),
      Test / packageDoc / publishArtifact := false
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      IntegrationTest / Keys.fork := false,
      IntegrationTest / unmanagedSourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "it")).value,
      IntegrationTest / unmanagedResourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "resources")).value,
      addTestReportOption(IntegrationTest, "int-test-reports"),
      IntegrationTest / parallelExecution := false
    )
    .settings(
      update / evictionWarningOptions := EvictionWarningOptions.default
                                                               .withWarnTransitiveEvictions(false)
                                                               .withWarnDirectEvictions(false)
                                                               .withWarnScalaVersionEviction(false)
    )
    .settings(resolvers ++= Seq(Resolver.jcenterRepo))
