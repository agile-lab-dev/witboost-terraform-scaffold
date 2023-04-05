package it.agilelab.spinframework.app.api.routes

import akka.http.scaladsl.server.{ Directives, Route }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

class Routes(routes: Route*) {
  // creates a concatenation of all routes exposed via the api
  def route(): Route =
    Directives.concat(routes: _*)
}
