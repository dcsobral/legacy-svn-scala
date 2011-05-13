import sbt._
import Keys._
import Defaults.defaultSettings
import java.net.{URL, URLClassLoader}

// object ScalaInstance
// {
//  val VersionPrefix = "version "
//  /** Creates a ScalaInstance using the given provider to obtain the jars and loader.*/
//  def apply(version: String, launcher: xsbti.Launcher): ScalaInstance =
//    apply(version, launcher.getScala(version))
//  def apply(version: String, provider: xsbti.ScalaProvider): ScalaInstance =
//    new ScalaInstance(version, provider.loader, provider.libraryJar, provider.compilerJar, (provider.jars.toSet - provider.libraryJar - provider.compilerJar).toSeq)
// 
//  def apply(scalaHome: File, launcher: xsbti.Launcher): ScalaInstance =
//    apply(libraryJar(scalaHome), compilerJar(scalaHome), launcher, jlineJar(scalaHome))
//  def apply(version: String, scalaHome: File, launcher: xsbti.Launcher): ScalaInstance =
//    apply(version, libraryJar(scalaHome), compilerJar(scalaHome), launcher, jlineJar(scalaHome))
//  def apply(libraryJar: File, compilerJar: File, launcher: xsbti.Launcher, extraJars: File*): ScalaInstance =
//  {
//    val loader = scalaLoader(launcher, libraryJar :: compilerJar :: extraJars.toList)
//    val version = actualVersion(loader)(" (library jar  " + libraryJar.getAbsolutePath + ")")
//    new ScalaInstance(version, loader, libraryJar, compilerJar, extraJars)
//  }
//  def apply(version: String, libraryJar: File, compilerJar: File, launcher: xsbti.Launcher, extraJars: File*): ScalaInstance =
//    new ScalaInstance(version, scalaLoader(launcher, libraryJar :: compilerJar :: extraJars.toList), libraryJar, compilerJar, extraJars)
// 
//  private def compilerJar(scalaHome: File) = scalaJar(scalaHome, "scala-compiler.jar")
//  private def libraryJar(scalaHome: File) = scalaJar(scalaHome, "scala-library.jar")
//  private def jlineJar(scalaHome: File) = scalaJar(scalaHome, "jline.jar")
//  def scalaJar(scalaHome: File, name: String)  =  new File(scalaHome, "lib" + File.separator + name)
// 
//  /** Gets the version of Scala in the compiler.properties file from the loader.*/
//  private def actualVersion(scalaLoader: ClassLoader)(label: String) =
//  {
//    val v = try { Class.forName("scala.tools.nsc.Properties", true, scalaLoader).getMethod("versionString").invoke(null).toString }
//    catch { case cause: Exception => throw new InvalidScalaInstance("Scala instance doesn't exist or is invalid: " + label, cause) }
//    if(v.startsWith(VersionPrefix)) v.substring(VersionPrefix.length) else v
//  }
// 
//  import java.net.{URL, URLClassLoader}
//  private def scalaLoader(launcher: xsbti.Launcher, jars: Seq[File]): ClassLoader =
//    new URLClassLoader(jars.map(_.toURI.toURL).toArray[URL], launcher.topLoader)
// }
// 

// public interface Launcher
// {
//  public static final int InterfaceVersion = 1;
//  public ScalaProvider getScala(String version);
//  public ScalaProvider getScala(String version, String reason);
//  public ClassLoader topLoader();
//  public GlobalLock globalLock();
//  public File bootDirectory();
//  // null if none set
//  public File ivyHome();
// }

// /** Provides access to the jars and classes for a particular version of Scala.*/
// public interface ScalaProvider
// {
//  public Launcher launcher();
//  /** The version of Scala this instance provides.*/
//  public String version();
// 
//  /** A ClassLoader that loads the classes from scala-library.jar and scala-compiler.jar.*/
//  public ClassLoader loader();
//  /** Returns the scala-library.jar and scala-compiler.jar for this version of Scala. */
//  public File[] jars();
//  public File libraryJar();
//  public File compilerJar();
//  /** Creates an application provider that will use 'loader()' as the parent ClassLoader for
//  * the application given by 'id'.  This method will retrieve the application if it has not already
//  * been retrieved.*/
//  public AppProvider app(ApplicationID id);
// }

// public interface AppProvider
// {
//  /** Returns the ScalaProvider that this AppProvider will use. */
//  public ScalaProvider scalaProvider();
//  /** The ID of the application that will be created by 'newMain' or 'mainClass'.*/
//  public ApplicationID id();
// 
//  /** Loads the class for the entry point for the application given by 'id'.  This method will return the same class
//  * every invocation.  That is, the ClassLoader is not recreated each call.*/
//  public Class<? extends AppMain> mainClass();
//  /** Creates a new instance of the entry point of the application given by 'id'.
//  * It is guaranteed that newMain().getClass() == mainClass()*/
//  public AppMain newMain();
//  
//  /** The classpath from which the main class is loaded, excluding Scala jars.*/
//  public File[] mainClasspath();
// 
//  public ComponentProvider components();
// }
// 
// 
// // All internal projects must be listed in `projects`.
// lazy val projects = Seq(root, sub1, sub2)
// 
// // Declare a project in the root directory of the build with ID "root".
// // Declare an execution dependency on sub1.
// lazy val root = Project("root", file(".")) aggregate(sub1)
// 
// // Declare a project with ID 'sub1' in directory 'a'.
// // Declare a classpath dependency on sub2 in the 'test' configuration.
// lazy val sub1 = Project("sub1", file("a")) dependsOn(sub2 % "test")
// 
// // Declare a project with ID 'sub2' in directory 'b'.
// // Declare a configuration dependency on the root project.
// lazy val sub2 = Project("sub2", file("b"), delegates = root :: Nil)

trait ScalaStage {
  def stage: String
  def builder: ScalaInstance
  def builds: ScalaInstance
  def next: Option[ScalaStage]
  
  def depends = List(
    "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test",
    "org.scala-lang" % "jline" % "2.9.0",
    "org.apache.ant" % "ant" % "1.8.2"
  )
  def stageSettings(stage: String, component: String): Seq[Setting[_]] = Seq(
		target <<= target(_ / stage / component),
		crossPaths := false,
		classpathOptions := ClasspathOptions.manual,
		sourceDirectory in Compile <<= baseDirectory(_ / "src"),
		scalaSource in Compile <<= sourceDirectory(_ / component),
		version := "2.10.1-SNAPSHOT",
		scalaVersion := "2.9.0"
	)	
	def unmanaged(files: File => Seq[File]) =
		unmanagedClasspath in Compile <<= unmanagedBase map { base => files(base) map Attributed.blank }

  def librarySettings = stageSettings(stage, "library") ++ Seq(
    unmanaged(ScalaBuild.sharedJars),
    classpathOptions := ClasspathOptions(autoBoot = true, bootLibrary = true, compiler = false, extra = false)
	)
	def compilerSettings = stageSettings(stage, "compiler") ++ Seq(
	  unmanaged(ScalaBuild.sharedJars),
	  mainClass in (Compile, run) := Some("scala.tools.nsc.MainGenericRunner")
	)
  
  lazy val library = (
    Project(stage + "-library", file("."), settings = defaultSettings ++ librarySettings)
  )
  lazy val compiler = (
    Project(stage + "-compiler", file("."), settings = defaultSettings ++ compilerSettings)
    dependsOn library
    aggregate library
  )

  def projects = List(library, compiler)	
}

object ScalaBuild extends Build {
  Defaults.scalaInstanceSetting
  
  def launcher = implicitly[AppConfiguration].provider.scalaProvider.launcher

	def sharedJars(lib: File) = List(
	  lib / "fjbg.jar", 
	  lib / "jline.jar",
	  lib /"ant" / "ant.jar",
	  lib / "msil.jar"
	)
	def extraJars = sharedJars(unmanagedBase)

  object locker extends ScalaStage {
    def stage   = "locker"
    def depends = sharedDependencies
    def builder = ScalaInstance(scalaVersion, launcher)
    def builds  = ScalaInstance(libraryJar, compilerJar, launcher, extraJars: _*)
    def next    = Some(quick)
  }
  object quick extends ScalaStage {
    def stage   = "quick"
    def depends = sharedDependencies
    def builder = locker.builds
    def builds  = ScalaInstance(libraryJar, compilerJar, launcher, extraJars: _*)
    def next    = None
  }

  lazy val projects   = Seq(root) ++ locker.projects ++ quick.projects
  lazy val root       = Project("scala", file(".")) aggregate (quick)
}
