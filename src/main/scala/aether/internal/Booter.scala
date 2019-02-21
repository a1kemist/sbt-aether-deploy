package aether
package internal

import java.io.File

import org.apache.maven.repository.internal._

import org.eclipse.aether.{DefaultRepositorySystemSession, RepositorySystemSession, RepositorySystem}

import org.eclipse.aether.impl._
import org.eclipse.aether.repository.{ProxySelector, LocalRepository}
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory


import sbt.std.TaskStreams

object Booter {
  private def newRepositorySystem(plugin: Boolean): RepositorySystem = {
    val locator = new DefaultServiceLocator()
    locator.addService(classOf[RepositoryLayoutFactory], classOf[SbtPluginLayoutFactory])
    locator.addService(classOf[VersionResolver], classOf[DefaultVersionResolver])
    locator.addService(classOf[VersionRangeResolver], classOf[DefaultVersionRangeResolver])
    locator.addService(classOf[ArtifactDescriptorReader], classOf[DefaultArtifactDescriptorReader])
    locator.addService(classOf[RepositoryConnectorFactory], classOf[org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory])
    locator.setServices(classOf[ProxySelector], SystemPropertyProxySelector())
    if (!plugin) {
      locator.addService(classOf[MetadataGeneratorFactory], classOf[SnapshotMetadataGeneratorFactory])
      locator.addService(classOf[MetadataGeneratorFactory], classOf[VersionsMetadataGeneratorFactory])
    }

    addTransporterFactories(locator)

    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler {
      override def serviceCreationFailed(clazz: Class[_], impl: Class[_], exception: Throwable) {
        println("Service of type %s failed to be crated by impl type %s".format(clazz, impl))
        exception.printStackTrace(System.err)
      }
    })

    val system: RepositorySystem = locator.getService(classOf[RepositorySystem])
    if (system == null) sys.error("Failed to create RepositorySystem. This cannot be good!")
    system
  }

  def apply(localRepoDir: File, streams: TaskStreams[_], plugin: Boolean = false): (RepositorySystem, RepositorySystemSession) = {
    val system = newRepositorySystem(plugin)
    system -> newSession(system, localRepoDir, streams)
  }

  private def newSession(implicit system: RepositorySystem, localRepoDir: File, streams: TaskStreams[_]): RepositorySystemSession = {
    val session = new DefaultRepositorySystemSession()
    val localRepo = new LocalRepository(localRepoDir)
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
    session.setTransferListener(new ConsoleTransferListener(streams.log))
    session.setRepositoryListener(new ConsoleRepositoryListener(streams.log))
    session
  }

  private def addTransporterFactories(locator: DefaultServiceLocator) {
    val services = Seq(
      new HttpTransporterFactory(),
      new FileTransporterFactory().setPriority(10000f)
    )

    locator.setServices(classOf[TransporterFactory], services : _*)
  }
}
