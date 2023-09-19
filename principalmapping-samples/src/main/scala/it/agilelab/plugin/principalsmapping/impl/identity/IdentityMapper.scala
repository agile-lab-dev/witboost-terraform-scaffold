package it.agilelab.plugin.principalsmapping.impl.identity

import it.agilelab.plugin.principalsmapping.api.Mapper

class IdentityMapper extends Mapper {
  override def map(subjects: Set[String]):  Map[String, Either[Throwable, String]] = subjects.map( m => ( m -> Right(m) )).toMap
}