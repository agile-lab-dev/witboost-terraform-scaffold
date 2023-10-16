package it.agilelab.provisioners

import com.typesafe.config.{ Config, ConfigFactory }
import it.agilelab.spinframework.app.config.ConfigurationModel

object TestConfig {

  /** Load a custom configuration to be used during tests
    *
    *  @param configResource path of the config file to load.
    *                        The path is relative to `src/test/resources`
    *  @return a [[Config]] object containing only the
    *          fields specified in the file `configResource`.
    *          No other default configurations are merged.
    */
  def load(configResource: String): Config =
    ConfigFactory.parseResources(configResource).getConfig(ConfigurationModel.datameshProvisioner)
}
