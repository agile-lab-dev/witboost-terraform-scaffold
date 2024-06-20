package it.agilelab.provisioners.terraform

import io.circe.Json

case class TerraformOutput(name: String, value: Json, typeOf: Json, sensitive: Boolean)
