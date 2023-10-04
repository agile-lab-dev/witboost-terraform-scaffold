package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.ConfigFactory
import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub
import it.agilelab.spinframework.app.config.PrincipalMapperPluginLoader
import it.agilelab.spinframework.app.features.compiler._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class UpdateAclServiceTest extends AnyFlatSpec with should.Matchers {
  val parser: Parser = ParserFactory.parser()

  "The provision service" should "return a 'success' result for successfully loading the plugin" in {

    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProvider                  = CloudProviderStub.updateAcl((_, _) => ProvisionResult.completed())
    val principalMapperPluginLoader    = new PrincipalMapperPluginLoader()

    val provisionService: ProvisionService =
      new ProvisionService(compile, cloudProvider, principalMapperPluginLoader)

    val jsonDescriptor = JsonDescriptor("{}")

    val provisionResult: ProvisionResult = provisionService.doUpdateAcl(
      jsonDescriptor,
      Set("Alice", "Bob"),
      ConfigFactory.parseString(
        """
          |terraform {
          |  principalMappingPlugin {
          |    pluginClass = "it.agilelab.plugin.principalsmapping.impl.identity.IdentityMapperFactory"
          |    identity {}
          |  }
          |}
          |""".stripMargin
      )
    )

    provisionResult.isSuccessful shouldBe true
  }

  "The provision service" should "return a 'failure' result for unsuccessfully loading the plugin" in {

    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProvider                  = CloudProviderStub.updateAcl((_, _) => ProvisionResult.completed())
    val principalMapperPluginLoader    =
      new PrincipalMapperPluginLoader()

    val provisionService: ProvisionService =
      new ProvisionService(compile, cloudProvider, principalMapperPluginLoader)

    val jsonDescriptor = JsonDescriptor("{}")

    val provisionResult: ProvisionResult = provisionService.doUpdateAcl(
      jsonDescriptor,
      Set("Alice", "Bob"),
      ConfigFactory.parseString("""
                                  |terraform {
                                  |  principalMappingPlugin {
                                  |    pluginClass = ""
                                  |  }
                                  |}
          """.stripMargin)
    )

    provisionResult.isSuccessful shouldBe false

  }

  "The provision service" should "return a 'failure' result for unsuccessfully mapping the subjects" in {

    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProvider                  = CloudProviderStub.updateAcl((_, _) => ProvisionResult.completed())
    val principalMapperPluginLoader    = new PrincipalMapperPluginLoader()

    val provisionService: ProvisionService =
      new ProvisionService(compile, cloudProvider, principalMapperPluginLoader)

    val jsonDescriptor = JsonDescriptor("{}")

    val provisionResult: ProvisionResult = provisionService.doUpdateAcl(
      jsonDescriptor,
      Set("Nevil"),
      ConfigFactory.parseString("""
                                  |terraform {
                                  |  principalMappingPlugin {
                                  |    pluginClass = "it.agilelab.spinframework.app.config.FakeMapperFactory"
                                  |    fake {}
                                  |  }
                                  |}
    """.stripMargin)
    )

    provisionResult.isSuccessful shouldBe false

  }

}
