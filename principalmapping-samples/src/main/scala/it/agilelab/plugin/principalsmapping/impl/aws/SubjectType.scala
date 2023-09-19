package it.agilelab.plugin.principalsmapping.impl.aws

object SubjectType extends Enumeration{
  type SubjectType = Value
  val user, group, unknown = Value

  val userPrefix = "user:"
  val groupPrefix = "group:"

  def mapType(subj : String) : SubjectType = {
    if ( subj.startsWith(groupPrefix))
      SubjectType.group
    else if (subj.startsWith(userPrefix))
      SubjectType.user
    else
      SubjectType.unknown
  }

  def extract(subject: String) : String = {
    subject.split(":")(1)
  }

}