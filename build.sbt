ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "ss-api",
    libraryDependencies ++= Seq(
      guice,
      evolutions,
      filters,
      "org.postgresql" % "postgresql" % "42.7.5",
      "org.playframework" %% "play-slick" % "6.2.0",
      "org.playframework" %% "play-slick-evolutions" % "6.2.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.typelevel" %% "cats-core" % "2.13.0",
      "com.auth0" % "java-jwt" % "4.4.0",
      "redis.clients" % "jedis" % "5.1.0"
    ),
    // Add routing settings for Scala 3
    scalacOptions ++= Seq("-Xignore-scala2-macros", "-source:3.3"),
    // Ensure routes file is properly handled
    routesImport += "controllers._",
    // Add this to correctly configure assets
    PlayKeys.playDefaultPort := 9000
  )
