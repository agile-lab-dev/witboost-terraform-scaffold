package it.agilelab.spinframework.app.config

import it.agilelab.spinframework.app.api.{
  GetStatusController,
  ProvisionController,
  ServerController,
  ValidateController
}
import it.agilelab.spinframework.app.api.routes.Routes
import it.agilelab.spinframework.app.api.server.HttpServer
import it.agilelab.spinframework.app.features.compiler.{ CompileService, Parser, ParserFactory }
import it.agilelab.spinframework.app.features.provision.ProvisionService

/** Used to link a specific provisioner's configuration
  * to the internal instances of the framework classes.
  *
  * @param specific configuration of a specific provisioner
  */
final private[app] class FrameworkDependencies(specific: SpecificProvisionerDependencies) {

  private val parser: Parser   = ParserFactory.parser()
  private val compileService   = new CompileService(parser, specific.descriptorValidator)
  private val provisionService = new ProvisionService(compileService, specific.cloudProvider)

  /** Provides a server to manage the http requests from a specific provisioner.
    */
  val httpServer: HttpServer = new HttpServer()

  private val serverController    = new ServerController(httpServer)
  private val provisionController = new ProvisionController(provisionService)
  private val getStatusController = new GetStatusController(specific.getStatus)
  private val validateController  = new ValidateController(compileService)

  /** Each controller expose its endpoint with the method route needed by the Akka library.
    */
  val routes = new Routes(
    serverController.route(),
    provisionController.route(),
    getStatusController.route(),
    validateController.route()
  )
}
