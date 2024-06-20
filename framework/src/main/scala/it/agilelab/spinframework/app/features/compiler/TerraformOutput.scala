package it.agilelab.spinframework.app.features.compiler

import io.circe.Json

case class TerraformOutput(name: String, value: Json)
