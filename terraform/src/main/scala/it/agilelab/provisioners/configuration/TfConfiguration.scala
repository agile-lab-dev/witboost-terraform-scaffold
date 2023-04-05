package it.agilelab.provisioners.configuration

import com.typesafe.config.Config
import it.agilelab.spinframework.app.config.Configuration

object TfConfiguration extends Configuration {
  def apply(): Config = provisionerConfig
}
