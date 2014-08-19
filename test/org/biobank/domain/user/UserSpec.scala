package org.biobank.domain.user

import org.biobank.domain.DomainSpec
import org.biobank.fixture.NameGenerator

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.OptionValues._
import org.scalatest.Tag
import com.github.nscala_time.time.Imports._
import org.slf4j.LoggerFactory
import scalaz._
import scalaz.Scalaz._

/**
 * Note: to run from Eclipse uncomment the @RunWith line. To run from SBT the line should be
 * commented out.
 *
 */
//@RunWith(classOf[JUnitRunner])
class UserSpec extends DomainSpec {

  val log = LoggerFactory.getLogger(this.getClass)

  val nameGenerator = new NameGenerator(this.getClass)

  "A user" can {

    "be created" in {
      val version = -1L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val id = UserId(email)
      val password = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      val timeNow = DateTime.now

      val validation = RegisteredUser.create(
        id, version, timeNow, name, email, password, salt, avatarUrl)
      validation should be ('success)
      validation map { user =>
        user shouldBe a[RegisteredUser]
        user should have (
          'id (id),
          'version (0L),
          'name (name),
          'email (email),
          'password (password),
          'salt (salt),
          'avatarUrl (avatarUrl)
        )

        user.addedDate should be (timeNow)
        user.lastUpdateDate should be (None)
      }
    }

    "can be activated, locked, and unlocked" in {
      val user = factory.createRegisteredUser

      val activeUser = user.activate(user.versionOption, DateTime.now)  | fail
      activeUser shouldBe a[ActiveUser]
      activeUser.version should be(user.version + 1)
      activeUser.addedDate should be (user.addedDate)
      var updateDate = activeUser.lastUpdateDate | fail
        (updateDate to DateTime.now).millis should be < 200L

      val lockedUser = activeUser.lock(activeUser.versionOption, DateTime.now) | fail
      lockedUser shouldBe a[LockedUser]
      lockedUser.version should be(activeUser.version + 1)
      lockedUser.addedDate should be (user.addedDate)
      updateDate = lockedUser.lastUpdateDate | fail
        (updateDate to DateTime.now).millis should be < 200L

      val unlockedUser = lockedUser.unlock(lockedUser.versionOption, DateTime.now) | fail
      unlockedUser shouldBe a[ActiveUser]
      unlockedUser.version should be(lockedUser.version + 1)
      unlockedUser.addedDate should be (user.addedDate)
      updateDate = unlockedUser.lastUpdateDate | fail
        (updateDate to DateTime.now).millis should be < 200L
    }
  }

  "An active user" can {

    "be updated" in {
      val user = factory.createActiveUser

      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test2.com/")

      val validation = user.update(
        user.versionOption, DateTime.now, name, email, password, salt, avatarUrl)
      validation should be ('success)
      validation map { user2 =>
        user2 shouldBe a[ActiveUser]
        user2 should have (
          'id (user.id),
          'version (user.version + 1),
          'name (name),
          'email (email),
          'password (password),
          'salt (salt),
          'avatarUrl (avatarUrl)
        )

        user2.addedDate should be (user.addedDate)
        val updateDate = user2.lastUpdateDate | fail
          (updateDate to DateTime.now).millis should be < 200L
      }
    }

  }

  "A user" should {

    "not be created with an empty id" in {
      val id = UserId("")
      val version = -1L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      RegisteredUser.create(
        id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => err.list should (have length 1 and contain("id is null or empty")),
        user => fail("id validation failed")
      )
    }

    "not be created with an invalid version" in {
      val id = UserId(nameGenerator.next[User])
      val version = -2L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      RegisteredUser.create(
        id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => err.list should (have length 1 and contain("invalid version value: -2")),
        user => fail("version validation failed")
      )
    }

    "not be updated with an invalid version" taggedAs(Tag("single")) in {
      val user = factory.createActiveUser

      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test3.com/")

      val validation = user.update(
        Some(user.version - 1), DateTime.now, name, email, password, salt, avatarUrl)
      validation should be ('failure)
      validation.swap.map { err =>
        err.list should have length 1
              err.list.head should include ("expected version doesn't match current version")
      }
    }

    "not be created with an empty name" in {
      val id = UserId(nameGenerator.next[User])
      val version = 0L
      val name = ""
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      RegisteredUser.create(
        id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => err.list should (have length 1 and contain("name is null or empty")),
        user => fail("name validation failed")
      )
    }

    "not be created with an empty password" in {
      val id = UserId(nameGenerator.next[User])
      val version = 0L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = ""
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      RegisteredUser.create(
        id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => err.list should (have length 1 and contain("password is null or empty")),
        user => fail("user password validation failed")
      )
    }

    "not be created with an empty salt option" in {
      val id = UserId(nameGenerator.next[User])
      val version = 0L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = ""
      val avatarUrl = Some("http://test.com/")

      RegisteredUser.create(id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => err.list should (have length 1 and contain("salt is null or empty")),
        user => fail("user salt validation failed")
      )
    }

    "not be created with an invalid avatar url" in {
      val id = UserId(nameGenerator.next[User])
      val version = 0L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some(nameGenerator.next[User])

      RegisteredUser.create(id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => {
          err.list should have length 1
          err.list.head should include("invalid avatar url")
        },
        user => fail("user avaltar url validation failed")
      )
    }

    "pass authentication" in {
      val id = UserId(nameGenerator.next[User])
      val version = 0L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      val v = RegisteredUser.create(id, version, DateTime.now, name, email, password, salt, avatarUrl)
      val user = v.getOrElse(fail("could not create user"))
      val authenticatedUser = user.authenticate(email, password).getOrElse(fail("could authenticate user"))
      authenticatedUser should be(user)
    }

    "fail authentication for bad password" in {
      val id = UserId(nameGenerator.next[User])
      val version = 0L
      val name = nameGenerator.next[User]
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      val badPassword = nameGenerator.next[User]

      val v = RegisteredUser.create(id, version, DateTime.now, name, email, password, salt, avatarUrl)
      val user = v.getOrElse(fail("could not create user"))
      user.authenticate(email, badPassword).fold(
        err => err.list should (have length 1 and contain("authentication failure")),
        x => fail("authentication should fail")
      )
    }

    "have more than one validation fail" in {
      val id = UserId(nameGenerator.next[User])
      val version = -2L
      val name = ""
      val email = nameGenerator.nextEmail[User]
      val password = nameGenerator.next[User]
      val hasher = nameGenerator.next[User]
      val salt = nameGenerator.next[User]
      val avatarUrl = Some("http://test.com/")

      val badPassword = nameGenerator.next[User]

      RegisteredUser.create(id, version, DateTime.now, name, email, password, salt, avatarUrl).fold(
        err => {
          err.list should have length 2
          err.list.head should be ("invalid version value: -2")
          err.list.tail.head should be ("name is null or empty")
        },
        user => fail
      )
    }

  }

}