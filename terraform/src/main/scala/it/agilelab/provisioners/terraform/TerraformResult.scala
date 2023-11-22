package it.agilelab.provisioners.terraform

import com.fasterxml.jackson.annotation
import io.circe.generic.extras._
import io.circe.{ parser, Decoder, Encoder }

/** The result coming from the execution of a Terraform command.
  *
  * @param processResult the exitCode-output pair resulting from
  *                      the execution of a local process
  */
class TerraformResult(processResult: ProcessResult) {

  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  @ConfiguredJsonCodec
  case class Snippet(
    context: Option[String],
    code: String,
    startLine: Int,
    highlightStartOffset: Int,
    highlightEndOffset: Int,
    values: List[String]
  )
  @ConfiguredJsonCodec
  case class Position(line: Int, column: Int, byte: Int)
  @ConfiguredJsonCodec
  case class Range(filename: String, start: Position, end: Position)
  @ConfiguredJsonCodec
  case class Diagnostic(severity: String, summary: String, detail: String, range: Range, snippet: Snippet)

  @ConfiguredJsonCodec
  case class ErrorResponse(
    @JsonKey("@level") level: String,
    @JsonKey("@message") message: String,
    @JsonKey("@module") module: String,
    @JsonKey("@timestamp") timestamp: String,
    @JsonKey("diagnostic") diagnostic: Diagnostic,
    @JsonKey("type") typeOf: String
  )

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

  /** Allows to establish if the execution has been successful or not.
    *
    * @return true if the execution of the command has raised no error.
    */
  def isSuccess: Boolean = processResult.exitCode == 0

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
        .filter(x => x.contains("diagnostic"))
        .flatMap { x =>
          parser.parse(x).flatMap(_.as[ErrorResponse]) match {
            case Right(value) =>
              val messageString  = value.message
              val detailString   = value.diagnostic.detail
              val snippetString  = value.diagnostic.snippet.code
              val positionString = value.diagnostic.snippet.startLine
              val fileName       = value.diagnostic.range.filename
              val appendString   = s"$messageString. $detailString $snippetString at line: $positionString of $fileName"
              List(appendString)
            case Left(_)      => None
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
      parser.parse(processResult.buildOutputString).flatMap(_.as[ValidationResponse]) match {
        case Right(value) =>
          value.diagnostics.map { x =>
            val filename = x.range.filename
            val line     = x.range.start.line
            val summary  = x.summary
            val code     = x.snippet.code
            val context  = x.snippet.context.getOrElse("")
            val output   = s"$summary. Context [$context]. Code [$code] at line $line of $filename"
            output
          }
        case Left(_)      => List("Details about the errors are not available. Contact the Platform team for assistance!")
      }
    } else List.empty[String]

}
