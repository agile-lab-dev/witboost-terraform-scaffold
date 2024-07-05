package it.agilelab.spinframework.app.features.provision

import cats.effect.{ FiberIO, IO }
import cats.implicits.toTraverseOps
import com.typesafe.config.Config
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.features.compiler.{ ErrorMessage, YamlDescriptor }
import it.agilelab.spinframework.app.features.status.TaskOperation.TaskOperation
import it.agilelab.spinframework.app.features.status._
import org.slf4j.{ Logger, LoggerFactory }

import java.util.UUID
import scala.concurrent.ExecutionContext
class AsyncProvisionerService(
  provision: Provision,
  taskRepository: TaskRepository,
  executionContext: ExecutionContext
) extends AsyncProvision {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private def buildTask(taskOperation: TaskOperation): Task =
    Task(ComponentToken(UUID.randomUUID().toString), TaskStatus.WAITING, taskOperation, Info())

  override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config): IO[ProvisionResult] = {
    val task = buildTask(TaskOperation.PROVISION)
    handleTask(task, provision.doProvisioning(yamlDescriptor, cfg)) *>
      IO.pure(ProvisionResult.running(task.id))
  }

  override def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config): IO[ProvisionResult] = {
    val task = buildTask(TaskOperation.UNPROVISION)
    handleTask(task, provision.doUnprovisioning(yaml, removeData, cfg)) *>
      IO.pure(ProvisionResult.running(task.id))
  }

  override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): IO[ProvisionResult] =
    IO.blocking {
      provision.doUpdateAcl(provisionInfo, refs, cfg)
    }

  override def doValidate(yamlDescriptor: YamlDescriptor): IO[ProvisionResult] = IO.blocking {
    // Validate can be also async, but for now we only care about async provisioning
    provision.doValidate(yamlDescriptor)
  }

  private def handleTask(task: Task, blockingFunction: => ProvisionResult): IO[FiberIO[Option[Task]]] = {
    val action: IO[Option[Task]] = for {
      _                  <- taskRepository.createTask(task)
      _                  <- {
        logger.info(s"Starting execution of asynchronous task '{}'", task.id)
        taskRepository.updateTask(task.copy(status = TaskStatus.RUNNING))
      }
      result             <- IO.blocking {
                              val result = blockingFunction
                              logger.info(s"Task '{}' ended with result {}", task.id, result)
                              result
                            }
      // We need to query the repository because the task could've been marked as failed in the meantime
      maybeRetrievedTask <- taskRepository.findTask(task.id)
      updatedTask        <- maybeRetrievedTask.traverse { updatedTask =>
                              if (!TaskStatus.hasTerminated(updatedTask.status)) {
                                val stored = taskRepository
                                  .updateTask(Task.fromProvisionResult(result, task.operation).copy(id = task.id))
                                  .map { task =>
                                    logger.info(s"Stored updated task {}", task)
                                    task
                                  }
                                stored
                              } else {
                                IO.pure(updatedTask)
                              }
                            }
    } yield updatedTask

    action.handleErrorWith { error =>
      logger.error(s"Error while executing asynchronous task '${task.id}'", error)
      taskRepository.findTask(task.id).flatMap { updatedTask =>
        updatedTask.traverse { task =>
          if (!TaskStatus.hasTerminated(task.status)) {
            taskRepository.updateTask(
              task.copy(status = TaskStatus.FAILED, info = Info(errors = List(ErrorMessage(error.getMessage))))
            )
          } else IO.pure(task)
        }
      }
    }.startOn(executionContext)
  }
}
