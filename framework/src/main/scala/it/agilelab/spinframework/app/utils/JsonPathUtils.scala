package it.agilelab.spinframework.app.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.jayway.jsonpath.Option.{ ALWAYS_RETURN_LIST, DEFAULT_PATH_LEAF_TO_NULL }
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.{ Configuration, JsonPath, JsonPathException }
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

object JsonPathUtils {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private lazy val conf: Configuration = Configuration
    .builder()
    .jsonProvider(new JacksonJsonNodeJsonProvider())
    .options(ALWAYS_RETURN_LIST, DEFAULT_PATH_LEAF_TO_NULL)
    .build()

  /** Understands, from the descriptor, the descriptor kind.
    * @param jsonString the json object in which to search for, serialized as String
    * @return whether this is a data product or component deployment
    */
  def isDataProductProvisioning(jsonString: String): Boolean = {
    val docContext = JsonPath.using(conf).parse(jsonString)
    Try(docContext.read[ArrayNode]("$.componentIdToProvision")) match {
      case Success(node)                  =>
        if (node.size() == 0 || node.get(0).isNull) {
          true
        } else false
      case Failure(ex: JsonPathException) =>
        logger.error("It was not possible to determine if provisioning a component or a dp", ex)
        false
    }
  }

  /** *
    * Parse the json object and extracts the path specified
    * @param jsonString the json object in which to search for, serialized as String
    * @param jsonPath the JsonPath expression representing the path to read
    * @return either a String describing the error occurred or the path that was read
    */
  def getValue(jsonString: String, jsonPath: String): Either[String, String] = {
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
          val x = node.get(0)
          Right(
            if (x.isContainerNode) x.toPrettyString
            else x.asText
          )
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
