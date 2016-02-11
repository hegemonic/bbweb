package org.biobank.domain.containers

import org.biobank.domain._
import org.biobank.infrastructure.JsonUtils._

import play.api.libs.json._
import org.joda.time.DateTime
import scalaz.Scalaz._

trait ContainerSchemaValidations {

  val NameMinLength = 2

}


/**
 * A plan for how the children in a {@link Container} are positioned and labelled.
 */
case class ContainerSchema(id:           ContainerSchemaId,
                           version:      Long,
                           timeAdded:    DateTime,
                           timeModified: Option[DateTime],
                           name:         String,
                           description:  Option[String],
                           shared:       Boolean)
    extends ConcurrencySafeEntity[ContainerSchemaId]
    with HasUniqueName
    with HasDescriptionOption
    with ContainerSchemaValidations {
  import CommonValidations._

  /** Used to change the name. */
  def withName(name: String): DomainValidation[ContainerSchema] = {
    validateString(name, NameMinLength, InvalidName) fold (
      err => err.failure,
      s   => copy(version = version + 1, name = name).success
    )
  }

  /** Used to change the description. */
  def withDescription(description: Option[String]): DomainValidation[ContainerSchema] = {
    validateNonEmptyOption(description, InvalidDescription) fold (
      err => err.failure,
      s   => copy(version = version + 1, description  = description).success
    )
  }

  def withShared(shared: Boolean): DomainValidation[ContainerSchema] = {
    copy(version = version + 1, shared  = shared).success
  }

}

/**
  * Factory object used to create a container schema.
  */
object ContainerSchema extends ContainerSchemaValidations {
  import CommonValidations._

  /**
    * The factory method to create a container schema.
    *
    * Performs validation on fields.
    */
  def create(id:          ContainerSchemaId,
             version:     Long,
             name:        String,
             description: Option[String],
             shared:      Boolean)
      : DomainValidation[ContainerSchema] = {
    (validateId(id) |@|
      validateAndIncrementVersion(version) |@|
      validateString(name, NameMinLength, InvalidName) |@|
      validateNonEmptyOption(description, InvalidDescription)) {
        ContainerSchema(_, _, DateTime.now, None, _, _, shared)
      }
  }

  implicit val containerSchemaWrites = Json.writes[ContainerSchema]
}
