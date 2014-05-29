package org.biobank.domain

import org.biobank.domain.validation.UserValidationHelper
import org.biobank.infrastructure.event.UserEvents._
import com.github.nscala_time.time.Imports._
import scalaz._
import scalaz.Scalaz._

/** A user of the system.
  */
sealed trait User extends ConcurrencySafeEntity[UserId] {

  /** The user's full name. */
  val name: String

  /** The user's email. Must be unique to the system. */
  val email: String

  /** The user's password */
  val password: String

  /** The string used to hash the password. */
  val hasher: String

  /** The string used to salt the password. */
  val salt: Option[String]

  /** An optional URL to the user's avatar icon. */
  val avatarUrl: Option[String]

  /**
   * Authenticate a user.
   */
  def authenticate(email: String, password: String): DomainValidation[User] = {
    if (this.password.equals(password)) this.success
    else DomainError("authentication failure").failNel
  }

  override def toString =
    s"""|${this.getClass.getSimpleName}: {
        |  id: $id,
        |  version: $version,
        |  addedDate: $addedDate,
        |  lastUpdateDate: $lastUpdateDate,
        |  name: $name,
        |  email: $email
        |}""".stripMargin
}

/** A user that just registered with the system. This user does not yet have full access
  * the system.
  */
case class RegisteredUser private (
  id: UserId,
  version: Long,
  addedDate: DateTime,
  lastUpdateDate: Option[DateTime],
  name: String,
  email: String,
  password: String,
  hasher: String,
  salt: Option[String],
  avatarUrl: Option[String]) extends User {

  /* Activates a registered user. */
  def activate(
    expectedVersion: Option[Long],
    dateTime: DateTime): DomainValidation[ActiveUser] = {
    for {
      validVersion <- requireVersion(expectedVersion)
      validatedUser <- ActiveUser.create(this)
      activatedUser <- validatedUser.copy(lastUpdateDate = Some(dateTime)).success
    } yield activatedUser
  }
}

/** Factory object. */
object RegisteredUser extends UserValidationHelper {

  /** Creates a registered user. */
  def create(
    id: UserId,
    version: Long,
    dateTime: DateTime,
    name: String,
    email: String,
    password: String,
    hasher: String,
    salt: Option[String],
    avatarUrl: Option[String]): DomainValidation[RegisteredUser] = {

    (validateId(id) |@|
      validateAndIncrementVersion(version) |@|
      validateNonEmpty(name, "name is null or empty") |@|
      validateEmail(email) |@|
      validateNonEmpty(password, "password is null or empty") |@|
      validateNonEmpty(hasher, "hasher is null or empty") |@|
      validateNonEmptyOption(salt, "salt is null or empty") |@|
      validateAvatarUrl(avatarUrl)) {
        RegisteredUser(_, _, dateTime, None, _, _, _, _, _, _)
      }
  }

}

/** A user that has access to the system. */
case class ActiveUser private (
  id: UserId,
  version: Long = -1,
  addedDate: DateTime,
  lastUpdateDate: Option[DateTime],
  name: String,
  email: String,
  password: String,
  hasher: String,
  salt: Option[String],
  avatarUrl: Option[String]) extends User {

  /** Locks an active user. */
  def lock(
    expectedVersion: Option[Long],
    dateTime: DateTime): DomainValidation[LockedUser] = {
    for {
      validVersion <- requireVersion(expectedVersion)
      validatedUser <- LockedUser.create(this)
      lockedUser <- validatedUser.copy(lastUpdateDate = Some(dateTime)).success
    } yield lockedUser
  }

  def update(
    expectedVersion: Option[Long],
    dateTime: DateTime,
    name: String,
    email: String,
    password: String,
    hasher: String,
    salt: Option[String],
    avatarUrl: Option[String]) = {

    for {
      validVersion <- requireVersion(expectedVersion)
      validatedUser <- RegisteredUser.create(id, version, addedDate, name, email, password, hasher,
        salt, avatarUrl)
      registeredUser <- validatedUser.activate(validatedUser.versionOption, dateTime)
      udpatedUser <- registeredUser.copy(
        version = version + 1,
        lastUpdateDate = Some(dateTime)).success
    } yield udpatedUser
  }
}

/** Factory object. */
object ActiveUser extends UserValidationHelper {

  /** Creates an active user from a registered user. */
  def create[T <: User](user: T): DomainValidation[ActiveUser] = {
    (validateId(user.id) |@|
      validateAndIncrementVersion(user.version) |@|
      validateNonEmpty(user.name, "name is null or empty") |@|
      validateEmail(user.email) |@|
      validateNonEmpty(user.password, "password is null or empty") |@|
      validateNonEmpty(user.hasher, "hasher is null or empty") |@|
      validateNonEmptyOption(user.salt, "salt is null or empty") |@|
      validateAvatarUrl(user.avatarUrl)) {
        ActiveUser(_, _, user.addedDate, None, _, _, _, _, _, _)
      }
  }

}

/** A user who no longer has access to the system. */
case class LockedUser private (
  id: UserId,
  version: Long = -1,
  addedDate: DateTime,
  lastUpdateDate: Option[DateTime],
  name: String,
  email: String,
  password: String,
  hasher: String,
  salt: Option[String],
  avatarUrl: Option[String]) extends User {

  /** Unlocks a locked user. */
  def unlock(
    expectedVersion: Option[Long],
    dateTime: DateTime): DomainValidation[ActiveUser] = {
    for {
      validVersion <- requireVersion(expectedVersion)
      validatedUser <- ActiveUser.create(this)
      activeUser <- validatedUser.copy(lastUpdateDate = Some(dateTime)).success
    } yield activeUser
  }

}

/** Factory object. */
object LockedUser extends UserValidationHelper {

  /** Creates an active user from a locked user. */
  def create(user: ActiveUser): DomainValidation[LockedUser] = {
    (validateId(user.id) |@|
      validateAndIncrementVersion(user.version) |@|
      validateNonEmpty(user.name, "name is null or empty") |@|
      validateEmail(user.email) |@|
      validateNonEmpty(user.password, "password is null or empty") |@|
      validateNonEmpty(user.hasher, "hasher is null or empty") |@|
      validateNonEmptyOption(user.salt, "salt is null or empty") |@|
      validateAvatarUrl(user.avatarUrl)) {
        LockedUser(_, _, user.addedDate, None, _, _, _, _, _, _)
      }
  }

}

