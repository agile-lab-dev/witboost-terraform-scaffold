package it.agilelab.provisioners.terraform

/** Simplifies the creation of logger instances.
  */
object TerraformLogger {

  /** Logs on std output.
    */
  val logOnConsole: TerraformLogger = line => println(line.apply())

  /** Avoid logging completely.
    */
  val noLog: TerraformLogger = _ => ()
}

/** Logs the input command and any output that comes from Terraform after the execution of the command.
  */
trait TerraformLogger {

  /** Accepts a function that provides the line to log and
    *
    * @param line a function that provides on line to log.
    */
  def println(line: () => String): Unit
}
