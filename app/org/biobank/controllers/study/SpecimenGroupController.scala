package org.biobank.controllers.study

import org.biobank.controllers._
import org.biobank.service._
import org.biobank.service.json.Study._
import org.biobank.service.json.SpecimenGroup._
import org.biobank.infrastructure.command.StudyCommands._
import org.biobank.domain.study._
import org.biobank.domain.AnatomicalSourceType._
import org.biobank.domain.PreservationType._
import org.biobank.domain.PreservationTemperatureType._
import org.biobank.domain.SpecimenType._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.{ Logger, Play }
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Results._

import scalaz._
import scalaz.Scalaz._

/**
 * Handles all operations user can perform on a Specimen Group.
 */
object SpecimenGroupController extends BbwebController {

  private def studyService = Play.current.plugin[BbwebPlugin].map(_.studyService).getOrElse {
    sys.error("Bbweb plugin is not registered")
  }

  def list = AuthAction(parse.json) { token => userId => implicit request =>
    val idResult = request.body.validate[StudyId]
    idResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
      },
      studyId => {
        Logger.info(s"list: $studyId")
        val json = Json.toJson(studyService.specimenGroupsForStudy(studyId.id).toList)
        Ok(json)
      }
    )
  }

  def addSpecimenGroup = CommandAction { cmd: AddSpecimenGroupCmd => implicit userId =>
    val future = studyService.addSpecimenGroup(cmd)
    future.map { validation =>
      validation.fold(
        err   => BadRequest(Json.obj("status" ->"KO", "message" -> err.list.mkString(", "))),
        event => Ok(Json.obj("status" ->"OK", "message" -> (s"specimen group added: ${event.name}.") ))
      )
    }
  }

  def updateSpecimenGroup(id: String) = CommandAction { cmd: UpdateSpecimenGroupCmd => implicit userId =>
    val future = studyService.updateSpecimenGroup(cmd)
    future.map { validation =>
      validation.fold(
        err   => BadRequest(Json.obj("status" ->"KO", "message" -> err.list.mkString(", "))),
        event => Ok(Json.obj("status" ->"OK", "message" -> (s"specimen group updated: ${event.name}.") ))
      )
    }
  }

  def removeSpecimenGroup(id: String) = CommandAction { cmd: RemoveSpecimenGroupCmd => implicit userId =>
    val future = studyService.removeSpecimenGroup(cmd)
    future.map { validation =>
      validation.fold(
        err   => BadRequest(Json.obj("status" ->"KO", "message" -> err.list.mkString(", "))),
        event => Ok(Json.obj("status" ->"OK", "message" -> (s"specimen group removed: ${event.specimenGroupId}.") ))
      )
    }
  }

}
