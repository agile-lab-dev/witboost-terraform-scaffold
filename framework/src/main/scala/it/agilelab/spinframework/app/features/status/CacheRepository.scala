package it.agilelab.spinframework.app.features.status

import cats.effect.IO
import it.agilelab.spinframework.app.features.provision.ComponentToken
import org.slf4j.{ Logger, LoggerFactory }

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CacheRepository extends TaskRepository {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val tasks: ConcurrentHashMap[ComponentToken, Task] = new ConcurrentHashMap()

  override def createTask(task: Task): IO[Task] =
    IO {
      tasks.putIfAbsent(task.id, task) match {
        case storedTask if storedTask != null => storedTask
        case null                             => task
      }
    }

  override def findTask(token: ComponentToken): IO[Option[Task]] = IO {
    val task = Option(tasks.get(token))
    logger.debug("Retrieved task {} with token '{}'", task, token)
    task
  }

  override def updateTask(task: Task): IO[Task] = IO {
    tasks.put(task.id, task)
    tasks.get(task.id)
  }

  override def deleteTask(token: ComponentToken): IO[Option[Task]] =
    IO(Option(tasks.remove(token)))

  override def forceFail(): IO[List[Task]] = IO {
    val changed: mutable.ListBuffer[Task] = ListBuffer()
    tasks.forEach { (key, value) =>
      if (!TaskStatus.hasTerminated(value.status)) {
        val newTask = value.copy(status = TaskStatus.FAILED)
        tasks.replace(key, newTask)
        changed.append(newTask)
      }
    }
    changed.toList
  }
}
