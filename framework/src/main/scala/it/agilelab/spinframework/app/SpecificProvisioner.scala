package it.agilelab.spinframework.app

import cats.effect.{ ExitCode, IO, IOApp }
import com.comcast.ip4s.{ Host, Port }
import it.agilelab.spinframework.app.config.Configuration.{
  networking_httpServer_interface,
  networking_httpServer_port,
  provisionerConfig
}
import it.agilelab.spinframework.app.config.{
  AsynchronousSpecificProvisionerDependencies,
  FrameworkDependencies,
  SpecificProvisionerDependencies,
  SynchronousSpecificProvisionerDependencies
}
import org.http4s.ember.server.EmberServerBuilder

/** This trait is meant to be extended by a specific provisioner implementation to inherit the main
  * and to implement the configuration to use.
  */
trait SpecificProvisioner extends IOApp {

  private lazy val frameworkDependencies = new FrameworkDependencies(specificProvisionerDependencies)

  /** Serves as entry point of a specific provisioner.
    *
    * The server that can execute the operations of provisioning a component and validating its description
    * is started in this method.
    *
    * @param args list of parameters
    */
  def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withPort(Port.fromInt(provisionerConfig.getInt(networking_httpServer_port)).get)
      .withHost(Host.fromString(provisionerConfig.getString(networking_httpServer_interface)).get)
      .withHttpApp(frameworkDependencies.httpApp)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)

  /** Override this method to specify the configuration of a specific provisioner.
    *
    * A configuration must contain the logic for managing a component and it can follow
    * a synchronous semantic ([[SynchronousSpecificProvisionerDependencies]]) or an asynchronous semantic
    * ([[AsynchronousSpecificProvisionerDependencies]]).
    *
    * @return a configuration instance
    */
  def specificProvisionerDependencies: SpecificProvisionerDependencies

}
