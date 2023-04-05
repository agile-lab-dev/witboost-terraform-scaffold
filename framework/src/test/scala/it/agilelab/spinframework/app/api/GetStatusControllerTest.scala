package it.agilelab.spinframework.app.api

import it.agilelab.spinframework.app.api.dtos.ProvisioningStatusDtoObj._
import it.agilelab.spinframework.app.api.dtos.{
  ProvisionRequestDtoJsonFormat,
  ProvisioningStatusDto,
  ProvisioningStatusDtoJsonFormat,
  SystemErrorDto
}
import it.agilelab.spinframework.app.api.dtos.{
  ProvisionRequestDtoJsonFormat,
  ProvisioningStatusDto,
  ProvisioningStatusDtoJsonFormat,
  SystemErrorDto
}
import it.agilelab.spinframework.app.api.helpers.ControllerTestBase
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus
import it.agilelab.spinframework.app.features.status.GetStatus

class GetStatusControllerTest
    extends ControllerTestBase
    with ProvisionRequestDtoJsonFormat
    with ProvisioningStatusDtoJsonFormat {

  "The server" should "return a 200 - COMPLETED" in {
    val getStatus: GetStatus = _ => ProvisioningStatus.Completed
    val statusEndpoint       = new GetStatusController(getStatus)

    Get(endpoint("/status/component-token")) ~> statusEndpoint.route() ~> check {
      response.status.intValue() shouldBe 200
      responseAs[ProvisioningStatusDto] shouldBe COMPLETED
    }
  }

  "The server" should "return a 200 - FAILED" in {
    val getStatus: GetStatus = _ => ProvisioningStatus.Failed
    val statusEndpoint       = new GetStatusController(getStatus)

    Get(endpoint("/status/component-token")) ~> statusEndpoint.route() ~> check {
      response.status.intValue() shouldBe 200
      responseAs[ProvisioningStatusDto] shouldBe FAILED
    }
  }

  "The server" should "return a 200 - RUNNING" in {
    val getStatus: GetStatus = _ => ProvisioningStatus.Running
    val statusEndpoint       = new GetStatusController(getStatus)

    Get(endpoint("/status/component-token")) ~> statusEndpoint.route() ~> check {
      response.status.intValue() shouldBe 200
      responseAs[ProvisioningStatusDto] shouldBe RUNNING
    }
  }

  "The server" should "return a 500 with meaningful error on status exception" in {
    val getStatus: GetStatus = _ => null // an invalid status is returned
    val statusEndpoint       = new GetStatusController(getStatus)

    Get(endpoint("/status/component-token")) ~> statusEndpoint.route() ~> check {
      response.status.intValue() shouldBe 500
      val body = responseAs[SystemErrorDto]
      body.error should include("IllegalStateException")
      body.error should include("null")
    }
  }
}
