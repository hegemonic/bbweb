package service

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.stm.Ref

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import domain._
import domain.study._

import org.eligosource.eventsourced.core._

import scalaz._
import Scalaz._

import scala.language.postfixOps

class StudyService(studiesRef: Ref[Map[String, Study]],
  studyProcessor: ActorRef)(implicit system: ActorSystem) {
  import system.dispatcher

  //
  // Consistent reads
  //

  def getStudiesMap = studiesRef.single.get

  //
  // Updates
  //

  implicit val timeout = Timeout(5 seconds)

  def addStudy(name: String, description: String): Future[DomainValidation[DisabledStudy]] =
    studyProcessor ? Message(AddStudy(name, description)) map (_.asInstanceOf[DomainValidation[DisabledStudy]])
}

// -------------------------------------------------------------------------------------------------------------
//  InvoiceProcessor is single writer to studiesRef, so we can have reads and writes in separate transactions
// -------------------------------------------------------------------------------------------------------------
class StudyProcessor(studiesRef: Ref[Map[String, Study]]) extends Actor { this: Emitter =>

  def receive = {
    case AddStudy(name, description) =>
      process(addStudy(name, description)) { study =>
        emitter("listeners") sendEvent StudyAdded(name, description)
      }
  }

  def process(validation: DomainValidation[Study])(onSuccess: Study => Unit) = {
    validation.foreach { study =>
      updateStudies(study)
      onSuccess(study)
    }
    sender ! validation
  }

  def addStudy(name: String, description: String): DomainValidation[DisabledStudy] = {
    readStudies.find(s => s._2.name.equals(name)) match {
      case Some(study) => DomainError("study with name already exists: %s" format name).fail
      case None => Study.add(Study.nextIdentity, name, description)
    }
  }

  private def updateStudies(study: Study) =
    studiesRef.single.transform(studies => studies + (study.id.toString -> study))

  private def readStudies =
    studiesRef.single.get
}

object StudyProcessor {
  private[service] def notDisabledError(name: String) =
    DomainError("study is not disabled: %s" format name)

  private[service] def notEnabledError(name: String) =
    DomainError("study is not enabled: %s" format name)
}