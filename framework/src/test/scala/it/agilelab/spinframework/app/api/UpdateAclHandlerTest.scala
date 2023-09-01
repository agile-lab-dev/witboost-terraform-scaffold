package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{ ProvisionInfo, SystemError, UpdateAclRequest }
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }

class UpdateAclHandlerTest extends HandlerTestBase {

  "The server" should "return a 500 error" in {
    val handler                    = new SpecificProvisionerHandler(null, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/updateacl")
          .withEntity(UpdateAclRequest(refs = Vector.empty, provisionInfo = ProvisionInfo("", "")))
      )

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

}
