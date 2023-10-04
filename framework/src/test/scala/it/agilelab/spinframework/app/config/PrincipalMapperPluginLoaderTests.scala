package it.agilelab.spinframework.app.config

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.Failure

class PrincipalMapperPluginLoaderTests extends AnyFlatSpec with should.Matchers {

  "PrincipalMapperPluginLoader" should "load the service with success" in {

    val config                      = ConfigFactory.parseString(
      """
        |terraform {
        |    repositoryPath = "src/main/resources/terraform"
        |    descriptorToVariablesMapping = {}
        |    principalMappingPlugin {
        |        pluginClass = "it.agilelab.plugin.principalsmapping.impl.identity.IdentityMapperFactory"
        |        identity {}
        |    }
        |}
        |""".stripMargin
    )
    val principalMapperPluginLoader = new PrincipalMapperPluginLoader()
    val mapper                      = principalMapperPluginLoader.load(config)
    mapper.isSuccess shouldBe true

  }

  "PrincipalMapperPluginLoader" should "fail to load if the plugin is not correctly configured" in {

    val config = ConfigFactory.parseString(
      """
        |terraform {
        |    repositoryPath = "src/main/resources/terraform"
        |    descriptorToVariablesMapping = {}
        |    principalMappingPlugin {
        |        pluginClass = "it.agilelab.plugin.principalsmapping.impl.identity.IdentityMapperFactory"
        |    }
        |}
        |""".stripMargin
    )

    val principalMapperPluginLoader = new PrincipalMapperPluginLoader()
    val mapper                      = principalMapperPluginLoader.load(config)
    mapper.isSuccess shouldBe false

  }

  "PrincipalMapperPluginLoader" should "fail to load if the plugin class is wrong" in {

    val config = ConfigFactory.parseString("""
                                             |terraform {
                                             |    repositoryPath = "src/main/resources/terraform"
                                             |    descriptorToVariablesMapping = {}
                                             |    principalMappingPlugin {
                                             |        pluginClass = "java.lang.Thread"
                                             |    }
                                             |}

                                             |""".stripMargin)

    val principalMapperPluginLoader = new PrincipalMapperPluginLoader()
    val mapper                      = principalMapperPluginLoader.load(config)
    mapper.isSuccess shouldBe false

  }

}
