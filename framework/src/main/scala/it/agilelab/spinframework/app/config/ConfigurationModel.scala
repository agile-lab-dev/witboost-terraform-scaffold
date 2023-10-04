package it.agilelab.spinframework.app.config

/** Contains the main entries of the configuration files used inside the framework.
  */
trait ConfigurationModel {
  private val networking: String                   = "networking"
  private val httpServer: String                   = "httpServer"
  private val terraform: String                    = "terraform"
  private val interface: String                    = "interface"
  private val port: String                         = "port"
  private val repositoryPath: String               = "repositoryPath"
  private val descriptorToVariablesMapping: String = "descriptorToVariablesMapping"
  private val principalmappingPlugin: String       = "principalMappingPlugin"
  private val principalmappingPluginClass: String  = "pluginClass"

  val datameshProvisioner: String = "datameshProvisioner"

  val networking_httpServer_interface: String = s"$networking.$httpServer.$interface"
  val networking_httpServer_port: String      = s"$networking.$httpServer.$port"
  val terraform_repositoryPath: String        = s"$terraform.$repositoryPath"
  val descriptor_mapping: String              = s"$terraform.$descriptorToVariablesMapping"
  val principalmapping_plugin: String         = s"$terraform.$principalmappingPlugin"
  val principalmapping_plugin_class: String   = s"$terraform.$principalmappingPlugin.$principalmappingPluginClass"

}

object ConfigurationModel extends ConfigurationModel
