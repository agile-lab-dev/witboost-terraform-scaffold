package it.agilelab.provisioners.terraform

case class TerraformModule(
  path: String,
  mappings: Map[String, String],
  backendConfigs: Map[String, String],
  stateKey: String
)
