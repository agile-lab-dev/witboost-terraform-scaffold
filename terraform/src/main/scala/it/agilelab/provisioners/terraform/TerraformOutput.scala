package it.agilelab.provisioners.terraform

case class TerraformOutput(name: String, value: String, typeOf: String, sensitive: Boolean)
