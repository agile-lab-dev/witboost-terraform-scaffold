package it.agilelab.spinframework.app.features.compiler.circe

import io.circe.Json

trait CirceParsedCatalogInfo
object CirceParsedCatalogInfo {
  def apply(json: Json) = new CirceParsedDescriptor(json.hcursor, Seq.empty) with CirceParsedCatalogInfo
}
