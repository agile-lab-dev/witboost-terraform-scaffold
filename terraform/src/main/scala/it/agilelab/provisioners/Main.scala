package it.agilelab.provisioners

import it.agilelab.provisioners.configuration.TfDependencies
import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies

object Main extends SpecificProvisioner {
  override val specificProvisionerDependencies: SynchronousSpecificProvisionerDependencies = new TfDependencies
}
