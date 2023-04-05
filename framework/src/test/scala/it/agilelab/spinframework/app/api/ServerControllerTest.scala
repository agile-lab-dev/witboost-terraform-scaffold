package it.agilelab.spinframework.app.api

import akka.Done
import it.agilelab.spinframework.app.api.dtos.ProvisionRequestDtoJsonFormat
import it.agilelab.spinframework.app.api.helpers.ControllerTestBase
import it.agilelab.spinframework.app.api.routes.Routes
import it.agilelab.spinframework.app.api.server.Server
import it.agilelab.spinframework.app.config.Configuration

import scala.concurrent.Future

class ServerControllerTest extends ControllerTestBase with ProvisionRequestDtoJsonFormat {

  private class ServerStub() extends Server {
    var running = true
    override def start(routes: Routes, args: Array[String]): Server = { running = true; this }
    override def stop(): Future[Done] = { running = false; Future(Done) }
  }

  private val server     = new ServerStub()
  private val controller = new ServerController(server)

  "The server" should "return a simple health check response " in {
    Get(endpoint("")) ~> controller.route() ~> check {
      responseAs[String] shouldEqual "server-running"
    }
  }

}
