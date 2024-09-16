package it.agilelab.spinframework.app.api

import cats.effect.IO
import com.typesafe.config.Config
import io.circe.Json
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{
  Log,
  ProvisionInfo,
  ReverseProvisioningRequest,
  ReverseProvisioningStatus,
  SystemError,
  ValidationError
}
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.features.compiler.{ ErrorMessage, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.{ AsyncProvision, ProvisionResult }
import it.agilelab.spinframework.app.features.status.GetStatus
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }
import org.mockito.internal.matchers.Any

class ReverseProvisioningHandlerTest extends HandlerTestBase {

  class ProvisionStub extends AsyncProvision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config): IO[ProvisionResult] =
      IO.pure(ProvisionResult.completed())

    override def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config): IO[ProvisionResult] =
      IO.pure(ProvisionResult.completed())

    override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): IO[ProvisionResult] =
      IO.pure(ProvisionResult.completed())

    override def doValidate(yamlDescriptor: YamlDescriptor): IO[ProvisionResult] =
      IO.pure(ProvisionResult.completed())

    override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): IO[ProvisionResult] =
      IO.pure(ProvisionResult.completed())
  }

  "The server" should "return a 200 - COMPLETED" in {
    val provisionStub: AsyncProvision = new ProvisionStub {
      override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): IO[ProvisionResult] =
        IO.pure(ProvisionResult.completed())
    }
    val handler                       = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]]    = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/reverse-provisioning")
          .withEntity(ReverseProvisioningRequest("urn:some:id", "dev", Some(Json.True), Some(Json.True)))
      )
    val expected                      =
      new ReverseProvisioningStatus(ReverseProvisioningStatus.Status.Completed, Json.Null, Some(Vector.empty))
    check[ReverseProvisioningStatus](response, Status.Ok, Some(expected)) shouldBe true
  }

  "The server" should "return a 400 - Missing params" in {
    val provisionStub: AsyncProvision = new ProvisionStub {
      override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): IO[ProvisionResult] =
        IO.pure(ProvisionResult.completed())
    }
    val handler                       = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]]    = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/reverse-provisioning")
          .withEntity(ReverseProvisioningRequest("urn:some:id", "dev", None, Some(Json.True)))
      )
    check[ValidationError](response, Status.BadRequest) shouldBe true

  }

  "The server" should "return a 400 - Missing catalogInfo" in {
    val provisionStub: AsyncProvision = new ProvisionStub {
      override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): IO[ProvisionResult] =
        IO.pure(ProvisionResult.completed())
    }
    val handler                       = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]]    = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/reverse-provisioning")
          .withEntity(ReverseProvisioningRequest("urn:some:id", "dev", Some(Json.True), None))
      )
    check[ValidationError](response, Status.BadRequest) shouldBe true
  }

  "The server" should "return a 400 error for v1/reverse-provisioning" in {
    val handler                    = new SpecificProvisionerHandler(null, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/reverse-provisioning")
          .withEntity(ReverseProvisioningRequest("", ""))
      )

    check[ValidationError](response, Status.BadRequest) shouldBe true
  }

  it should "return a 500 error for v1/reverse-provisioning/{token}/status" in {
    val getStatus: GetStatus       = _ => IO.raiseError(new Exception("Error!"))
    val handler                    = new SpecificProvisionerHandler(null, null, getStatus)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.GET, uri = uri"datamesh.specificprovisioner/v1/reverse-provisioning/token/status")
      )

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

}
