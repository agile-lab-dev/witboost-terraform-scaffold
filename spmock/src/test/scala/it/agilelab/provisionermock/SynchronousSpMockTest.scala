package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.api.dtos.{ ProvisionRequestDto, ProvisioningStatusDto, ValidationErrorDto }
import it.agilelab.spinframework.app.features.support.test.HttpResponse
import org.scalatest.flatspec.AnyFlatSpec

class SynchronousSpMockTest extends SpMockSuite {
  override def specificProvisioner: SpecificProvisioner = SynchronousSpMock

  "The synchronous spmock" should "accept a provision request and return the provisioning status" in {
    val provisionResponse: HttpResponse[ProvisioningStatusDto] = httpClient.post(
      endpoint = "/provision",
      request = ProvisionRequestDto(descriptor = "container: somename"),
      bodyClass = classOf[ProvisioningStatusDto]
    )

    provisionResponse.status shouldBe 200
    provisionResponse.body.status shouldBe "COMPLETED"
  }

}
