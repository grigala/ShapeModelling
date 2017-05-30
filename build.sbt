name := "ShapeModelling"

organization := "ch.unibas.cs.gravis.grigala"

version := "1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "shapemodelling unibas" at "http://shapemodelling.cs.unibas.ch/repository/public"
)

libraryDependencies ++= Seq(
  "ch.unibas.cs.gravis" %% "scalismo" % "0.10.+"
)

libraryDependencies += "ch.unibas.cs.gravis" % "scalismo-ui_2.11" % "0.6.+"

libraryDependencies += "ch.unibas.cs.gravis" % "scalismo-sampling_2.11" % "develop-SNAPSHOT"

dependencyOverrides += "ch.unibas.cs.gravis" % "scalismo-native-all" % "3.1.1"