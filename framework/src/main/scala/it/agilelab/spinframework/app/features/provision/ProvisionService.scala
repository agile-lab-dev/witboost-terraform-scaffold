package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.Config
import it.agilelab.plugin.principalsmapping.api.Mapper
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.config.PrincipalMapperPluginLoader
import it.agilelab.spinframework.app.features.compiler._
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success }

class ProvisionService(
  compile: Compile,
  cloudProvider: CloudProvider,
  principalMapperPluginLoader: PrincipalMapperPluginLoader
) extends Provision {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = {
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)
    cloudProvider.provision(result.descriptor)
  }

  override def doUnprovisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = {
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)
    cloudProvider.unprovision(result.descriptor)
  }

  override def doUpdateAcl(
    jsonDescriptor: JsonDescriptor,
    refs: Set[String],
    cfg: Config = provisionerConfig
  ): ProvisionResult = {

    val result: CompileResult = compile.doCompile(jsonDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    principalMapperPluginLoader.load(cfg) match {
      case Success(m) =>
        m.map(refs).partition(_._2.isLeft) match {
          // Fail if there's one failed mapping
          case (l, _) if l.nonEmpty =>
            ProvisionResult.failure(
              l.map(e =>
                ErrorMessage(s"An error occurred while mapping the subject `${e._1}`. Detailed error: ${e._2.left}")
              ).toSeq
            )
          case (_, r)               =>
            val principals = r.map(_._2.getOrElse(null)).toSet
            cloudProvider.updateAcl(result.descriptor, principals)
        }
      case Failure(f) =>
        logger.error("Error in doUpdateAcl", f)
        ProvisionResult.failure(
          Seq(
            ErrorMessage(
              s"An unexpected error occurred while instantiating the Principal Mapper Plugin. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
            )
          )
        )
    }

  }
}
