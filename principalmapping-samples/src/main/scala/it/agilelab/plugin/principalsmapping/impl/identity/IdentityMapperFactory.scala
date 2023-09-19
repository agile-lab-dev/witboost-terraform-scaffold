package it.agilelab.plugin.principalsmapping.impl.identity

import com.typesafe.config.{Config, ConfigFactory}
import it.agilelab.plugin.principalsmapping.api.{Mapper, MapperFactory}

import scala.util.{Success, Try}

class IdentityMapperFactory extends MapperFactory{

  override def create(config: Config = ConfigFactory.empty()): Try[Mapper] = {
    Success(new IdentityMapper)
  }
  override def configIdentifier: String = "identity"

}
