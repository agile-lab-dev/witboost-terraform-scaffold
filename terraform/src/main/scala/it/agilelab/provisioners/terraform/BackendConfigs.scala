package it.agilelab.provisioners.terraform

/** A companion to [[BackendConfigs]] class to group some convenience methods.
  */
object BackendConfigs {

  /** Allows to specify that no backend config is provided for the command.
    *
    * @return an empty instance of [[BackendConfigs]]
    */
  def noConfig() = new BackendConfigs(Map.empty, ("", ""))

  /** Allows easy construction of backend config instance.
    *
    * @param stateKey the key/value representing the state key
    * @param configValuePairs a list of configs, each expressed as a name-value pair.
    * @return an instance of [[BackendConfigs]]
    */
  def configs(stateKey: (String, String), configValuePairs: (String, String)*): BackendConfigs =
    new BackendConfigs(configValuePairs.toMap, stateKey)

}

/** A list of backend configs to parameterize Terraform configuration.
  * Each backend config is a name-value pair.
  *
  * @param backendConfigs a map the contains all the backend configs.
  * @param stateKey the key/value representing the state key
  */
class BackendConfigs(backendConfigs: Map[String, String], stateKey: (String, String)) {

  /** Converts each backend config in a Terraform compliant format.
    * Ex. the config ("key" -> "my-state-key") is translated to
    * the string '-backend-config key="my-state-key"'
    *
    * @return the configs as a list of parameters for command line.
    */
  def toOptions: String =
    (backendConfigs + stateKey).map(buildOption).mkString(" ")

  private def buildOption(variable: (String, String)): String = {
    val (name, value) = variable
    s"""-backend-config="$name=$value""""
  }
}
