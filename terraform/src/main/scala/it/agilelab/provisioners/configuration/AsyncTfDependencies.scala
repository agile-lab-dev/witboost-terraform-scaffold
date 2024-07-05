package it.agilelab.provisioners.configuration
import com.typesafe.config.Config
import it.agilelab.spinframework.app.config.AsynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.status.{ GetStatus, StatusService, TaskRepository }

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class AsyncTfDependencies(repositoryConfig: Config, poolSize: Int)
    extends TfDependencies
    with AsynchronousSpecificProvisionerDependencies {

  override val taskRepository: TaskRepository     = TaskRepository.fromConfig(repositoryConfig)
  override val getStatus: GetStatus               = new StatusService(taskRepository)
  override val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(poolSize))

}
