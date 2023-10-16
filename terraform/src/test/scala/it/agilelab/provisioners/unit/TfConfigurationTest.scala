package it.agilelab.provisioners.unit

import it.agilelab.provisioners.configuration.TfConfiguration._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TfConfigurationTest extends AnyFlatSpec with should.Matchers {

  "TfConfiguration" should "override the httpServer port" in {
    provisionerConfig.getInt(networking_httpServer_port) shouldBe 8081
  }

  it should "read the interface value" in {
    provisionerConfig.getString(networking_httpServer_interface) shouldBe "0.0.0.0"
  }

}
