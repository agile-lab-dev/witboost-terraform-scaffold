package it.agilelab.spinframework.app.utils

import it.agilelab.spinframework.app.api.generated.definitions.Log

import java.time.{ OffsetDateTime, ZoneOffset }

object LogUtils {
  def addLog(log: String, level: Log.Level): Log = Log(OffsetDateTime.now(ZoneOffset.UTC), level, log)
}
