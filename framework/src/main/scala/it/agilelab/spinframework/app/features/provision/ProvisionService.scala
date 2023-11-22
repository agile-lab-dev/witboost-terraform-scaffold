package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.Config
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.config.{ PrincipalMapperPluginLoader, SpecificProvisionerDependencies }
import it.agilelab.spinframework.app.features.compiler._
import it.agilelab.spinframework.app.utils.JsonPathUtils
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success }

class ProvisionService(
  compile: Compile,
  specific: SpecificProvisionerDependencies,
  principalMapperPluginLoader: PrincipalMapperPluginLoader
) extends Provision {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val useCaseTemplateIdJsonPath =
    "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].useCaseTemplateId"

  override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = {
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    val res = for {
      useCaseTemplateId <- JsonPathUtils.getValue(result.descriptor.toString, useCaseTemplateIdJsonPath)
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
    } yield cloudProvider
    res match {
      case Right(cloudProvider) => cloudProvider.provision(result.descriptor)
      case Left(message)        => ProvisionResult.failure(Seq(ErrorMessage(message)))
    }
  }

  override def doUnprovisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = {
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    val res = for {
      useCaseTemplateId <- JsonPathUtils.getValue(result.descriptor.toString, useCaseTemplateIdJsonPath)
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
    } yield cloudProvider
    res match {
      case Right(cloudProvider) => cloudProvider.unprovision(result.descriptor)
      case Left(message)        => ProvisionResult.failure(Seq(ErrorMessage(message)))
    }
  }

  override def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult = {

    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)

    val res = for {
      useCaseTemplateId <- JsonPathUtils.getValue(result.descriptor.toString, useCaseTemplateIdJsonPath)
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
    } yield cloudProvider
    res match {
      case Right(cloudProvider) => cloudProvider.validate(result.descriptor)
      case Left(message)        => ProvisionResult.failure(Seq(ErrorMessage(message)))
    }
  }

  override def doUpdateAcl(
    provisionInfo: ProvisionInfo,
    refs: Set[String],
    cfg: Config = provisionerConfig
  ): ProvisionResult = {

    val jsonDescriptor            = JsonDescriptor(provisionInfo.result)
    val jsonResult: CompileResult = compile.doCompile(jsonDescriptor)
    if (!jsonResult.isSuccess) return ProvisionResult.failure(jsonResult.errors)

    val yamlDescriptor            = YamlDescriptor(provisionInfo.request)
    val yamlResult: CompileResult = compile.doCompile(yamlDescriptor)
    if (!yamlResult.isSuccess) return ProvisionResult.failure(yamlResult.errors)

    val res = for {
      useCaseTemplateId <- JsonPathUtils.getValue(yamlResult.descriptor.toString, useCaseTemplateIdJsonPath)
      cloudProvider     <- specific.cloudProvider(useCaseTemplateId)
    } yield (cloudProvider, useCaseTemplateId)

    res match {
      case Left(message)                             => ProvisionResult.failure(Seq(ErrorMessage(message)))
      case Right((cloudProvider, useCaseTemplateId)) =>
        val moduleConfig = cfg.getConfig(s"""terraform."$useCaseTemplateId"""")
        principalMapperPluginLoader.load(moduleConfig) match {
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
                cloudProvider.updateAcl(jsonResult.descriptor, yamlResult.descriptor, principals)
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
}
