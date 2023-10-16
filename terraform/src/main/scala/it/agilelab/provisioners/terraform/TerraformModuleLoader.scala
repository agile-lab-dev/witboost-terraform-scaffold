package it.agilelab.provisioners.terraform

import com.typesafe.config.Config
import it.agilelab.provisioners.configuration.TfConfiguration._

import java.nio.file.{ Files, Paths }
import scala.jdk.CollectionConverters._
object TerraformModuleLoader {

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
      if (!Files.exists(Paths.get(path)))
        Left(s"The configured path $path is not existent for the module $moduleId")
      else {
        val mappings = moduleConf
          .getConfig("descriptorToVariablesMapping")
          .entrySet()
          .asScala
          .map(e => e.getKey -> e.getValue.unwrapped.toString)
          .toMap
        Right(TerraformModule(path, mappings))
      }
    } else {
      Left(s"Missing configuration for the module $moduleId")
    }
}
