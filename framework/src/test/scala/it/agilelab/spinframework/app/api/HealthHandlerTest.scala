package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.api.routes.HealthCheck
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }

class HealthHandlerTest extends HandlerTestBase {

  "The server" should "return a simple health check response " in {
    val response: IO[Response[IO]] = HealthCheck
      .routes[IO]()
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"health")
      )

    check[String](response, Status.Ok, None) shouldBe true
  }

}
