package it.agilelab.spinframework.app.features.status

import cats.effect.IO
import com.typesafe.config.Config
import it.agilelab.spinframework.app.config.Configuration.async_config_type
import it.agilelab.spinframework.app.features.provision.ComponentToken
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Success, Try }

trait TaskRepository {

  /** Saves a task to be retrieved at a later moment in time */
  def createTask(task: Task): IO[Task]

  /** Queries a task using token as the identifier */
  def findTask(token: ComponentToken): IO[Option[Task]]

  /** Updates an existing task identified by the taskId with the one received */
  def updateTask(task: Task): IO[Task]

  /** Deletes a task using the taskId as the identifier */
  def deleteTask(token: ComponentToken): IO[Option[Task]]

  /** Forces FAILED status on all tasks created by a specific provisioner instance.
    *  This is done to avoid leaving hanging tasks when one instance crashes or goes down.
    *
    *  Currently forceFail fails all task, as provisionerId is not yet implemented
    */
  def forceFail(): IO[List[Task]]
}

object TaskRepository {
  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  val CACHE_REPOSITORY_TYPE = "cache"

  def fromConfig(repositoryConfig: Config): TaskRepository =
    Try(repositoryConfig.getString(async_config_type)) match {
      case Success(CACHE_REPOSITORY_TYPE) => new CacheRepository
      case _                              =>
        logger.warn(s"No configuration found for $async_config_type, fall-backing to Cache Repository")
        new CacheRepository // TODO currently supporting only CacheRepository
    }
}
