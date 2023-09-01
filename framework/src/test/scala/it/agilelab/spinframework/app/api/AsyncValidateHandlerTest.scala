package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{ SystemError, ValidationRequest }
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }

class AsyncValidateHandlerTest extends HandlerTestBase {

  "The server" should "return a 500 error for v2/validate" in {
    val handler                    = new SpecificProvisionerHandler(null, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v2/validate")
          .withEntity(ValidationRequest(""))
      )

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

  it should "return a 500 error for v2/validate/{token}/status" in {
    val handler                    = new SpecificProvisionerHandler(null, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"datamesh.specificprovisioner/v2/validate/token/status")
      )

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

}
