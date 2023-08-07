package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{ SystemError, ProvisioningStatus => PSDto }
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus
import it.agilelab.spinframework.app.features.status.GetStatus
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }

class GetStatusHandlerTest extends HandlerTestBase {

  "The server" should "return a 200 - COMPLETED" in {
    val getStatus: GetStatus       = _ => ProvisioningStatus.Completed
    val handler                    = new SpecificProvisionerHandler(null, null, getStatus)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"datamesh.specificprovisioner/provision/token/status")
      )
    val expected                   = new PSDto(PSDto.Status.Completed)

    check[PSDto](response, Status.Ok, Some(expected)) shouldBe true
  }

  it should "return a 200 - FAILED" in {
    val getStatus: GetStatus       = _ => ProvisioningStatus.Failed
    val handler                    = new SpecificProvisionerHandler(null, null, getStatus)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"datamesh.specificprovisioner/provision/token/status")
      )
    val expected                   = new PSDto(PSDto.Status.Failed)

    check[PSDto](response, Status.Ok, Some(expected)) shouldBe true
  }

  it should "return a 200 - RUNNING" in {
    val getStatus: GetStatus       = _ => ProvisioningStatus.Running
    val handler                    = new SpecificProvisionerHandler(null, null, getStatus)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"datamesh.specificprovisioner/provision/token/status")
      )
    val expected                   = new PSDto(PSDto.Status.Running)

    check[PSDto](response, Status.Ok, Some(expected)) shouldBe true
  }

  it should "return a 500 with meaningful error on status exception" in {
    val getStatus: GetStatus       = _ => null // an invalid status is returned
    val handler                    = new SpecificProvisionerHandler(null, null, getStatus)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"datamesh.specificprovisioner/provision/token/status")
      )
    val expected                   = SystemError("null")

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

}
