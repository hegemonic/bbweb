package service

import infrastructure.{
  DomainValidation,
  DomainError,
  ProcessorMsg,
  ReadRepository,
  ReadWriteRepository,
  ServiceMsg,
  StudyProcessorMsg
}
import infrastructure.commands._
import infrastructure.events._
import domain.{
  AnnotationTypeId,
  ConcurrencySafeEntity,
  Entity,
  UserId
}
import domain.AnatomicalSourceType._
import domain.PreservationType._
import domain.PreservationTemperatureType._
import domain.SpecimenType._
import domain.AnnotationValueType._
import domain.study._
import service.study.{
  CollectionEventTypeService,
  SpecimenGroupService,
  StudyAnnotationTypeService
}
import service.study.SpecimenGroupService
import Study._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging
import akka.actor.ActorLogging
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.stm.Ref
import scala.language.postfixOps
import org.eligosource.eventsourced.core._

import scalaz._
import Scalaz._

case class StudyMessage(cmd: Any, userId: UserId, time: Long, listeners: MessageEmitter)

/**
 * This is the Study Aggregate Processor.
 *
 * It handles the commands to configure studies.
 *
 * @param studyRepository The repository for study entities.
 * @param specimenGroupRepository The repository for specimen group entities.
 * @param cetRepo The repository for Container Event Type entities.
 * @param annotationTypeRepo The repository for Collection Event Annotation Type entities.
 * @param sg2cetRepo The value object repository that associates a specimen group to a
 *         collection event type.
 * @param at2cetRepo The value object repository that associates a collection event annotation
 *         type to a collection event type.
 */
class StudyProcessor(
  studyRepository: StudyReadWriteRepository,
  specimenGroupRepository: SpecimenGroupReadWriteRepository,
  cetRepo: CollectionEventTypeReadWriteRepository,
  ceventAnnotationTypeRepo: CollectionEventAnnotationTypeReadWriteRepository,
  sg2cetRepo: SpecimenGroupCollectionEventTypeReadWriteRepository,
  at2cetRepo: CollectionEventTypeAnnotationTypeReadWriteRepository)
  extends Processor with ActorLogging { this: Emitter =>

  /**
   * The domain service that handles specimen group commands.
   */
  val specimenGroupService = new SpecimenGroupService(
    studyRepository, specimenGroupRepository)

  /**
   * The domain service that handles collection event type commands.
   */
  val collectionEventTypeService = new CollectionEventTypeService(
    studyRepository, cetRepo, specimenGroupRepository, ceventAnnotationTypeRepo,
    sg2cetRepo, at2cetRepo)

  val annotationTypeService = new StudyAnnotationTypeService(
    ceventAnnotationTypeRepo)

  def receive = {
    case serviceMsg: ServiceMsg =>
      serviceMsg.cmd match {
        case cmd: AddStudyCmd =>
          process(addStudy(cmd, emitter("listeners"), serviceMsg.id))

        case cmd: UpdateStudyCmd =>
          process(updateStudy(cmd, emitter("listeners")))

        case cmd: EnableStudyCmd =>
          process(enableStudy(cmd, emitter("listeners")))

        case cmd: DisableStudyCmd =>
          process(disableStudy(cmd, emitter("listeners")))

        case cmd: SpecimenGroupCommand =>
          processEntityMsg(cmd, cmd.studyId, serviceMsg.id, specimenGroupService.process)

        case cmd: CollectionEventTypeCommand =>
          processEntityMsg(cmd, cmd.studyId, serviceMsg.id, collectionEventTypeService.process)

        case cmd: StudyAnnotationTypeCommand =>
          processEntityMsg(cmd, cmd.studyId, serviceMsg.id, annotationTypeService.process)

        case other => // must be for another command handler
      }

    case _ =>
      throw new Error("invalid message received: ")
  }

  private def processEntityMsg[T](
    cmd: StudyCommand,
    studyId: String,
    id: Option[String],
    processFunc: StudyProcessorMsg => DomainValidation[T]) = {
    val updatedItem = for {
      study <- validateStudy(new StudyId(studyId))
      item <- processFunc(StudyProcessorMsg(cmd, study, emitter("listeners"), id))
    } yield item
    process(updatedItem)
  }

  override protected def process[T](validation: DomainValidation[T]) = {
    validation match {
      case Success(domainObject) =>
      // update the addedBy and updatedBy fields on the study aggregate
      case Failure(x) =>
      // do nothing
    }
    super.process(validation)
  }

  private def validateStudy(studyId: StudyId): DomainValidation[DisabledStudy] =
    studyRepository.getByKey(studyId) match {
      case Failure(msglist) => noSuchStudy(studyId).fail
      case Success(study) => study match {
        case _: EnabledStudy => notDisabledError(study.name).fail
        case dstudy: DisabledStudy => dstudy.success
      }
    }

  def logMethod(methodName: String, cmd: Any, study: DomainValidation[Study]) {
    if (log.isDebugEnabled) {
      log.debug("%s: %s".format(methodName, cmd))
      study match {
        case Success(item) =>
          log.debug("%s: %s".format(methodName, item))
        case Failure(msglist) =>
          log.debug("%s: { msg: %s }".format(methodName, msglist.head))
      }
    }
  }

  private def addStudy(
    id: StudyId,
    version: Long,
    name: String,
    description: Option[String]): DomainValidation[DisabledStudy] = {

    def nameCheck(id: StudyId, name: String): DomainValidation[Boolean] = {
      studyRepository.getValues.exists {
        item => !item.id.equals(id) && item.name.equals(name)
      } match {
        case true => DomainError("study with name already exists: %s" format name).fail
        case false => true.success
      }
    }

    for {
      nameCheck <- nameCheck(id, name)
      newItem <- DisabledStudy(id, version, name, description).success
    } yield newItem
  }

  private def addStudy(
    cmd: AddStudyCmd,
    listeners: MessageEmitter,
    id: Option[String]): DomainValidation[DisabledStudy] = {

    def addItem(item: Study): Study = {
      studyRepository.updateMap(item)
      listeners sendEvent StudyAddedEvent(item.id, item.name, item.description)
      item
    }

    val item = for {
      studyId <- id.toSuccess(DomainError("study ID is missing"))
      newItem <- addStudy(new StudyId(studyId), version = 0L, cmd.name, cmd.description)
      addedItem <- addItem(newItem).success
    } yield newItem

    logMethod("addStudy", cmd, item)
    item
  }

  private def updateStudy(
    cmd: UpdateStudyCmd,
    listeners: MessageEmitter): DomainValidation[DisabledStudy] = {
    val studyId = new StudyId(cmd.id)

    def updateItem(item: Study) = {
      studyRepository.updateMap(item)
      listeners sendEvent StudyUpdatedEvent(item.id, item.name, item.description)
    }

    val item = for {
      prevItem <- studyRepository.getByKey(new StudyId(cmd.id))
      validVersion <- prevItem.requireVersion(cmd.expectedVersion)
      newItem <- addStudy(prevItem.id, prevItem.version + 1, cmd.name, cmd.description)
      updatedItem <- updateItem(newItem).success
    } yield newItem

    logMethod("updateStudy", cmd, item)
    item
  }

  private def enableStudy(cmd: EnableStudyCmd, listeners: MessageEmitter): DomainValidation[EnabledStudy] = {
    def enableStudy(study: Study): DomainValidation[EnabledStudy] = {
      study match {
        case es: EnabledStudy =>
          DomainError("study is already enabled: {id: %s}".format(es.id)).fail
        case ds: DisabledStudy =>
          val specimenGroupCount = specimenGroupRepository.getValues.find(
            s => s.studyId.equals(ds.id)).size
          val collectionEventTypecount = cetRepo.getValues.find(
            s => s.studyId.equals(ds.id)).size
          ds.enable(specimenGroupCount, collectionEventTypecount)
      }
    }

    def updateItem(item: Study) = {
      studyRepository.updateMap(item)
      listeners sendEvent StudyEnabledEvent(item.id)
    }

    val item = for {
      prevItem <- studyRepository.getByKey(new StudyId(cmd.id))
      newItem <- enableStudy(prevItem)
      updatedItem <- updateItem(newItem).success
    } yield newItem

    logMethod("enableStudy", cmd, item)
    item
  }

  private def disableStudy(cmd: DisableStudyCmd, listeners: MessageEmitter): DomainValidation[DisabledStudy] = {
    def disableStudy(study: Study): DomainValidation[DisabledStudy] = {
      study match {
        case ds: DisabledStudy =>
          DomainError("study is already disnabled: {id: %s}".format(ds.id)).fail
        case es: EnabledStudy =>
          es.disable
      }
    }

    def updateItem(item: Study) = {
      studyRepository.updateMap(item)
      listeners sendEvent StudyDisabledEvent(item.id)
    }

    val item = for {
      prevItem <- studyRepository.getByKey(new StudyId(cmd.id))
      newItem <- disableStudy(prevItem)
      updatedItem <- updateItem(newItem).success
    } yield newItem

    logMethod("disableStudy", cmd, item)
    item
  }
}
