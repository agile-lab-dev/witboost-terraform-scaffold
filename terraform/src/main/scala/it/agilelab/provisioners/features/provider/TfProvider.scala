package it.agilelab.provisioners.features.provider

import com.fasterxml.jackson.databind.node.ArrayNode
import com.jayway.jsonpath.Option.{ ALWAYS_RETURN_LIST, DEFAULT_PATH_LEAF_TO_NULL }
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.{ Configuration, JsonPath, JsonPathException }
import it.agilelab.provisioners.configuration.TfConfiguration._
import it.agilelab.provisioners.terraform.{ TerraformCommands, TerraformVariables }
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ErrorMessage }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

class TfProvider(terraform: TerraformCommands) extends CloudProvider {

  private lazy val terraformInitResult = terraform.doInit()
  private lazy val conf: Configuration = Configuration
    .builder()
    .jsonProvider(new JacksonJsonNodeJsonProvider())
    .options(ALWAYS_RETURN_LIST, DEFAULT_PATH_LEAF_TO_NULL)
    .build()

  override def provision(descriptor: ComponentDescriptor): ProvisionResult = {
    if (!terraformInitResult.isSuccess)
      return ProvisionResult.failure(Seq(ErrorMessage(terraformInitResult.buildOutputString)))

    variablesFrom(descriptor) match {
      case Left(l)     => ProvisionResult.failure(l)
      case Right(vars) =>
        val applyResult = terraform.doApply(vars)
        if (applyResult.isSuccess)
          ProvisionResult.completed()
        else
          ProvisionResult.failure(Seq(ErrorMessage(applyResult.buildOutputString)))

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
          ProvisionResult.failure(Seq(ErrorMessage(result.buildOutputString)))
    }

  def variablesFrom(
    descriptor: ComponentDescriptor,
    variableMappings: Option[Map[String, String]] = None
  ): Either[Seq[ErrorMessage], TerraformVariables] = {

    // read mappings from configs
    // e.g. resource_group_name -> component.specific.resource_group_name
    val mappings: Map[String, String] = variableMappings match {
      case None    =>
        provisionerConfig
          .getConfig(descriptor_mapping)
          .entrySet()
          .asScala
          .map(e => e.getKey -> e.getValue.unwrapped.toString)
          .toMap
      case Some(x) => x
    }

    // for each key, take the corresponding value
    // e.g. resource_group_name -> sample_name
    val (lefts, right) = mappings
      .map(mapping =>
        getValue(descriptor.toString, mapping._2) match {
          case Right(r) => Right(mapping._1 -> r)
          case Left(l)  => Left(l)
        }
      )
      .partitionMap(identity)

    if (lefts.isEmpty)
      Right(new TerraformVariables(right.toMap))
    else
      Left(lefts.map(err => ErrorMessage(err)).toSeq)

  }

  private def getValue(jsonString: String, jsonPath: String): Either[String, String] = {
    val docContext = JsonPath.using(conf).parse(jsonString)

    val componentId      = docContext.read[ArrayNode]("$.componentIdToProvision")
    val jsonPathReplaced = jsonPath.replace("{{componentIdToProvision}}", componentId.get(0).asText())

    val node = Try(docContext.read[ArrayNode](jsonPathReplaced))
    node match {
      case Success(node)                  =>
        if (node.size() == 0 || node.get(0).isNull) {
          Left(
            s"Terraform variables could not be extracted from the descriptor. No results for path: $jsonPathReplaced"
          )
        } else {
          Right(node.get(0).asText())
        }
      case Failure(ex: JsonPathException) =>
        Left(
          s"Terraform variables could not be extracted from the descriptor. The supplied Json Path expression ($jsonPathReplaced) is not valid: ${ex.getMessage}"
        )
      case Failure(ex)                    =>
        Left(
          s"Terraform variables could not be extracted from the descriptor. Failed to extract value from $jsonPathReplaced: ${ex.getMessage}"
        )

    }
  }
}
