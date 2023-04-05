package it.agilelab.spinframework.app.api.server

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import it.agilelab.spinframework.app.config.Configuration._
import it.agilelab.spinframework.app.api.routes.Routes

import scala.concurrent.{ ExecutionContextExecutor, Future }

class HttpServer() extends Server with HttpServerDefaults {
  implicit val system: ActorSystem[Nothing]               = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  var serverBinding: Future[Http.ServerBinding] = Future.never

  override def start(routes: Routes, args: Array[String]): Server = {
    val port      = Option(provisionerConfig.getInt(networking_httpServer_port)).getOrElse(defaultPort)
    val interface = Option(provisionerConfig.getString(networking_httpServer_interface)).getOrElse(defaultInterface)
    serverBinding = Http().newServerAt(interface, port).bind(routes.route())
    println(s"Server started: http://$interface:$port/${Controller.basePath}")
    this
  }

  override def stop(): Future[Done] = {
    serverBinding
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete { _ =>
        system.terminate()
      }
    system.whenTerminated
  }
}
