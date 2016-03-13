 package org.biobank.service.study

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.ImplementedBy
import javax.inject._
import org.biobank.domain.study._
import org.biobank.domain.{ DomainValidation, DomainError }
import org.biobank.dto._
import org.biobank.dto.{ CollectionDto, ProcessingDto }
import org.biobank.infrastructure._
import org.biobank.infrastructure.command.StudyCommands._
import org.biobank.infrastructure.event.CollectionEventTypeEvents._
import org.biobank.infrastructure.event.StudyEvents._
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent._
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz.Validation.FlatMap._

trait StudyServiceErrorMessages {

  val StudyNotFound = "study with id not found"

  val StudyAlreadyExists = "study already exists"

  val ErrMsgNameExists = "study with name already exists"

}

@ImplementedBy(classOf[StudiesServiceImpl])
trait StudiesService {

  def getStudyCount(): Int

  def getCountsByStatus(): StudyCountsByStatus

  def getStudies[T <: Study](filter:   String,
                             status:   String,
                             sortFunc: (Study, Study) => Boolean,
                             order:    SortOrder)
      : DomainValidation[Seq[Study]]

  def getStudyNames(filter: String, order: SortOrder): Seq[StudyNameDto]

  def getStudy(id: String): DomainValidation[Study]

  def specimenGroupWithId(studyId: String, specimenGroupId: String)
      : DomainValidation[SpecimenGroup]

  def specimenGroupsForStudy(studyId: String): DomainValidation[Set[SpecimenGroup]]

  def specimenGroupsInUse(studyId: String): DomainValidation[Set[SpecimenGroupId]]

  def collectionEventTypeWithId(studyId: String,
                                collectionEventTypeId: String)
      : DomainValidation[CollectionEventType]

  def collectionEventTypesForStudy(studyId: String): DomainValidation[Set[CollectionEventType]]

  def processingTypeWithId(Id: String, processingTypeId: String)
      : DomainValidation[ProcessingType]

  def processingTypesForStudy(studyId: String): DomainValidation[Set[ProcessingType]]

  def specimenLinkTypeWithId(processingTypeId: String, specimenLinkTypeId: String)
      : DomainValidation[SpecimenLinkType]

  def specimenLinkTypesForProcessingType(processingTypeId: String)
      : DomainValidation[Set[SpecimenLinkType]]

  def getCollectionDto(studyId: String): DomainValidation[CollectionDto]

  def getProcessingDto(studyId: String): DomainValidation[ProcessingDto]

  def processCommand(cmd: StudyCommand): Future[DomainValidation[Study]]

  def processCollectionEventTypeCommand(cmd: StudyCommand)
      : Future[DomainValidation[CollectionEventType]]

  def processRemoveCollectionEventTypeCommand(cmd: RemoveCollectionEventTypeCmd)
      : Future[DomainValidation[Boolean]]
}

/**
  * This is the Study Aggregate Application Service.
  *
  * Handles the commands to configure studies. the commands are forwarded to the Study Aggregate
  * Processor.
  *
  * @param studiesProcessor
  *
 */
class StudiesServiceImpl @javax.inject.Inject() (
  @Named("studiesProcessor") val processor: ActorRef,
  val studyRepository:                      StudyRepository,
  val processingTypeRepository:             ProcessingTypeRepository,
  val specimenGroupRepository:              SpecimenGroupRepository,
  val collectionEventTypeRepository:        CollectionEventTypeRepository,
  val specimenLinkTypeRepository:           SpecimenLinkTypeRepository)
    extends StudiesService
    with StudyServiceErrorMessages {
  import org.biobank.CommonValidations._

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout: Timeout = 5.seconds

  def getStudyCount(): Int = {
    studyRepository.getValues.size
  }

  def getCountsByStatus(): StudyCountsByStatus = {
    // FIXME should be replaced by DTO query to the database
    val studies = studyRepository.getValues
    StudyCountsByStatus(
      total         = studies.size,
      disabledCount = studies.collect { case s: DisabledStudy => s }.size,
      enabledCount  = studies.collect { case s: EnabledStudy => s }.size,
      retiredCount  = studies.collect { case s: RetiredStudy => s }.size
    )
  }

  def getStudies[T <: Study](filter:   String,
                             status:   String,
                             sortFunc: (Study, Study) => Boolean,
                             order:    SortOrder)
      : DomainValidation[Seq[Study]] = {
    val allStudies = studyRepository.getValues

    val studiesFilteredByName = if (!filter.isEmpty) {
      val filterLowerCase = filter.toLowerCase
      allStudies.filter { study => study.name.toLowerCase.contains(filterLowerCase) }
    } else {
      allStudies
    }

    val studiesFilteredByStatus = status match {
      case "all" =>
        studiesFilteredByName.success
      case "DisabledStudy" =>
        studiesFilteredByName.collect { case s: DisabledStudy => s }.success
      case "EnabledStudy" =>
        studiesFilteredByName.collect { case s: EnabledStudy => s }.success
      case "RetiredStudy" =>
        studiesFilteredByName.collect { case s: RetiredStudy => s }.success
      case _ => InvalidStatus(status).failureNel
    }

    studiesFilteredByStatus.map { studies =>
      val result = studies.toSeq.sortWith(sortFunc)

      if (order == AscendingOrder) {
        result
      } else {
        result.reverse
      }
    }
  }

  def getStudyNames(filter: String, order: SortOrder): Seq[StudyNameDto] = {
    val studies = studyRepository.getValues

    val filteredStudies = if (filter.isEmpty) {
      studies
    } else {
      studies.filter { s => s.name.contains(filter) }
    }

    val orderedStudies = filteredStudies.toSeq
    val result = orderedStudies.map { s =>
      StudyNameDto(s.id.id, s.name, s.getClass.getSimpleName)
    } sortWith(StudyNameDto.compareByName)

    if (order == AscendingOrder) {
      result
    } else {
      result.reverse
    }
  }

  def getStudy(id: String) : DomainValidation[Study] = {
    studyRepository.getByKey(StudyId(id))
  }

  def specimenGroupWithId(studyId: String, specimenGroupId: String)
      : DomainValidation[SpecimenGroup] = {
    validStudyId(studyId) { study =>
      specimenGroupRepository.withId(study.id, SpecimenGroupId(specimenGroupId))
    }
  }

  def specimenGroupsForStudy(studyId: String) : DomainValidation[Set[SpecimenGroup]] = {
    validStudyId(studyId) { study =>
      specimenGroupRepository.allForStudy(study.id).successNel
    }
  }

  def specimenGroupsInUse(studyId: String): DomainValidation[Set[SpecimenGroupId]] = {
    ???
    // validStudyId(studyId) { study =>
    //     val cetSpecimenGroupIds = for {
    //       ceventType <- collectionEventTypeRepository.allForStudy(study.id)
    //       sgItem     <- ceventType.specimenGroupData
    //     } yield SpecimenGroupId(sgItem.specimenGroupId)

    //     val sltSpecimenGroupIds = for {
    //       processingType   <- processingTypeRepository.allForStudy(study.id)
    //       specimenLinkType <- specimenLinkTypeRepository.allForProcessingType(processingType.id)
    //       sgId             <- Set(specimenLinkType.inputGroupId, specimenLinkType.outputGroupId)
    //     } yield sgId

    //     (cetSpecimenGroupIds ++ sltSpecimenGroupIds).success
    //   }
  }

  def collectionEventTypeWithId(studyId: String, collectionEventTypeId: String)
      : DomainValidation[CollectionEventType] = {
    validStudyId(studyId) { study =>
      collectionEventTypeRepository.withId(study.id, CollectionEventTypeId(collectionEventTypeId))
    }
  }

  def collectionEventTypesForStudy(studyId: String)
      : DomainValidation[Set[CollectionEventType]] = {
    validStudyId(studyId) { study =>
      collectionEventTypeRepository.allForStudy(study.id).success
    }
  }

  def processingTypeWithId(studyId: String, processingTypeId: String)
      : DomainValidation[ProcessingType] = {
    validStudyId(studyId) { study =>
      processingTypeRepository.withId(study.id, ProcessingTypeId(processingTypeId))
    }
  }

  def processingTypesForStudy(studyId: String)
      : DomainValidation[Set[ProcessingType]] = {
    validStudyId(studyId) { study =>
      processingTypeRepository.allForStudy(study.id).success
    }
  }

  def specimenLinkTypeWithId(processingTypeId: String, specimenLinkTypeId: String)
      : DomainValidation[SpecimenLinkType] = {
    validProcessingTypeId(processingTypeId) { processingType =>
      specimenLinkTypeRepository.withId(processingType.id, SpecimenLinkTypeId(specimenLinkTypeId))
    }
  }

  def specimenLinkTypesForProcessingType(processingTypeId: String)
      : DomainValidation[Set[SpecimenLinkType]] = {
    validProcessingTypeId(processingTypeId) { processingType =>
      specimenLinkTypeRepository.allForProcessingType(processingType.id).success
    }
  }

  def getCollectionDto(studyId: String): DomainValidation[CollectionDto] = {
    "deprectated: annot type refactor".failureNel
  }

  def getProcessingDto(studyId: String): DomainValidation[ProcessingDto] = {
    "deprectated: annot type refactor".failureNel
  }

  private def validStudyId[T](studyId: String)(fn: Study => DomainValidation[T])
      : DomainValidation[T] = {
    for {
      study <- studyRepository.getByKey(StudyId(studyId))
      result <- fn(study)
    } yield result
  }

  private def validCollectionEventTypeId[T](id: String)(fn: CollectionEventType => DomainValidation[T])
      : DomainValidation[T] = {
    for {
      cet <- collectionEventTypeRepository.getByKey(CollectionEventTypeId(id))
      result <- fn(cet)
    } yield result
  }

  private def validSpecimenLinkTypeId[T](id: String)(fn: SpecimenLinkType => DomainValidation[T])
      : DomainValidation[T] = {
    for {
      slt <- specimenLinkTypeRepository.getByKey(SpecimenLinkTypeId(id))
      result <- fn(slt)
    } yield result
  }

  private def validProcessingTypeId[T](processingTypeId: String)(fn: ProcessingType => DomainValidation[T])
      : DomainValidation[T] = {
    for {
      pt <- processingTypeRepository.getByKey(ProcessingTypeId(processingTypeId))
      study <- studyRepository.getByKey(pt.studyId)
      result <- fn(pt)
    } yield result
  }

  def processCommand(cmd: StudyCommand): Future[DomainValidation[Study]] =
    ask(processor, cmd).mapTo[DomainValidation[StudyEvent]].map { validation =>
      for {
        event <- validation
        study <- studyRepository.getByKey(StudyId(event.id))
      } yield study
    }

  def processCollectionEventTypeCommand(cmd: StudyCommand)
      : Future[DomainValidation[CollectionEventType]] = {
    cmd match {
      case c: RemoveCollectionEventTypeCmd =>
        Future.successful(DomainError(s"invalid service call: $cmd").failureNel[CollectionEventType])
      case _ =>
        ask(processor, cmd).mapTo[DomainValidation[CollectionEventTypeEvent]].map { validation =>
          for {
            event  <- validation
            result <- collectionEventTypeRepository.getByKey(CollectionEventTypeId(event.id))
          } yield result
        }
    }
  }

  def processRemoveCollectionEventTypeCommand(cmd: RemoveCollectionEventTypeCmd)
      : Future[DomainValidation[Boolean]] = {
    ask(processor, cmd).mapTo[DomainValidation[CollectionEventTypeEvent]].map { validation =>
      for {
        event  <- validation
        result <- true.success
      } yield result
    }
  }

  private def replyWithSpecimenGroup(future: Future[DomainValidation[StudyEventOld]])
      : Future[DomainValidation[SpecimenGroup]] = {
    future map { validation =>
      for {
        event <- validation
        sg <- {
          val specimenGroupId = if (event.eventType.isSpecimenGroupAdded) {
            event.getSpecimenGroupAdded.getSpecimenGroupId
          } else {
            event.getSpecimenGroupUpdated.getSpecimenGroupId
          }
          specimenGroupRepository.getByKey(SpecimenGroupId(specimenGroupId))
        }
      } yield sg
    }
  }

  private def replyWithProcessingType(future: Future[DomainValidation[StudyEventOld]])
      : Future[DomainValidation[ProcessingType]] = {
    future map { validation =>
      for {
        event <- validation
        pt <- {
          val ptId = if (event.eventType.isProcessingTypeAdded) {
            event.getProcessingTypeAdded.getProcessingTypeId
          } else {
            event.getProcessingTypeUpdated.getProcessingTypeId
          }
          processingTypeRepository.getByKey(ProcessingTypeId(ptId))
        }
      } yield pt
    }
  }

  private def replyWithSpecimenLinkType(future: Future[DomainValidation[StudyEventOld]])
      : Future[DomainValidation[SpecimenLinkType]] = {
    future map { validation =>
      for {
        event <- validation
        slt <- {
          val sltId = if (event.eventType.isSpecimenLinkTypeAdded) {
            event.getSpecimenLinkTypeAdded.getSpecimenLinkTypeId
          } else {
            event.getSpecimenLinkTypeUpdated.getSpecimenLinkTypeId
          }
          specimenLinkTypeRepository.getByKey(SpecimenLinkTypeId(sltId))
        }
      } yield slt
    }
  }

}
