package it.agilelab.spinframework.app.typesafeconfig

import com.typesafe.config.Config
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import it.agilelab.spinframework.app.config.Configuration._
import it.agilelab.spinframework.app.api.server.HttpServerDefaults

/** This suite executes test based on a configuration obtained by the merge
  * (automatically executed by Typesafe) of reference.conf and
  * application.conf, that can be found in the test resources folder.
  */
class TypesafeConfigTest extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  "The configuration" should "not be empty" in {
    provisionerConfig.isEmpty shouldBe false
  }

  it should "have the networking configuration" in {
    provisionerConfig.hasPath(networking_httpServer_port) shouldBe true
    provisionerConfig.hasPath(networking_httpServer_interface) shouldBe true
  }

  it should "have coherent values as interface and port" in {
    val port: Int = provisionerConfig.getInt(networking_httpServer_port)
    port shouldBe HttpServerDefaults.defaultPort

    val interface: String = provisionerConfig.getString(networking_httpServer_interface)
    interface shouldBe HttpServerDefaults.defaultInterface
  }

  it should "have the terraform configuration" in {
    provisionerConfig.hasPath(terraform_repositoryPath) shouldBe true
  }

  it should "return the test configuration" in {
    val TEST_KEY: String   = "test"
    val testConfig: Config = provisionerConfig.getConfig(TEST_KEY)

    testConfig.hasPath("key") shouldBe true
    testConfig.getString("key") shouldBe "value"
  }

}
