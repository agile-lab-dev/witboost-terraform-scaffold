package it.agilelab.spinframework.app.api

import cats.effect.IO
import com.typesafe.config.Config
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{
  DescriptorKind,
  ProvisionInfo,
  ProvisioningRequest,
  SystemError,
  ValidationError,
  ValidationResult
}
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.features.compiler.{ Compile, CompileResult, ErrorMessage, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.{ AsyncProvision, Provision, ProvisionResult }
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }

class ValidateHandlerTest extends HandlerTestBase {

  class ProvisionStub extends Provision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config): ProvisionResult               =
      ProvisionResult.completed()
    override def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config): ProvisionResult  =
      ProvisionResult.completed()
    override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): ProvisionResult =
      ProvisionResult.completed()
    override def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult                                =
      ProvisionResult.completed()
  }

  "The server" should "return a 200 with no error when the descriptor validation succeeds" in {
    val compileStub: Compile       = _ => CompileResult.success(null)
    val handler                    =
      new SpecificProvisionerHandler(AsyncProvision.fromSyncProvision(new ProvisionStub()), compileStub, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/validate")
          .withEntity(ProvisioningRequest(DescriptorKind.ComponentDescriptor, "a-yaml-descriptor"))
      )
    val expected                   = ValidationResult(valid = true)

    check[ValidationResult](response, Status.Ok, Some(expected)) shouldBe true
  }

  it should "return a 200 with a list of errors when the validation fails" in {
    val errors           = Seq(ErrorMessage("error1"), ErrorMessage("error2"))
    val failingProvision = new ProvisionStub {
      override def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult =
        ProvisionResult.failure(errors)
    }

    val compileStub: Compile       = _ => CompileResult.failure(errors)
    val handler                    = new SpecificProvisionerHandler(AsyncProvision.fromSyncProvision(failingProvision), compileStub, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/validate")
          .withEntity(ProvisioningRequest(DescriptorKind.ComponentDescriptor, "a-yaml-descriptor"))
      )
    val expected                   = ValidationResult(valid = false, error = Some(ValidationError(errors.map(_.description).toVector)))

    check[ValidationResult](response, Status.Ok, Some(expected)) shouldBe true
  }

  it should "return a 500 with with meaningful error on validate exception" in {
    val failing = new ProvisionStub {
      override def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult =
        throw new Exception("Error!")
    }

    val compileStub: Compile       = _ => throw new NullPointerException
    val handler                    = new SpecificProvisionerHandler(AsyncProvision.fromSyncProvision(failing), compileStub, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/validate")
          .withEntity(ProvisioningRequest(DescriptorKind.ComponentDescriptor, "a-yaml-descriptor"))
      )

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

}
