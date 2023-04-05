package it.agilelab.spinframework.app.api

import it.agilelab.spinframework.app.api.dtos.ProvisioningStatusDtoObj.COMPLETED
import it.agilelab.spinframework.app.api.dtos._
import it.agilelab.spinframework.app.features.compiler.{ ErrorMessage, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, Provision, ProvisionResult }
import it.agilelab.spinframework.app.api.dtos.{
  ProvisionRequestDto,
  ProvisionRequestDtoJsonFormat,
  ProvisioningStatusDto,
  ProvisioningStatusDtoJsonFormat,
  SystemErrorDto,
  ValidationErrorDto,
  ValidationErrorDtoJsonFormat
}
import it.agilelab.spinframework.app.api.helpers.ControllerTestBase
import it.agilelab.spinframework.app.features.compiler.{ ErrorMessage, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, Provision, ProvisionResult }

class ProvisionControllerTest
    extends ControllerTestBase
    with ProvisionRequestDtoJsonFormat
    with ProvisioningStatusDtoJsonFormat
    with ValidationErrorDtoJsonFormat {

  class ProvisionStub extends Provision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = ProvisionResult.completed()
    override def doUnprovisioning(yaml: YamlDescriptor): ProvisionResult         = ProvisionResult.completed()
  }

  "The server" should "return a 200 - COMPLETED" in {
    val provisionStub: Provision = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = ProvisionResult.completed()
    }
    val provisionController      = new ProvisionController(provisionStub)

    Post(endpoint("/provision"), ProvisionRequestDto("a-yaml-descriptor")) ~> provisionController.route() ~> check {
      response.status.intValue() shouldBe 200
      responseAs[ProvisioningStatusDto] shouldBe COMPLETED
    }
  }

  "The server" should "return a 202 with a component token" in {
    val provisionStub: Provision = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult =
        ProvisionResult.running(ComponentToken("some-token"))
    }
    val provisionController      = new ProvisionController(provisionStub)

    Post(endpoint("/provision"), ProvisionRequestDto("a-yaml-descriptor")) ~> provisionController.route() ~> check {
      response.status.intValue() shouldBe 202
      responseAs[String] shouldBe "some-token"
    }
  }

  "The server" should "return a 400 with a list of errors" in {
    val errors                   = Seq(ErrorMessage("first error"), ErrorMessage("second error"))
    val provisionStub: Provision = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = ProvisionResult.failure(errors)
    }
    val provisionController      = new ProvisionController(provisionStub)

    Post(endpoint("/provision"), ProvisionRequestDto("a-yaml-descriptor")) ~> provisionController.route() ~> check {
      response.status.intValue() shouldBe 400
      responseAs[ValidationErrorDto] shouldBe fromStrings(Seq("first error", "second error"))
    }
  }

  "The server" should "return a 500 with meaningful error on provision exception" in {
    val provisionStub: Provision = new ProvisionStub {
      override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = throw new NullPointerException
    }
    val controller               = new ProvisionController(provisionStub)

    Post(endpoint("/provision"), ProvisionRequestDto("a-yaml-descriptor")) ~> controller.route() ~> check {
      response.status.intValue() shouldBe 500
      val body = responseAs[SystemErrorDto]
      body.error should include("NullPointerException")
    }
  }

  "The server" should "return a 500 with meaningful error on unprovision exception" in {
    val provisionStub: Provision = new ProvisionStub {
      override def doUnprovisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = throw new NullPointerException
    }
    val controller               = new ProvisionController(provisionStub)

    Post(endpoint("/unprovision"), ProvisionRequestDto("a-yaml-descriptor")) ~> controller.route() ~> check {
      response.status.intValue() shouldBe 500
      val body = responseAs[SystemErrorDto]
      body.error should include("NullPointerException")
    }
  }

}
