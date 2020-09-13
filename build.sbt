import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport.PathList

lazy val commonSettings = Seq(
  organization := "otonomo",
  version := "1.0",
  scalaVersion := "2.11.12"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "otonomo",
  ).
  enablePlugins(AssemblyPlugin)


//seq(assemblySettings: _*)

name := "otonomo"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion = "2.3.2"


libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.3",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.8"
//  ,
//  "com.databricks" % "spark-csv_2.10" % "1.4.0",
//  "org.apache.commons" % "commons-csv" % "1.4"
)

//enablePlugins(UniversalPlugin)
//enablePlugins(JavaAppPackaging)
//enablePlugins(JavaServerAppPackaging)

sbtassembly.AssemblyKeys.assemblyMergeStrategy in assembly := {
  //case PathList("META-INF", _*) => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") =>
    println("===============META-INF====================")
    MergeStrategy.discard
  case PathList(xs @ _*) if xs.last endsWith ".SF" =>
    println("===============OTHER META-INF====================")
    println(xs)
    MergeStrategy.discard
  case PathList(xs @ _*) if xs.last endsWith ".DSA" =>
    println("===============OTHER META-INF====================")
    println(xs)
    MergeStrategy.discard
  case x =>
    //println("=====================================")
    //println(x)
    MergeStrategy.first
}

//assemblyExcludedJars in assembly := {
//  val cp = (fullClasspath in assembly).value
//  cp filter { el =>
//      el.data.getName.contains("commons-beanutils-1.7.0.jar")
//  }
//}

// we specify the name for our fat jar
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true, includeDependency = true)

mainClass in assembly := some("VehicleDataProcessor")
assemblyJarName in assembly := "VehicleDataProcessorFatJar.jar"
// the bash scripts classpath only needs the fat jar
//scriptClasspath := Seq( (assemblyJarName in assembly).value )

//// removes all jar mappings in universal and appends the fat jar
//mappings in Universal := {
//  // universalMappings: Seq[(File,String)]
//  val universalMappings = (mappings in Universal).value
//  val fatJar = (assembly in Compile).value
//  // removing means filtering
//  val filtered = universalMappings filter {
//    case (file, name) =>  ! name.endsWith(".jar")
//  }
//  // add the fat jar
//  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
//}

//mappings in Universal += (packageBin in Compile).value -> "VehicleDataProcessorFatJar.jar"
//mappings in Universal := {
//  val universalMappings = (mappings in Universal).value
//  val fatJar = (assembly in Compile).value
//  val filtered = universalMappings filter {
//    case (file, name) =>  ! name.endsWith(".jar")
//  }
//  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
//}


