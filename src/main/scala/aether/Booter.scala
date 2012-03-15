package aether

import org.sonatype.aether.repository.LocalRepository
import org.sonatype.aether.{RepositorySystemSession, RepositorySystem}
import java.io.File
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory
import org.apache.maven.wagon.Wagon
import org.sonatype.maven.wagon.AhcWagon
import org.sonatype.aether.connector.wagon.{WagonRepositoryConnectorFactory, WagonProvider}
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory
import org.apache.maven.repository.internal.{MavenServiceLocator, MavenRepositorySystemSession}

object Booter {
  def newRepositorySystem = {
    val locator = new MavenServiceLocator();
    locator.addService(classOf[RepositoryConnectorFactory], classOf[FileRepositoryConnectorFactory])
    locator.addService(classOf[RepositoryConnectorFactory], classOf[WagonRepositoryConnectorFactory])
    locator.setServices(classOf[WagonProvider], ManualWagonProvider)
    locator.getService(classOf[RepositorySystem])
  }

  def newSession(implicit system: RepositorySystem, localRepoDir: File): RepositorySystemSession = {
      val session = new MavenRepositorySystemSession()

      val localRepo = new LocalRepository(localRepoDir);
      session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo))
      session.setTransferListener(new ConsoleTransferListener())
      session.setRepositoryListener(new ConsoleRepositoryListener())
      session
  }

  object ManualWagonProvider extends WagonProvider {
    def lookup(roleHint: String ): Wagon = {
      roleHint match {
        case "http" => new AhcWagon()
        case _ => throw new IllegalArgumentException("Unknown wagon type")
      }
    }

    def release(wagon: Wagon){}
  }
}