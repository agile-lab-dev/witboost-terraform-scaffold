package it.agilelab.plugin.principalsmapping.api


trait Mapper {

  /** This method defines the main mapping logic
   *
   * @param subjects set of subjects, i.e. witboost users and groups
   * @return the mapping. For each subject, we can return either Throwable, or the successfully mapped principal
   */
  def map(subjects: Set[String]): Map[String, Either[Throwable, String]]
}
