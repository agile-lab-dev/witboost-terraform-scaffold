package it.agilelab.spinframework.app.api

import it.agilelab.spinframework.app.api.dtos._
import it.agilelab.spinframework.app.api.dtos.{
  ProvisionRequestDto,
  ProvisionRequestDtoJsonFormat,
  SystemErrorDto,
  ValidateResponseDto,
  ValidateResponseDtoJsonFormat
}
import it.agilelab.spinframework.app.api.helpers.ControllerTestBase
import it.agilelab.spinframework.app.features.compiler.{ Compile, CompileResult, ErrorMessage }

class ValidateControllerTest
    extends ControllerTestBase
    with ProvisionRequestDtoJsonFormat
    with ValidateResponseDtoJsonFormat {

  "The server" should "return a 200 with no error when the descriptor validation succeeds" in {
    val compileStub: Compile = _ => CompileResult.success(null)
    val validateController   = new ValidateController(compileStub)

    Post(endpoint("/validate"), ProvisionRequestDto("a-yaml-descriptor")) ~> validateController.route() ~> check {
      response.status.intValue() shouldBe 200
      responseAs[ValidateResponseDto] shouldBe dtos.ValidateResponseDto(
        valid = true,
        fromStrings(Seq.empty)
      )
    }
  }

  "The server" should "return a 200 with a list of errors when the validation fails" in {
    val compileStub: Compile = _ => CompileResult.failure(Seq(ErrorMessage("error1"), ErrorMessage("error2")))
    val validateController   = new ValidateController(compileStub)

    Post(endpoint("/validate"), ProvisionRequestDto("a-yaml-descriptor")) ~> validateController.route() ~> check {
      response.status.intValue() shouldBe 200
      val validationResponse = responseAs[ValidateResponseDto]
      validationResponse.valid shouldBe false
      validationResponse.error.errors should contain("error1")
      validationResponse.error.errors should contain("error2")
    }
  }

  "The server" should "return a 500 with with meaningful error on validate exception" in {
    val compileStub: Compile = _ => throw new NullPointerException
    val controller           = new ValidateController(compileStub)

    Post(endpoint("/validate"), ProvisionRequestDto("a-yaml-descriptor")) ~> controller.route() ~> check {
      response.status.intValue() shouldBe 500
      val body = responseAs[SystemErrorDto]
      body.error should include("NullPointerException")
    }
  }
}
