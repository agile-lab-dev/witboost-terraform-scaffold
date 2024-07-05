package it.agilelab.provisioners

import it.agilelab.provisioners.configuration.{ AsyncTfDependencies, SyncTfDependencies }
import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.config.Configuration.{
  async_config,
  async_provision_enabled,
  async_provision_pool_size,
  provisionerConfig
}
import it.agilelab.spinframework.app.config.SpecificProvisionerDependencies
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

object Main extends SpecificProvisioner {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override val specificProvisionerDependencies: SpecificProvisionerDependencies = {
    Try(provisionerConfig.getBoolean(async_provision_enabled)) match {
      case Success(true)  =>
        logger.info("Initializing provisioner in async provisioning mode")
        new AsyncTfDependencies(
          Try(provisionerConfig.getObject(async_config)).get.toConfig,
          Try(provisionerConfig.getInt(async_provision_pool_size)).getOrElse(16)
        )
      case Success(false) =>
        logger.info("Initializing provisioner in sync provisioning mode")
        new SyncTfDependencies
      case Failure(error) => // Fallback to sync provisioning
        logger.warn(
          s"No configuration found for $async_provision_enabled, fall-backing to Synchronous provisioning",
          error
        )
        new SyncTfDependencies
    }
  }
}
