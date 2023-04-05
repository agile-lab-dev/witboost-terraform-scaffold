package it.agilelab.spinframework.app

import it.agilelab.spinframework.app.api.server.HttpServer
import it.agilelab.spinframework.app.config.{
  AsynchronousSpecificProvisionerDependencies,
  FrameworkDependencies,
  SpecificProvisionerDependencies,
  SynchronousSpecificProvisionerDependencies
}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success, Try }

/** This trait is meant to be extended by a specific provisioner implementation to inherit the main
  * and to implement the configuration to use.
  */
trait SpecificProvisioner {

  private lazy val frameworkDependencies = new FrameworkDependencies(specificProvisionerDependencies)

  /** Serves as entry point of a specific provisioner.
    *
    * The server that can execute the operations of provisioning a component and validating its description
    * is started in this method.
    *
    * @param args list of parameters
    */
  final def main(args: Array[String]): Unit =
    frameworkDependencies.httpServer.start(frameworkDependencies.routes, args)

  /** Use this to stop the httpServer instance of a specific provisioner.
    * The [[HttpServer]] is stopped
    * using a method of the class [[akka.actor.ActorSystem]] that terminates the actor.
    */
  final def teardown(): Unit =
    Try(Await.ready(frameworkDependencies.httpServer.stop(), 15.second)) match {
      case Success(result)    =>
        result.value match {
          case Some(_) => println("server-stopped")
          case None    => throw new RuntimeException("Error while stopping framework")
        }
      case Failure(exception) =>
        println(exception.getMessage)
        throw exception
    }

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
