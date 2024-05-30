package it.agilelab.provisioners.terraform

import io.circe.generic.extras._
import io.circe.parser
import org.slf4j.{ Logger, LoggerFactory }

/** The result coming from the execution of a Terraform command.
  *
  * @param processResult the exitCode-output pair resulting from
  *                      the execution of a local process
  */
class TerraformResult(processResult: ProcessResult) {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  /** Builds a single string that encompass all the output returned by Terraform
    *  on the execution of the command.
    *  N.B.: this is a potentially risky operation: in case the output is too large to be concatenated,
    *  the operation could fail.
    *
    * @return build a unique string that encompasses the output.
    */
  def buildOutputString: String = processResult.buildOutputString

  /** Builds a list of [[TerraformOutput]] which contains the outputs of the terraform provisioning.
    * This is applicable in case of the following terraform operations - Apply.
    *
    * @return either an extraction/parsing error, or a list of [[TerraformOutput]]
    */
  def terraformOutputs: Either[io.circe.Error, List[TerraformOutput]] =
    if (isSuccess) {
      val outputLines = processResult.buildOutputString
        .split("\n")
        .filter(_.contains("outputs"))
        .toList

      outputLines
        .map(l =>
          parser
            .parse(l)
            .flatMap(json => json.as[OutputMessage])
            .flatMap(om =>
              Right(
                om.outputs
                  .map(k =>
                    TerraformOutput(
                      name = k._1,
                      value = k._2.value,
                      typeOf = k._2.typeOf,
                      sensitive = k._2.sensitive
                    )
                  )
                  .toList
              )
            )
        )
        .partition(_.isLeft) match {
        case (_, r) if r.nonEmpty => r.last
        case (l, _) if l.nonEmpty => l.last
        case (Nil, Nil)           => Right(List.empty[TerraformOutput])
      }

    } else {
      Right(List.empty[TerraformOutput])
    }

  /** Builds a list of strings which comprises of error messages.
    * This is applicable in case of the following terraform operations - Apply, Plan and Destroy.
    * Terraform init doesn't have any such kind and terraform validate would be written at a latter time.
    *
    * @return a list which stores error messages after the above mentioned operations are performed and failed.
    */
  def errorMessages: List[String] =
    if (!isSuccess) {
      val result = processResult.buildOutputString
        .split("\n")
        .toList
        .filter(s => s.contains("diagnostic"))
        .flatMap { diagnosticString =>
          parser.parse(diagnosticString).flatMap(_.as[ErrorResponse]) match {
            case Right(errorResponse) =>
              List(errorResponse.toString)
            case Left(error)          =>
              logger.error("Unable to return the parsed ErrorResponse.", error)
              List(diagnosticString)
          }
        }
      if (result.isEmpty)
        List("Details about the errors are not available. Contact the Platform team for assistance!")
      else
        result
    } else List.empty[String]

  /** Builds a list of strings which comprises of validation messages.
    * This is applicable in case of the following terraform operations - Validate.
    *
    * @return a list which stores validation messages after the above mentioned operations are performed and failed.
    */
  def validationErrors: List[String] =
    if (!isSuccess) {
      val processResultOutputString = processResult.buildOutputString

      parser.parse(processResultOutputString).flatMap(_.as[ValidationResponse]) match {
        case Right(validationResponse) =>
          validationResponse.diagnostics.map { diagnostic =>
            diagnostic.toString
          }
        case Left(error)               =>
          logger.error("Unable to return the parsed ValidationResponse.", error)
          List(processResultOutputString)
      }
    } else List.empty[String]

  /** Allows to establish if the execution has been successful or not.
    *
    * @return true if the execution of the command has raised no error.
    */
  def isSuccess: Boolean = processResult.exitCode == 0

  @ConfiguredJsonCodec
  case class Snippet(
    context: Option[String],
    code: String,
    startLine: Int,
    highlightStartOffset: Int,
    highlightEndOffset: Int,
    values: List[SnippetValue]
  ) {
    override def toString: String =
      s"""Context: [${context.getOrElse("")}]
         |Code: [$code]
         |""".stripMargin
  }

  @ConfiguredJsonCodec
  case class SnippetValue(
    traversal: String,
    statement: String
  )

  @ConfiguredJsonCodec
  case class Position(line: Int, column: Int, byte: Int)

  @ConfiguredJsonCodec
  case class Range(filename: String, start: Position, end: Position)

  @ConfiguredJsonCodec
  case class Diagnostic(
    severity: String,
    summary: String,
    detail: String,
    range: Option[Range],
    snippet: Option[Snippet]
  ) {
    override def toString: String =
      s"""Summary: [$summary]
         |Detail: [$detail]
         |${snippet.getOrElse("The snippet is not available.")}
         |""".stripMargin
  }

  @ConfiguredJsonCodec
  case class ErrorResponse(
    @JsonKey("@level") level: String,
    @JsonKey("@message") message: String,
    @JsonKey("@module") module: String,
    @JsonKey("@timestamp") timestamp: String,
    @JsonKey("diagnostic") diagnostic: Diagnostic,
    @JsonKey("type") typeOf: String
  ) {
    override def toString: String =
      s"""Message: [$message]
         |$diagnostic
         |""".stripMargin
  }

  @ConfiguredJsonCodec
  case class ValidationResponse(
    @JsonKey("format_version") formatVersion: String,
    @JsonKey("valid") valid: Boolean,
    @JsonKey("error_count") errorCount: Int,
    @JsonKey("warning_count") warningCount: Int,
    @JsonKey("diagnostics") diagnostics: List[Diagnostic]
  )

  @ConfiguredJsonCodec
  case class Output(sensitive: Boolean, @JsonKey("type") typeOf: String, value: String)

  @ConfiguredJsonCodec
  case class OutputMessage(
    @JsonKey("@level") level: String,
    @JsonKey("@message") message: String,
    @JsonKey("@module") module: String,
    @JsonKey("@timestamp") timestamp: String,
    @JsonKey("outputs") outputs: Map[String, Output],
    @JsonKey("type") typeOf: String
  )

}
