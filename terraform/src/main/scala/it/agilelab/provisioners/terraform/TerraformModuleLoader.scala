package it.agilelab.provisioners.terraform

import com.typesafe.config.Config
import it.agilelab.provisioners.configuration.TfConfiguration._
import org.slf4j.{ Logger, LoggerFactory }

import java.nio.file.{ Files, Paths }
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }
object TerraformModuleLoader {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val bcConfigs  = "backendConfigs.configs"
  private val bcStateKey = "backendConfigs.stateKey"

  /** *
    * Loads the configuration for a specific moduleId
    * @param moduleId the identifier of the module to load
    * @param config the configuration
    * @return either a String describing the error occurred or a [[TerraformModule]] instance
    */
  def from(moduleId: String, config: Config = provisionerConfig): Either[String, TerraformModule] =
    if (config.getConfig("terraform").hasPath(s""""$moduleId"""")) {
      val moduleConf = config.getConfig("terraform").getConfig(s""""$moduleId"""")
      val path       = moduleConf.getString("repositoryPath")

      if (!Files.exists(Paths.get(path))) {
        Left(s"The configured path $path is not existent for the module $moduleId")
      } else if (!moduleConf.hasPath(bcConfigs)) {
        Left(s"The configured path $bcConfigs is not existent for the module $moduleId")
      } else if (!moduleConf.hasPath(bcStateKey)) {
        Left(s"The configured path $bcStateKey is not existent for the module $moduleId")
      } else if (!hasStateKeyValue(moduleConf)) {
        Left(s"The configured state key $bcStateKey doesn't match any item in `$bcConfigs`")
      } else {
        // mappings can be empty
        val mappings = moduleConf
          .getConfig("descriptorToVariablesMapping")
          .entrySet()
          .asScala
          .map(e => e.getKey -> e.getValue.unwrapped.toString)
          .toMap

        Try {
          val backendConfigs =
            moduleConf
              .getConfig(bcConfigs)
              .entrySet()
              .asScala
              .map(e => e.getKey -> e.getValue.unwrapped.toString)
              .toMap
          // Extract the stateKey
          val keyName        = moduleConf.getString(bcStateKey)

          (backendConfigs, keyName)

        }.toEither match {
          case Right(r) => Right(TerraformModule(path, mappings, r._1, r._2))
          case Left(f)  =>
            logger.error("It was not possible to create terraform backend configs.", f)
            Left(f.getMessage)
        }

      }
    } else {
      Left(s"Missing configuration for the module $moduleId")
    }

  private def hasStateKeyValue(conf: Config): Boolean =
    Try {
      val stateKey = conf.getString(bcStateKey)
      conf.hasPath(s"$bcConfigs.$stateKey")
    } match {
      case Success(b) => b
      case Failure(_) => false
    }

}
