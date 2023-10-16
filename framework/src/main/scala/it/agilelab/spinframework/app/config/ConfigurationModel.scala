package it.agilelab.spinframework.app.config

/** Contains the main entries of the configuration files used inside the framework.
  */
trait ConfigurationModel {
  private val networking: String = "networking"
  private val httpServer: String = "httpServer"
  private val interface: String  = "interface"
  private val port: String       = "port"

  val datameshProvisioner: String = "datameshProvisioner"

  val networking_httpServer_interface: String = s"$networking.$httpServer.$interface"
  val networking_httpServer_port: String      = s"$networking.$httpServer.$port"
  val terraform: String                       = "terraform"
  val repositoryPath: String                  = "repositoryPath"
  val descriptorToVariablesMapping: String    = "descriptorToVariablesMapping"
  val principalMappingPlugin: String          = "principalMappingPlugin"
  val principalMappingPluginClass: String     = s"$principalMappingPlugin.pluginClass"
}

object ConfigurationModel extends ConfigurationModel
