package it.agilelab.spinframework.app.config

import com.typesafe.config.Config
import it.agilelab.plugin.principalsmapping.api.{ Mapper, MapperFactory }
import it.agilelab.spinframework.app.config.Configuration._

import java.util.ServiceLoader
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

class PrincipalMapperPluginLoader() {

  /** Tries to loads the Principal Mapper Plugin
    * @return the requested mapper
    */
  def load(cfg: Config): Try[Mapper] =
    for {
      pluginClass    <- extractPluginClass(cfg)
      mf             <- mapperFactory(pluginClass)
      specificConfig <- getSpecificConfig(mf, cfg)
      mapper         <- mf.create(specificConfig)
    } yield mapper

  private def mapperFactory(pluginClass: String): Try[MapperFactory] =
    Try {
      ServiceLoader
        .load(classOf[MapperFactory])
        .asScala
        .filter(_.getClass.eq(Class.forName(pluginClass)))
    } match {
      case Success(mappers)   =>
        if (mappers.isEmpty) {
          Failure(new Throwable(s"Couldn't load plugin with class $pluginClass"))
        } else {
          Success(mappers.head)
        }
      case Failure(exception) =>
        Failure(exception)
    }

  private def getSpecificConfig(mf: MapperFactory, cfg: Config): Try[Config] =
    Try {
      cfg.getConfig(principalMappingPlugin).getConfig(mf.configIdentifier)
    }

  private def extractPluginClass(cfg: Config): Try[String] =
    Try {
      cfg.getString(principalMappingPluginClass)
    }
}
