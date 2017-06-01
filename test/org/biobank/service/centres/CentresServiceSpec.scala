package org.biobank.service.centres

import org.biobank.fixture._
import org.biobank.domain._
import org.biobank.domain.access._
import org.biobank.domain.ConcurrencySafeEntity
import org.biobank.domain.centre._
import org.biobank.domain.study._
import org.biobank.domain.user._
import org.biobank.service.{FilterString, SortString}
import org.biobank.service.users._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks._

/**
 * Primarily these are tests that exercise the User Access aspect of CentresService.
 */
class CentresServiceSpec
    extends ProcessorTestFixture
    with UserServiceFixtures
    with ScalaFutures {

  import org.biobank.TestUtils._
  import org.biobank.infrastructure.command.CentreCommands._

  class UsersWithCentreAccessFixture {
    val location             = factory.createLocation
    val centre               = factory.createDisabledCentre.copy(locations = Set(location))

    val allCentresAdminUser    = factory.createActiveUser
    val centreOnlyAdminUser    = factory.createActiveUser
    val centreUser             = factory.createActiveUser
    val noMembershipUser       = factory.createActiveUser
    val noCentrePermissionUser = factory.createActiveUser

    val allCentresMembership = factory.createMembership.copy(
        userIds = Set(allCentresAdminUser.id),
        centreInfo = MembershipCentreInfo(true, Set.empty[CentreId]))

    val centreOnlyMembership = factory.createMembership.copy(
        userIds = Set(centreOnlyAdminUser.id, centreUser.id),
        centreInfo = MembershipCentreInfo(false, Set(centre.id)))

    val noCentresMembership = factory.createMembership.copy(
        userIds = Set(noMembershipUser.id, noCentrePermissionUser.id),
        centreInfo = MembershipCentreInfo(false, Set.empty[CentreId]))

    def usersCanReadTable() = Table(("users with read access", "label"),
                                    (allCentresAdminUser, "all centres admin user"),
                                    (centreOnlyAdminUser,  "centre only admin user"),
                                    (centreUser,           "non-admin centre user"))

    def usersCanAddOrUpdateTable() = Table(("users with update access", "label"),
                                      (allCentresAdminUser, "all centres admin user"),
                                      (centreOnlyAdminUser,  "centre only admin user"))

    def usersCannotAddOrUpdateTable() = Table(("users with update access", "label"),
                                         (centreUser,             "non-admin centre user"),
                                         (noMembershipUser,       "all centres admin user"),
                                         (noCentrePermissionUser, "centre only admin user"))
    Set(centre,
        allCentresAdminUser,
        centreOnlyAdminUser,
        centreUser,
        noMembershipUser,
        noCentrePermissionUser,
        allCentresMembership,
        centreOnlyMembership,
        noCentresMembership
    ).foreach(addToRepository)

    addUserToCentreAdminRole(allCentresAdminUser)
    addUserToCentreAdminRole(centreOnlyAdminUser)
    addUserToRole(centreUser, RoleId.CentreUser)
    addUserToRole(noMembershipUser, RoleId.CentreUser)
  }

  class CentresOfAllStatesFixure extends UsersWithCentreAccessFixture {
    val disabledCentre = centre
    val enabledCentre = factory.createEnabledCentre

    Set(disabledCentre, enabledCentre).foreach(addToRepository)
    addToRepository(centreOnlyMembership.copy(
                      centreInfo = centreOnlyMembership.centreInfo.copy(
                          centreIds = Set(disabledCentre.id, enabledCentre.id))))

  }

  protected val nameGenerator = new NameGenerator(this.getClass)

  protected val accessItemRepository = app.injector.instanceOf[AccessItemRepository]

  protected val membershipRepository = app.injector.instanceOf[MembershipRepository]

  protected val userRepository = app.injector.instanceOf[UserRepository]

  private val centreRepository = app.injector.instanceOf[CentreRepository]

  private val studyRepository = app.injector.instanceOf[StudyRepository]

  private val centresService = app.injector.instanceOf[CentresService]

  private def addUserToCentreAdminRole(user: User): Unit = {
    addUserToRole(user, RoleId.CentreAdministrator)
  }

  override protected def addToRepository[T <: ConcurrencySafeEntity[_]](entity: T): Unit = {
    entity match {
      case u: User       => userRepository.put(u)
      case i: AccessItem => accessItemRepository.put(i)
      case s: Study      => studyRepository.put(s)
      case c: Centre     => centreRepository.put(c)
      case m: Membership => membershipRepository.put(m)
      case _             => fail("invalid entity")
    }
  }

  private def updateCommandsTable(sessionUserId: UserId,
                                  centre:        Centre,
                                  location:      Location,
                                  study:         Study) = {
    Table("centre update commands",
          UpdateCentreNameCmd(
            sessionUserId   = sessionUserId.id,
            id              = centre.id.id,
            expectedVersion = centre.version,
            name            = nameGenerator.next[String]
          ),
          UpdateCentreDescriptionCmd(
            sessionUserId   = sessionUserId.id,
            id              = centre.id.id,
            expectedVersion = centre.version,
            description     = Some(nameGenerator.next[String])
          ),
          AddCentreLocationCmd(
            sessionUserId   = sessionUserId.id,
            id              = centre.id.id,
            expectedVersion = centre.version,
            name            = location.name,
            street          = location.street,
            city            = location.city,
            province        = location.province,
            postalCode      = location.postalCode,
            poBoxNumber     = location.poBoxNumber,
            countryIsoCode  = location.countryIsoCode
          ),
          RemoveCentreLocationCmd(
            sessionUserId   = sessionUserId.id,
            id              = centre.id.id,
            expectedVersion = centre.version,
            locationId      = location.id.id
          ),
          AddStudyToCentreCmd(
            sessionUserId   = sessionUserId.id,
            id              = centre.id.id,
            expectedVersion = centre.version,
            studyId         = study.id.id
          ),
          RemoveStudyFromCentreCmd(
            sessionUserId   = sessionUserId.id,
            id              = centre.id.id,
            expectedVersion = centre.version,
            studyId         = study.id.id
          )
    )
  }

  private def stateChangeCommandsTable(sessionUserId:  UserId,
                                       disabledCentre:  DisabledCentre,
                                       enabledCentre:   EnabledCentre) =
    Table("user sate change commands",
          EnableCentreCmd(
            sessionUserId   = sessionUserId.id,
            id              = disabledCentre.id.id,
            expectedVersion = disabledCentre.version
          ),
          DisableCentreCmd(
            sessionUserId   = sessionUserId.id,
            id              = enabledCentre.id.id,
            expectedVersion = enabledCentre.version
          )
    )

  override def beforeEach() {
    super.beforeEach()
    removeUsersFromRepository
    restoreRoles
    centreRepository.removeAll
  }

  describe("CentresService") {

    describe("when getting centre counts") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        forAll (f.usersCanReadTable) { (user, label) =>
          info(label)
          centresService.getCentresCount(user.id) mustSucceed { count =>
            count must be (1)
          }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture

        info("no membership user")
        centresService.getCentresCount(f.noMembershipUser.id) mustSucceed { count =>
          count must be (0)
        }

        info("no permission user")
        centresService.getCentresCount(f.noCentrePermissionUser.id) mustFail "Unauthorized"
      }

    }

    describe("when getting centre counts by status") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        forAll (f.usersCanReadTable) { (user, label) =>
          info(label)
          centresService.getCountsByStatus(user.id) mustSucceed { counts =>
            counts.total must be (1)
          }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture
        info("no membership user")
        centresService.getCountsByStatus(f.noMembershipUser.id) mustSucceed { counts =>
          counts.total must be (0)
        }

        info("no permission user")
        centresService.getCountsByStatus(f.noCentrePermissionUser.id) mustFail "Unauthorized"
      }

    }

    describe("when getting centres") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        forAll (f.usersCanReadTable) { (user, label) =>
          info(label)
          centresService.getCentres(user.id, new FilterString(""), new SortString(""))
            .mustSucceed { centres =>
              centres must have length (1)
            }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture
        info("no membership user")
        centresService.getCentres(f.noMembershipUser.id, new FilterString(""), new SortString(""))
          .mustSucceed { centres =>
            centres must have length (0)
          }

        info("no permission user")
        centresService.getCentres(f.noCentrePermissionUser.id, new FilterString(""), new SortString(""))
          .mustFail("Unauthorized")
      }

    }

    describe("when getting centre names") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        forAll (f.usersCanReadTable) { (user, label) =>
          info(label)
          centresService.getCentreNames(user.id, new FilterString(""), new SortString(""))
            .mustSucceed { centres =>
              centres must have length (1)
            }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture
        info("no membership user")
        centresService.getCentreNames(f.noMembershipUser.id, new FilterString(""), new SortString(""))
          .mustSucceed { centres =>
            centres must have length (0)
          }

        info("no permission user")
        centresService.getCentreNames(f.noCentrePermissionUser.id, new FilterString(""), new SortString(""))
          .mustFail("Unauthorized")
      }

    }

    describe("when getting a centre") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        forAll (f.usersCanReadTable) { (user, label) =>
          info(label)
          centresService.getCentre(user.id, f.centre.id) mustSucceed { result =>
            result.id must be (f.centre.id)
          }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture

        info("no membership user")
        centresService.getCentre(f.noMembershipUser.id, f.centre.id) mustFail "Unauthorized"

        info("no permission user")
        centresService.getCentre(f.noCentrePermissionUser.id, f.centre.id) mustFail "Unauthorized"
      }

    }

    describe("search locations") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        forAll (f.usersCanReadTable) { (user, label) =>
          info(label)
          val cmd = SearchCentreLocationsCmd(sessionUserId = user.id.id,
                                             filter        = "",
                                             limit         = 10)
          centresService.searchLocations(cmd).mustSucceed { centres =>
            centres must have size (1)
          }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture
        var cmd = SearchCentreLocationsCmd(sessionUserId = f.noMembershipUser.id.id,
                                           filter        = "",
                                           limit         = 10)

        info("no membership user")
        centresService.searchLocations(cmd).mustSucceed { centres =>
          centres must have size (0)
        }

        cmd = SearchCentreLocationsCmd(sessionUserId = f.noCentrePermissionUser.id.id,
                                       filter        = "",
                                       limit         = 10)
        info("no permission user")
        centresService.searchLocations(cmd) mustFail "Unauthorized"
      }

    }

    describe("when adding a centre") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture

        forAll (f.usersCanAddOrUpdateTable) { (user, label) =>
          val cmd = AddCentreCmd(sessionUserId = user.id.id,
                                name           = f.centre.name,
                                description    = f.centre.description)
          centreRepository.removeAll
          centresService.processCommand(cmd).futureValue mustSucceed { s =>
            s.name must be (f.centre.name)
          }
        }
      }

      it("users cannot access") {
        val f = new UsersWithCentreAccessFixture

        forAll (f.usersCannotAddOrUpdateTable) { (user, label) =>
          val cmd = AddCentreCmd(sessionUserId = user.id.id,
                                name           = f.centre.name,
                                description    = f.centre.description)
          centresService.processCommand(cmd).futureValue mustFail "Unauthorized"
        }
      }

    }

    describe("update a centre") {

      it("users can access") {
        val f = new UsersWithCentreAccessFixture
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        forAll (f.usersCanAddOrUpdateTable) { (user, label) =>
          info(label)
          forAll(updateCommandsTable(user.id, f.centre, f.location, study)) { cmd =>
            val centre = cmd match {
                case _: AddCentreLocationCmd =>
                  f.centre.copy(locations = Set.empty[Location])
                case _: RemoveStudyFromCentreCmd =>
                  f.centre.copy(studyIds = Set(study.id))
                case _ =>
                  f.centre
              }

            centreRepository.put(centre) // restore the centre to it's previous state
            centresService.processCommand(cmd).futureValue mustSucceed { s =>
              s.id must be (centre.id)
            }
          }
        }
      }

      it("users cannot update") {
        val f = new UsersWithCentreAccessFixture
        val study = factory.createDisabledStudy
        studyRepository.put(study)

        forAll (f.usersCannotAddOrUpdateTable) { (user, label) =>
          forAll(updateCommandsTable(user.id, f.centre, f.location, study)) { cmd =>
            centresService.processCommand(cmd).futureValue mustFail "Unauthorized"
          }
        }
      }

    }

    describe("change a centre's state") {

      it("users can access") {
        val f = new CentresOfAllStatesFixure
        forAll (f.usersCanAddOrUpdateTable) { (user, label) =>
          info(label)
          forAll(stateChangeCommandsTable(user.id,
                                          f.disabledCentre,
                                          f.enabledCentre)) { cmd =>
            Set(f.disabledCentre, f.enabledCentre).foreach(addToRepository)
            centresService.processCommand(cmd).futureValue mustSucceed { s =>
              s.id.id must be (cmd.id)
            }
          }
        }
      }

      it("users cannot update") {
        val f = new CentresOfAllStatesFixure
        forAll (f.usersCannotAddOrUpdateTable) { (user, label) =>
          info(label)
          forAll(stateChangeCommandsTable(user.id,
                                          f.disabledCentre,
                                          f.enabledCentre)) { cmd =>
            centresService.processCommand(cmd).futureValue mustFail "Unauthorized"
          }
        }
      }
    }

    describe("centres membership") {

      it("user has access to all centres corresponding his membership") {
        val secondCentre = factory.createDisabledCentre  // should show up in results
        addToRepository(secondCentre)

        val f = new UsersWithCentreAccessFixture
        centresService.getCentres(f.allCentresAdminUser.id, new FilterString(""), new SortString(""))
          .mustSucceed { reply =>
            reply must have size (2)
            val centreIds = reply.map(c => c.id)
            centreIds must contain (f.centre.id)
            centreIds must contain (secondCentre.id)
          }
      }

      it("user has access only to centres corresponding his membership") {
        val secondCentre = factory.createDisabledCentre  // should show up in results
        addToRepository(secondCentre)

        val f = new UsersWithCentreAccessFixture
        centresService.getCentres(f.centreOnlyAdminUser.id, new FilterString(""), new SortString(""))
          .mustSucceed { reply =>
            reply must have size (1)
            reply.map(c => c.id) must contain (f.centre.id)
          }
      }

      it("user does not have access to centre if not in membership") {
        val f = new UsersWithCentreAccessFixture

        // remove all studies from membership
        val noCentresMembership = f.centreOnlyMembership.copy(
            centreInfo = f.centreOnlyMembership.centreInfo.copy(centreIds = Set.empty[CentreId]))
        addToRepository(noCentresMembership)

        // should not show up in results
        val secondStudy = factory.createDisabledStudy
        addToRepository(secondStudy)

        centresService.getCentres(f.centreUser.id, new FilterString(""), new SortString(""))
          .mustSucceed { reply =>
            reply must have size (0)
          }
      }

    }

  }

}
