package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.api.generated.definitions.{ ProvisioningRequest, ProvisioningStatus }
import it.agilelab.spinframework.app.features.support.test.HttpResponse

class AsynchronousSpMockTest extends SpMockSuite {

  override def specificProvisioner: SpecificProvisioner = AsynchronousSpMock

  "The asynchronous spmock" should "accept a provision request and return its status from token" in {
    val provisionResponse: HttpResponse[String] = httpClient.post(
      endpoint = "/provision",
      request = ProvisioningRequest(descriptor = "container: somename"),
      bodyClass = classOf[String]
    )

    provisionResponse.status shouldBe 202
    val componentToken = provisionResponse.body

    val provisionStatusResponse = httpClient.get(s"/provision/$componentToken/status", classOf[ProvisioningStatus])

    provisionStatusResponse.status shouldBe 200
    provisionStatusResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

}
