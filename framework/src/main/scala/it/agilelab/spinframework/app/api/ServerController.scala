package it.agilelab.spinframework.app.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.agilelab.spinframework.app.api.server.{ Controller, Server }

class ServerController(val server: Server) extends Controller {

  def route(): Route =
    path(basePath) {
      get {
        complete(StatusCodes.OK, "server-running")
      }
    }
}
