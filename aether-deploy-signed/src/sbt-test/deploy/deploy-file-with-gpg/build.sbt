version in ThisBuild  := "0.1"

name := "deploy-file"

organization := "deploy-file"

scalaVersion := "2.11.6"

publishTo  := Some("foo" at (file(".") / "target" / "repo").toURI.toURL.toString)

overridePublishSignedSettings
