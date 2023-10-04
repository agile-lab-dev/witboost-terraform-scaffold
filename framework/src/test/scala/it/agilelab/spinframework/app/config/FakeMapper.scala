package it.agilelab.spinframework.app.config

import it.agilelab.plugin.principalsmapping.api.Mapper

class FakeMapper extends Mapper {
  override def map(subjects: Set[String]): Map[String, Either[Throwable, String]] =
    subjects.map(s => (s -> Left(new Throwable("Some error")))).toMap
}
