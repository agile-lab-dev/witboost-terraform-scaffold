package it.agilelab.spinframework.app.config

import com.typesafe.config.Config
import it.agilelab.plugin.principalsmapping.api.{ Mapper, MapperFactory }

import scala.util.{ Success, Try }

class FakeMapperFactory extends MapperFactory {

  override def create(config: Config): Try[Mapper] = Success(new FakeMapper)
  override def configIdentifier: String            = "fake"

}
