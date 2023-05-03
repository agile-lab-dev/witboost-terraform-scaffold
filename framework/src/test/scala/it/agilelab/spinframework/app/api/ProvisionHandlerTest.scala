package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.features.compiler.YamlDescriptor
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, Provision, ProvisionResult }
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }
import it.agilelab.spinframework.app.api.generated.definitions.{
  ProvisioningRequest,
  SystemError,
  ValidationError,
  ProvisioningStatus => PSDto
}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import it.agilelab.spinframework.app.features.compiler.ErrorMessage
class ProvisionHandlerTest extends HandlerTestBase {
  class ProvisionStub extends Provision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = ProvisionResult.completed()
    override def doUnprovisioning(yaml: YamlDescriptor): ProvisionResult         = ProvisionResult.completed()
  }

  "The server" should "return a 200 - COMPLETED" in {
    val provisionStub: Provision   = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = ProvisionResult.completed()
    }
    val handler                    = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/provision")
          .withEntity(ProvisioningRequest("a-yaml-descriptor"))
      )
    val expected                   = new PSDto(PSDto.Status.Completed)

    check[PSDto](response, Status.Ok, Some(expected)) shouldBe true
  }

  it should "return a 202 with a component token" in {
    val provisionStub: Provision   = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult =
        ProvisionResult.running(ComponentToken("some-token"))
    }
    val handler                    = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/provision")
          .withEntity(ProvisioningRequest("a-yaml-descriptor"))
      )
    val expected                   = "some-token"

    check[String](response, Status.Accepted, Some(expected)) shouldBe true
  }

  it should "return a 400 with a list of errors" in {
    val errors                     = Seq(ErrorMessage("first error"), ErrorMessage("second error"))
    val provisionStub: Provision   = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = ProvisionResult.failure(errors)
    }
    val handler                    = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/provision")
          .withEntity(ProvisioningRequest("a-yaml-descriptor"))
      )
    val expected                   = ValidationError(errors.map(_.description).toVector)

    check[ValidationError](response, Status.BadRequest, Some(expected)) shouldBe true
  }

  it should "return a 500 with meaningful error on provision exception" in {
    val provisionStub: Provision   = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult =
        throw new IllegalArgumentException("error")
    }
    val handler                    = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/provision")
          .withEntity(ProvisioningRequest("a-yaml-descriptor"))
      )
    val expected                   = SystemError("error")

    check[SystemError](response, Status.InternalServerError, Some(expected)) shouldBe true
  }

  it should "return a 500 with meaningful error on unprovision exception" in {
    val provisionStub: Provision   = new ProvisionStub {
      override def doUnprovisioning(yamlDescriptor: YamlDescriptor): ProvisionResult =
        throw new IllegalArgumentException("error")
    }
    val handler                    = new SpecificProvisionerHandler(provisionStub, null, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/unprovision")
          .withEntity(ProvisioningRequest("a-yaml-descriptor"))
      )
    val expected                   = SystemError("error")

    check[SystemError](response, Status.InternalServerError, Some(expected)) shouldBe true
  }

}
