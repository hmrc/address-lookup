credentials += Credentials(Path.userHome / ".sbt" / ".credentials")


resolvers ++= Seq(
  Resolver.url("hmrc-sbt-plugin-releases",
    url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns))

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.8")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "3.2.0")

addSbtPlugin("uk.gov.hmrc" % "hmrc-resolvers" % "0.4.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.0.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "0.10.0")
