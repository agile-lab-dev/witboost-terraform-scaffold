package it.agilelab.provisioners.terraform

trait Processor {
  def run(command: String): ProcessResult
}
