import com.typesafe.sbt.SbtAspectj._
import sbtassembly.MergeStrategy

name := "My Project"
 
version := "1.0"
 
scalaVersion := "2.11.5"

resolvers ++= Seq(
  "snapshots"           at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"            at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "rediscala" at "http://dl.bintray.com/etaty/maven",
  "Kamon Repository" at "http://repo.kamon.io",
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
)    

libraryDependencies ++= {
  val akkaVersion = "2.3.14"
  val kamonVersion = "0.4.0"      
  Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,         
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.mashupbots.socko" % "socko-webserver_2.11" % "0.6.0" excludeAll(
      ExclusionRule(organization = "ch.qos.logback"),
      ExclusionRule(organization = "com.typesafe.akka")
      ),
  "org.json4s" % "json4s-jackson_2.11" % "3.2.11",
  "com.etaty.rediscala" %% "rediscala" % "1.4.0" excludeAll(
    ExclusionRule(organization = "com.typesafe.akka")
    ),
  "com.notnoop.apns" % "apns" % "0.2.3",
  "com.googlecode.json-simple" % "json-simple" % "1.1.1",
  "com.dbay.apns4j" % "dbay-apns4j" % "1.0-SNAPSHOT",
  "com.ganyo" % "gcm-server" % "1.0.2",     
  "io.dropwizard.metrics" % "metrics-jvm" % "3.1.0",
  "com.amazonaws" % "aws-java-sdk-autoscaling" % "1.9.24",
  "com.amazonaws" % "aws-java-sdk-core" % "1.9.24",
  "com.amazonaws" % "aws-java-sdk-ec2" % "1.9.24",
  "org.mashupbots.socko" % "socko-webserver_2.11" % "0.6.0",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.novocode" % "junit-interface" % "0.8" % "test->default",
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "io.kamon" % "kamon-core_2.11" % kamonVersion excludeAll(
    ExclusionRule(organization = "com.typesafe.akka")
  ),      
  "io.kamon" % "kamon-statsd_2.11" % kamonVersion excludeAll(
    ExclusionRule(organization = "com.typesafe.akka")
    ),
  "io.kamon" % "kamon-system-metrics_2.11" % kamonVersion,      
  "io.kamon" % "kamon-akka_2.11" % kamonVersion % "provided",
  "io.kamon" % "kamon-akka-remote_2.11" % kamonVersion % "provided",
  "io.kamon" % "kamon-scala_2.11" % kamonVersion % "provided",      
  "io.kamon" % "kamon-log-reporter_2.11" % kamonVersion,      
  "org.aspectj" % "aspectjweaver" % "1.8.4",
  "org.fusesource" % "sigar" % "1.6.4",
  "com.github.scopt" % "scopt_2.11" % "3.3.0",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.9",
  "com.typesafe.play" % "play-json_2.11" % "2.4.0-M3",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.5.1")
}

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-Xlog-implicits")

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
    
parallelExecution in Test := false
 
libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.3.14"

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }

compileOrder := CompileOrder.JavaThenScala
    
aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj   
    
val libPath = Seq("resources").mkString(java.io.File.pathSeparator)    
    
javaOptions in run += s"-Djava.library.path=$libPath"    
    
fork in run := true 

//link: https://gist.github.com/colestanfield/fac042d3108b0c06e952
// Create a new MergeStrategy for aop.xml files
val aopMerge: MergeStrategy = new MergeStrategy {
  val name = "aopMerge"
  import scala.xml._
  import scala.xml.dtd._
 
  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
    val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
    val file = MergeStrategy.createMergeTarget(tempDir, path)
    val xmls: Seq[Elem] = files.map(XML.loadFile)
    val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
    val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
    val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
    val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
    val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
    val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
    val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
    XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
    IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
    Right(Seq(file -> path))
  }
}
 
// SBT assembly , Ref: https://github.com/sbt/sbt-assembly    
assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
  case PathList("com", "dbay", "apns4j", xs @ _*) => MergeStrategy.first            
  case PathList("play", "core", "server", xs @ _*) => MergeStrategy.first
  case PathList("org", "mashupbots", "socko", xs @ _*) => MergeStrategy.first    
  case PathList("org", "aspectj", "internal", "lang", xs @ _*) => MergeStrategy.first        
  case PathList("org", "aspectj", "runtime", "reflect", xs @ _*) => MergeStrategy.first
  case PathList("org", "iq80", "leveldb", xs @ _*) => MergeStrategy.first
  case PathList("org", "hyperic", "sigar", xs @ _*) => MergeStrategy.first 
  case PathList("META-INF", "aop.xml") => aopMerge    
// use outside file      
  case "logback.xml"     => MergeStrategy.discard
  case "application.conf"     => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
} 

assemblyJarName in assembly := "AkkaTest.jar"
    
// To skip the test during assembly
test in assembly := {}

mainClass in assembly := Some("test.LocalMain")

// Splitting your project and deps JARs
assemblyDefaultJarName in assemblyPackageDependency <<= (name, version) map { (name, version) => "deps.jar" }
//TODO for local test use, need remark
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = false)
    
// SBT Native Packager, for Docker support, Ref: https://github.com/sbt/sbt-native-packager
//enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)  
