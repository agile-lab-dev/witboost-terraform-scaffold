package it.agilelab.provisioners.features.provider

import it.agilelab.provisioners.features.descriptor.TerraformOutputsDescriptor
import it.agilelab.provisioners.terraform.{ TerraformBuilder, TerraformCommands, TerraformModule, TerraformVariables }
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ErrorMessage, TerraformOutput }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }
import it.agilelab.spinframework.app.utils.JsonPathUtils
import org.slf4j.{ Logger, LoggerFactory }
import java.nio.file.Path

class TfProvider(terraformBuilder: TerraformBuilder, terraformModule: TerraformModule) extends CloudProvider {

  private val terraform = terraformBuilder
    .onDirectory(terraformModule.path)

  private val terraformAcl = terraformBuilder
    .onDirectory(Path.of(terraformModule.path, "acl").toString)

  private lazy val terraformInitResult    = terraform.doInit()
  private lazy val terraformAclInitResult = terraformAcl.doInit()

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def provision(descriptor: ComponentDescriptor): ProvisionResult = {
    if (!terraformInitResult.isSuccess)
      return ProvisionResult.failure(terraformInitResult.errorMessages.map(ErrorMessage))

    variablesFrom(descriptor) match {
      case Left(l)     => ProvisionResult.failure(l)
      case Right(vars) =>
        val applyResult = terraform.doApply(vars)
        if (applyResult.isSuccess) {
          ProvisionResult.completed(
            applyResult.terraformOutputs match {
              case Right(r) => r.filter(!_.sensitive).map(o => TerraformOutput(name = o.name, o.value))
              // If the parsing of terraform output fails, an empty seq is returned.
              // The provisioning is considered successful, but no output will be returned back to the coordinator
              case Left(f)  =>
                logger.error("An error occurred while trying to extract outputs from terraform execution", f)
                Seq.empty
            }
          )
        } else
          ProvisionResult.failure(applyResult.errorMessages.map(ErrorMessage))
    }
  }

  override def unprovision(descriptor: ComponentDescriptor): ProvisionResult =
    variablesFrom(descriptor) match {
      case Left(l)     => ProvisionResult.failure(l)
      case Right(vars) =>
        val result = terraform.doDestroy(vars)
        if (result.isSuccess)
          ProvisionResult.completed()
        else
          ProvisionResult.failure(result.errorMessages.map(ErrorMessage))
    }

  override def updateAcl(descriptor: ComponentDescriptor, refs: Set[String]): ProvisionResult = {
    if (!terraformAclInitResult.isSuccess)
      return ProvisionResult.failure(terraformAclInitResult.errorMessages.map(ErrorMessage))

    TerraformOutputsDescriptor(descriptor).mapOutputs match {
      case Right(m) =>
        val v           = m + ("principals" -> refs.mkString(","))
        val vars        = new TerraformVariables(v)
        val applyResult = terraformAcl.doApply(vars)
        if (applyResult.isSuccess)
          ProvisionResult.completed()
        else
          ProvisionResult.failure(applyResult.errorMessages.map(ErrorMessage))
      case Left(l)  => ProvisionResult.failure(Seq(ErrorMessage(l.getMessage)))
    }
  }

  def variablesFrom(
    descriptor: ComponentDescriptor,
    variableMappings: Option[Map[String, String]] = None
  ): Either[Seq[ErrorMessage], TerraformVariables] = {

    // read mappings from configs
    // e.g. resource_group_name -> component.specific.resource_group_name
    val mappings: Map[String, String] = variableMappings match {
      case None    =>
        terraformModule.mappings
      case Some(x) => x
    }

    // for each key, take the corresponding value
    // e.g. resource_group_name -> sample_name
    val (lefts, right) = mappings
      .map(mapping =>
        JsonPathUtils.getValue(descriptor.toString, mapping._2) match {
          case Right(r) => Right(mapping._1 -> r)
          case Left(l)  => Left(l)
        }
      )
      .partitionMap(identity)

    if (lefts.isEmpty)
      Right(new TerraformVariables(right.toMap))
    else {
      Left(lefts.map(err => ErrorMessage(err)).toSeq)
    }
  }

  def terraformCommands: TerraformCommands = terraform

}
