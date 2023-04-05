package it.agilelab.spinframework.app.api.server

import akka.Done
import it.agilelab.spinframework.app.api.routes.Routes

import scala.concurrent.Future

trait Server {
  def start(routes: Routes, args: Array[String] = Array.empty): Server
  def stop(): Future[Done]
}
