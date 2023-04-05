package it.agilelab.spinframework.app.api.server

/** Default values for port and interface. This values are also used in tests
  * and are used as fallback for the HttpServer instance.
  */
trait HttpServerDefaults {
  val defaultPort: Int         = 8080
  val defaultInterface: String = "0.0.0.0"
}

object HttpServerDefaults extends HttpServerDefaults
