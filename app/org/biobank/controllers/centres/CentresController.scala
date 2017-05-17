package org.biobank.controllers.centres

import javax.inject.{Inject, Singleton}
import org.biobank.controllers._
import org.biobank.domain.centre.CentreId
import org.biobank.infrastructure.command.CentreCommands._
import org.biobank.service._
import org.biobank.service.centres.CentresService
import play.api.libs.json._
import play.api.mvc._
import play.api.{ Environment, Logger }
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz.Validation.FlatMap._

/**
 *  Uses [[http://labs.omniti.com/labs/jsend JSend]] format for JSon replies.
 */
@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
@Singleton
class CentresController @Inject() (val action:         BbwebAction,
                                   val env:            Environment,
                                   val centresService: CentresService)
                               (implicit val ec: ExecutionContext)
    extends CommandController {

  val log: Logger = Logger(this.getClass)

  private val PageSizeMax = 10

  def centreCounts(): Action[Unit] =
    action(parse.empty) { implicit request =>
      Ok(centresService.getCountsByStatus)
    }

  def list: Action[Unit] =
    action.async(parse.empty) { implicit request =>
      validationReply(
        Future {
          for {
            pagedQuery <- PagedQuery.create(request.rawQueryString, PageSizeMax)
            centres    <- centresService.getCentres(pagedQuery.filter, pagedQuery.sort)
            validPage  <- pagedQuery.validPage(centres.size)
            results    <- PagedResults.create(centres, pagedQuery.page, pagedQuery.limit)
          } yield results
        }
      )
    }

  def listNames: Action[Unit] =
    action.async(parse.empty) { implicit request =>
      validationReply(
        Future {
          for {
            filterAndSort <- FilterAndSortQuery.create(request.rawQueryString)
            centreNames    <- centresService.getCentreNames(filterAndSort.filter, filterAndSort.sort)
          } yield centreNames
        }
      )
    }

  def searchLocations(): Action[JsValue] =
    commandAction[SearchCentreLocationsCmd](JsNull){ cmd =>
      Future.successful(Ok(centresService.searchLocations(cmd)))
    }

  def query(id: CentreId): Action[Unit] =
    action(parse.empty) { implicit request =>
      validationReply(centresService.getCentre(id))
    }

  def snapshot: Action[Unit] =
    action.async(parse.empty) { implicit request =>
      centresService.snapshot
      Future.successful(Results.Ok(Json.obj("status" ->"success", "data" -> true)))
    }

  def add(): Action[JsValue] = commandAction[AddCentreCmd](JsNull)(processCommand)

  def updateName(id: CentreId): Action[JsValue] =
    commandAction[UpdateCentreNameCmd](Json.obj("id" -> id))(processCommand)

  def updateDescription(id: CentreId): Action[JsValue] =
    commandAction[UpdateCentreDescriptionCmd](Json.obj("id" -> id))(processCommand)

  def addStudy(centreId: CentreId): Action[JsValue] =
    commandAction[AddStudyToCentreCmd](Json.obj("id" -> centreId))(processCommand)

  def removeStudy(centreId: CentreId, ver: Long, studyId: String): Action[Unit] =
    action.async(parse.empty) { implicit request =>
      processCommand(RemoveStudyFromCentreCmd(request.authInfo.userId.id, centreId.id, ver, studyId))
    }

  def addLocation(id: CentreId): Action[JsValue] =
    commandAction[AddCentreLocationCmd](Json.obj("id" -> id))(processCommand)

  def updateLocation(id: CentreId, locationId: String): Action[JsValue] = {
    val json = Json.obj("id" -> id, "locationId" -> locationId)
    commandAction[UpdateCentreLocationCmd](json)(processCommand)
  }

  def removeLocation(centreId: CentreId, ver: Long, locationId: String): Action[Unit] =
    action.async(parse.empty) { implicit request =>
      processCommand(RemoveCentreLocationCmd(request.authInfo.userId.id, centreId.id, ver, locationId))
    }

  def enable(id: CentreId): Action[JsValue] =
    commandAction[EnableCentreCmd](Json.obj("id" -> id))(processCommand)

  def disable(id: CentreId): Action[JsValue] =
    commandAction[DisableCentreCmd](Json.obj("id" -> id))(processCommand)

  private def processCommand(cmd: CentreCommand): Future[Result] = {
    val future = centresService.processCommand(cmd)
    validationReply(future)
  }

}
