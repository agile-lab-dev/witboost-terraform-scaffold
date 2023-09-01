package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.Resource
import it.agilelab.spinframework.app.api.generated.definitions.{
  DescriptorKind,
  ProvisioningRequest,
  SystemError,
  ValidationError,
  ValidationResult
}
import it.agilelab.spinframework.app.api.helpers.HandlerTestBase
import it.agilelab.spinframework.app.features.compiler.{ Compile, CompileResult }
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Response, Status }
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import it.agilelab.spinframework.app.features.compiler.ErrorMessage

class ValidateHandlerTest extends HandlerTestBase {

  "The server" should "return a 200 with no error when the descriptor validation succeeds" in {
    val compileStub: Compile       = _ => CompileResult.success(null)
    val handler                    = new SpecificProvisionerHandler(null, compileStub, null)
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
    val errors                     = Seq(ErrorMessage("error1"), ErrorMessage("error2"))
    val compileStub: Compile       = _ => CompileResult.failure(errors)
    val handler                    = new SpecificProvisionerHandler(null, compileStub, null)
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
    val compileStub: Compile       = _ => throw new NullPointerException
    val handler                    = new SpecificProvisionerHandler(null, compileStub, null)
    val response: IO[Response[IO]] = new Resource[IO]()
      .routes(handler)
      .orNotFound
      .run(
        Request(method = Method.POST, uri = uri"datamesh.specificprovisioner/v1/validate")
          .withEntity(ProvisioningRequest(DescriptorKind.ComponentDescriptor, "a-yaml-descriptor"))
      )
    val expected                   = SystemError("System Error")

    check[SystemError](response, Status.InternalServerError) shouldBe true
  }

}
