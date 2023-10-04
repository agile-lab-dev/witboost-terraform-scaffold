package it.agilelab.provisioners.terraform

import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.{ Configuration, JsonKey }
import io.circe.{ parser, Decoder, Encoder }

/** The result coming from the execution of a Terraform command.
  *
  * @param processResult the exitCode-output pair resulting from
  *                      the execution of a local process
  */
class TerraformResult(processResult: ProcessResult) {

  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  case class Snippet(
    context: String,
    code: String,
    start_line: Int,
    highlight_start_offset: Int,
    highlight_end_offset: Int,
    values: List[String]
  )

  implicit val snippetDecoder: Decoder[Snippet] = deriveConfiguredDecoder[Snippet]
  implicit val snippetEncoder: Encoder[Snippet] = deriveConfiguredEncoder[Snippet]

  case class Position(line: Int, column: Int, byte: Int)

  implicit val positionDecoder: Decoder[Position] = deriveConfiguredDecoder[Position]
  implicit val positionEncoder: Encoder[Position] = deriveConfiguredEncoder[Position]

  case class Range(filename: String, start: Position, end: Position)

  implicit val rangeDecoder: Decoder[Range] = deriveConfiguredDecoder[Range]
  implicit val rangeEncoder: Encoder[Range] = deriveConfiguredEncoder[Range]

  case class Diagnostic(severity: String, summary: String, detail: String, range: Range, snippet: Snippet)

  implicit val diagnosticDecoder: Decoder[Diagnostic] = deriveConfiguredDecoder[Diagnostic]
  implicit val diagnosticEncoder: Encoder[Diagnostic] = deriveConfiguredEncoder[Diagnostic]

  @JsonCodec
  case class ErrorResponse(
    @JsonKey("@level") level: String,
    @JsonKey("@message") message: String,
    @JsonKey("@module") module: String,
    @JsonKey("@timestamp") timestamp: String,
    @JsonKey("diagnostic") diagnostic: Diagnostic,
    @JsonKey("type") typeOf: String
  )

  implicit val errorResponseDecoder: Decoder[ErrorResponse] = deriveConfiguredDecoder[ErrorResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveConfiguredEncoder[ErrorResponse]

  @JsonCodec
  case class Output(sensitive: Boolean, `type`: String, value: String)

  @JsonCodec
  case class OutputMessage(
    @JsonKey("@level") `@level`: String,
    @JsonKey("@message") `@message`: String,
    @JsonKey("@module") `@module`: String,
    @JsonKey("@timestamp") `@timestamp`: String,
    @JsonKey("outputs") outputs: Map[String, Output],
    @JsonKey("type") `type`: String
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
                      typeOf = k._2.`type`,
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
              val positionString = value.diagnostic.snippet.start_line
              val appendString   = s"$messageString. $detailString $snippetString at line: $positionString"
              List(appendString)
            case Left(_)      => None
          }
        }
      if (result.isEmpty) List("Details about the errors are not available. Contact the Platform team for assistance!")
      else result
    } else List.empty[String]

}
