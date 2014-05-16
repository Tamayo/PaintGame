name := "PaintJava"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaCore,
  cache,
  javaEbean,
  "com.impetus.client" % "kundera-cassandra" % "2.9" excludeAll(ExclusionRule(organization = "org.slf4j")),
  "org.apache.lucene" % "lucene-core" % "4.8.0"
)

resolvers ++= Seq(
"Kundera Public Repository" at "https://oss.sonatype.org/content/repositories/releases",
"Riptano" at "http://mvn.riptano.com/content/repositories/public",
"Kundera missing" at "http://kundera.googlecode.com/svn/maven2/maven-missing-resources",
"Scale 7" at "https://github.com/s7/mvnrepo/raw/master"
)

play.Project.playJavaSettings
