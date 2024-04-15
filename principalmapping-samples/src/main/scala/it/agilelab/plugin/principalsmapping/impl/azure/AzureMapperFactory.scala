package it.agilelab.plugin.principalsmapping.impl.azure

import com.typesafe.config.Config
import it.agilelab.plugin.principalsmapping.api.{Mapper, MapperFactory}

import scala.util.{Success, Try}

class AzureMapperFactory extends MapperFactory {
  override def create(config: Config): Try[Mapper] = AzureClient.getClient(config).flatMap(azureClient => Success(new AzureMapper(azureClient)))
  override def configIdentifier: String = "azure"
}
