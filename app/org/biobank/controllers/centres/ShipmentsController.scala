package org.biobank.controllers.centres

import javax.inject.{Inject, Singleton}
import org.biobank.controllers.{CommandController, JsonController}
import org.biobank.domain.centre.{CentreId, ShipmentId}
import org.biobank.service.centres.ShipmentsService
import org.biobank.service.users.UsersService
import org.biobank.service.{AuthToken, PagedQuery, PagedResults}
import play.api.libs.json._
import play.api.{Environment, Logger}
import scala.language.reflectiveCalls
import scalaz.Scalaz._
import scalaz.Validation.FlatMap._

/**
 *  Uses [[http://labs.omniti.com/labs/jsend JSend]] format for JSon replies.
 */
@Singleton
class ShipmentsController @Inject() (val env:              Environment,
                                     val authToken:        AuthToken,
                                     val usersService:     UsersService,
                                     val shipmentsService: ShipmentsService)
    extends CommandController
    with JsonController {

  import org.biobank.infrastructure.command.ShipmentCommands._
  import org.biobank.infrastructure.command.ShipmentSpecimenCommands._

  val log = Logger(this.getClass)

  private val PageSizeMax = 10

  def list(centreId:                  CentreId,
           courierFilterMaybe:        Option[String],
           trackingNumberFilterMaybe: Option[String],
           stateFilterMaybe:          Option[String],
           sortMaybe:                 Option[String],
           pageMaybe:                 Option[Int],
           pageSizeMaybe:             Option[Int],
           orderMaybe:                Option[String]) =
    AuthAction(parse.empty) { (token, userId, request) =>

      val courierFilter        = courierFilterMaybe.fold { "" } { cn => cn }
      val trackingNumberFilter = trackingNumberFilterMaybe.fold { "" } { tn => tn }
      val stateFilter          = stateFilterMaybe.fold { "" } { st => st }
      val sort                 = sortMaybe.fold { "courierName" } { s => s }
      val page                 = pageMaybe.fold { 1 } { p => p }
      val pageSize             = pageSizeMaybe.fold { 5 } { ps => ps }
      val order                = orderMaybe.fold { "asc" } { o => o }

      log.debug(
        s"""|ShipmentsController:list:
            | courierFilter:        $courierFilter,
            | trackingNumberFilter: $trackingNumberFilter,
            | stateFilter:          $stateFilter,
            | sort:                 $sort,
            | page:                 $page,
            | pageSize:             $pageSize,
            | order:                $order""".stripMargin)

      val pagedQuery = PagedQuery(page, pageSize, order)

      val validation = for {
          sortOrder   <- pagedQuery.getSortOrder
          shipments   <- shipmentsService.getShipments(centreId,
                                                       courierFilter,
                                                       trackingNumberFilter,
                                                       stateFilter,
                                                       sort,
                                                       sortOrder)
          page        <- pagedQuery.getPage(PageSizeMax, shipments.size)
          pageSize    <- pagedQuery.getPageSize(PageSizeMax)
          results     <- PagedResults.create(shipments, page, pageSize)
        } yield results

      validation.fold(
        err =>     BadRequest(err.toList.mkString),
        results => Ok(results)
      )
    }

  def get(id: ShipmentId) = AuthAction(parse.empty) { (token, userId, request) =>
    validationReply(shipmentsService.getShipment(id))
  }

  def listSpecimens(shipmentId:       ShipmentId,
                    stateFilterMaybe: Option[String],
                    sortMaybe:        Option[String],
                    pageMaybe:        Option[Int],
                    pageSizeMaybe:    Option[Int],
                    orderMaybe:       Option[String]) =
    AuthAction(parse.empty) { (token, userId, request) =>

      val stateFilter = stateFilterMaybe.fold { "" } { s => s }
      val sort        = sortMaybe.fold { "inventoryId" } { s => s }
      val page        = pageMaybe.fold { 1 } { p => p }
      val pageSize    = pageSizeMaybe.fold { 5 } { ps => ps }
      val order       = orderMaybe.fold { "asc" } { o => o }

      log.debug(s"""|ShipmentsController:listSpecimens:
                    | shipmentId:  $shipmentId,
                    | stateFilter: $stateFilter,
                    | sort:        $sort,
                    | page:        $page,
                    | pageSize:    $pageSize,
                    | order:       $order""".stripMargin)

      val pagedQuery = PagedQuery(page, pageSize, order)

      val validation = for {
          sortOrder         <- pagedQuery.getSortOrder
          shipmentSpecimens <- shipmentsService.getShipmentSpecimens(shipmentId, stateFilter, sort, sortOrder)
          page              <- pagedQuery.getPage(PageSizeMax, shipmentSpecimens.size)
          pageSize          <- pagedQuery.getPageSize(PageSizeMax)
          results           <- PagedResults.create(shipmentSpecimens, page, pageSize)
        } yield results

      validation.fold(
        err =>     BadRequest(err.list.toList.mkString),
        results => Ok(results)
      )
    }

  def canAddSpecimen(shipmentId: ShipmentId, specimenInventoryId: String) =
    AuthAction(parse.empty) { (token, userId, request) =>
      validationReply(shipmentsService.shipmentCanAddSpecimen(shipmentId, specimenInventoryId))
    }

  def getSpecimen(shipmentId: ShipmentId, shipmentSpecimenId: String) =
    AuthAction(parse.empty) { (token, userId, request) =>
      validationReply(shipmentsService.getShipmentSpecimen(shipmentId, shipmentSpecimenId))
    }

  def add() = commandActionAsync { cmd: AddShipmentCmd => processCommand(cmd) }

  def remove(shipmentId: ShipmentId, version: Long) =
    AuthActionAsync(parse.empty) { (token, userId, request) =>
      val cmd = ShipmentRemoveCmd(userId          = userId.id,
                                  id              = shipmentId.id,
                                  expectedVersion = version)
      val future = shipmentsService.removeShipment(cmd)
      validationReply(future)
    }

  def updateCourier(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : UpdateShipmentCourierNameCmd => processCommand(cmd) }

  def updateTrackingNumber(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : UpdateShipmentTrackingNumberCmd => processCommand(cmd) }

  def updateFromLocation(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : UpdateShipmentFromLocationCmd => processCommand(cmd) }

  def updateToLocation(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : UpdateShipmentToLocationCmd => processCommand(cmd) }

  /**
   * Changes the state of a shipment from CREATED to SENT (skipping the PACKED state)
   */
  def skipStateSent(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : ShipmentSkipStateToSentCmd => processCommand(cmd) }

  /**
   * Changes the state of a shipment from SENT to UNPACKED (skipping the RECEVIED state)
   */
  def skipStateUnpacked(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : ShipmentSkipStateToUnpackedCmd => processCommand(cmd) }

  def changeState(id: ShipmentId) =
    commandActionAsync(Json.obj("id" -> id)) { cmd : ShipmentChangeStateCmd => processCommand(cmd) }

  def addSpecimen(shipmentId: ShipmentId) = commandActionAsync(Json.obj("shipmentId" -> shipmentId)) {
      cmd: ShipmentSpecimenAddCmd => processSpecimenCommand(cmd)
    }

  def removeSpecimen(shipmentId: ShipmentId, shipmentSpecimenId: String, version: Long) =
    AuthActionAsync(parse.empty) { (token, userId, request) =>
      val cmd = ShipmentSpecimenRemoveCmd(userId          = userId.id,
                                          shipmentId      = shipmentId.id,
                                          id              = shipmentSpecimenId,
                                          expectedVersion = version)
      val future = shipmentsService.removeShipmentSpecimen(cmd)
      validationReply(future)
    }

  def specimenContainer(shipmentId: ShipmentId, shipmentSpecimenId: String) =
    commandActionAsync(Json.obj("shipmentId" -> shipmentId, "id" -> shipmentSpecimenId)) {
      cmd: ShipmentSpecimenUpdateContainerCmd => processSpecimenCommand(cmd)
    }

  def specimenReceived(shipmentId: ShipmentId, shipmentSpecimenId: String) =
    commandActionAsync(Json.obj("shipmentId" -> shipmentId, "id" -> shipmentSpecimenId)) {
      cmd: ShipmentSpecimenReceivedCmd => processSpecimenCommand(cmd)
    }

  def specimenMissing(shipmentId: ShipmentId, shipmentSpecimenId: String) =
    commandActionAsync(Json.obj("shipmentId" -> shipmentId, "id" -> shipmentSpecimenId)) {
      cmd: ShipmentSpecimenMissingCmd => processSpecimenCommand(cmd)
    }

  def specimenExtra(shipmentId: ShipmentId, shipmentSpecimenId: String) =
    commandActionAsync(Json.obj("shipmentId" -> shipmentId, "id" -> shipmentSpecimenId)) {
      cmd: ShipmentSpecimenExtraCmd => processSpecimenCommand(cmd)
    }

  private def processCommand(cmd: ShipmentCommand) = {
    val future = shipmentsService.processCommand(cmd)
    validationReply(future)
  }

  private def processSpecimenCommand(cmd: ShipmentSpecimenCommand) = {
    val future = shipmentsService.processShipmentSpecimenCommand(cmd)
    validationReply(future)
  }
}
