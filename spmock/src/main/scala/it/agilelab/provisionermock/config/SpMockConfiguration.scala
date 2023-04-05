package it.agilelab.provisionermock.config

import com.typesafe.config.Config
import it.agilelab.spinframework.app.config.Configuration

object SpMockConfiguration extends Configuration {
  def apply(): Config = provisionerConfig
}
