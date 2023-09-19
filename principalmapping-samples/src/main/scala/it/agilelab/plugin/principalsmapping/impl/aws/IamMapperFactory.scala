package it.agilelab.plugin.principalsmapping.impl.aws

import com.typesafe.config.Config
import it.agilelab.plugin.principalsmapping.api.{Mapper, MapperFactory}

import scala.util.{Success, Try}

class IamMapperFactory extends MapperFactory {

  override def create(config: Config): Try[Mapper] = {
      Client.getClient(config).flatMap(s => Success(new IamMapper(s)))
  }
  override def configIdentifier: String = "awsIam"

}
