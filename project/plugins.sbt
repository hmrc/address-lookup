resolvers += Resolver.typesafeRepo("releases")
resolvers += MavenRepository(
  "HMRC-open-artefacts-maven2",
  "https://open.artefacts.tax.service.gov.uk/maven2"
)
resolvers += Resolver.url(
  "HMRC-open-artefacts-ivy",
  url("https://open.artefacts.tax.service.gov.uk/ivy2")
)(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "3.24.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.6.0")
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.8")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.1")
