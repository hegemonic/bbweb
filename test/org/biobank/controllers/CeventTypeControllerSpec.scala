package org.biobank.controllers

import org.biobank.domain.study.{ Study, SpecimenGroup }
import org.biobank.fixture.ControllerFixture
import org.biobank.service.json.JsonHelper._

import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.api.libs.json._
import org.scalatest.Tag
import org.slf4j.LoggerFactory
import org.joda.time.DateTime

class CeventTypeControllerSpec extends ControllerFixture {

  val log = LoggerFactory.getLogger(this.getClass)

  def addOnNonDisabledStudy(
    appRepositories: AppRepositories,
    study: Study) {
    appRepositories.studyRepository.put(study)

    val sg = factory.createSpecimenGroup
    appRepositories.specimenGroupRepository.put(sg)

    val annotType = factory.createCollectionEventAnnotationType
    appRepositories.collectionEventAnnotationTypeRepository.put(annotType)

    val cet = factory.createCollectionEventType.copy(
      specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
      annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))

    val cmdJson = Json.obj(
      "type"                 -> "AddCollectionEventTypeCmd",
      "studyId"              -> cet.studyId.id,
      "name"                 -> cet.name,
      "description"          -> cet.description,
      "recurring"            -> cet.recurring,
      "specimenGroupData"    -> Json.arr(
        Json.obj(
          "specimenGroupId"  -> cet.specimenGroupData(0).specimenGroupId,
          "maxCount"         -> cet.specimenGroupData(0).maxCount,
          "amount"           -> Some(cet.specimenGroupData(0).amount)
        )),
      "annotationTypeData"   -> Json.arr(
        Json.obj(
          "annotationTypeId" -> cet.annotationTypeData(0).annotationTypeId,
          "required"         -> cet.annotationTypeData(0).required
        ))
    )

    val json = makeJsonRequest(POST, "/studies/cetypes", BAD_REQUEST, cmdJson)

    (json \ "message").as[String] should include ("study is not disabled")
  }

  def updateOnNonDisabledStudy(
    appRepositories: AppRepositories,
    study: Study) {
    appRepositories.studyRepository.put(study)

    val sg = factory.createSpecimenGroup
    appRepositories.specimenGroupRepository.put(sg)

    val annotType = factory.createCollectionEventAnnotationType
    appRepositories.collectionEventAnnotationTypeRepository.put(annotType)

    val cet = factory.createCollectionEventType
    appRepositories.collectionEventTypeRepository.put(cet)

    val cet2 = factory.createCollectionEventType.copy(
      specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
      annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))

    val cmdJson = Json.obj(
      "type"                 -> "UpdateCollectionEventTypeCmd",
      "studyId"              -> cet.studyId.id,
      "id"                   -> cet.id.id,
      "expectedVersion"      -> Some(cet.version),
      "name"                 -> cet2.name,
      "description"          -> cet2.description,
      "recurring"            -> cet2.recurring,
      "specimenGroupData"    -> Json.arr(
        Json.obj(
          "specimenGroupId"  -> cet2.specimenGroupData(0).specimenGroupId,
          "maxCount"         -> cet2.specimenGroupData(0).maxCount,
          "amount"           -> Some(cet2.specimenGroupData(0).amount)
        )),
      "annotationTypeData"   -> Json.arr(
        Json.obj(
          "annotationTypeId" -> cet2.annotationTypeData(0).annotationTypeId,
          "required"         -> cet2.annotationTypeData(0).required
        ))
    )

    val json = makeJsonRequest(PUT, s"/studies/cetypes/${cet.id.id}", BAD_REQUEST, cmdJson)

    (json \ "message").as[String] should include ("study is not disabled")
  }

  def removeOnNonDisabledStudy(
    appRepositories: AppRepositories,
    study: Study) {
    appRepositories.studyRepository.put(study)

    val sg = factory.createSpecimenGroup
    appRepositories.specimenGroupRepository.put(sg)

    val annotType = factory.createCollectionEventAnnotationType
    appRepositories.collectionEventAnnotationTypeRepository.put(annotType)

    val cet = factory.createCollectionEventType.copy(
      specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
      annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))
    appRepositories.collectionEventTypeRepository.put(cet)

    val cmdJson = Json.obj(
      "type"            -> "RemoveCollectionEventTypeCmd",
      "studyId"         -> cet.studyId.id,
      "id"              -> cet.id.id,
      "expectedVersion" -> Some(cet.version)
    )

    val json = makeJsonRequest(DELETE, s"/studies/cetypes/${cet.id.id}", BAD_REQUEST, cmdJson)

    (json \ "message").as[String] should include ("study is not disabled")
  }

  describe("Collection Event Type REST API") {
    describe("GET /studies/cetypes") {
      it("should list none") {
        running(fakeApplication) {
          val appRepositories = new AppRepositories

          val study = factory.createDisabledStudy
          appRepositories.studyRepository.put(study)

          val idJson = Json.obj("id" -> study.id.id)
          val json = makeJsonRequest(GET, "/studies/cetypes", json = idJson)
          val jsonList = json.as[List[JsObject]]
          jsonList should have size 0
        }
      }
    }

    describe("GET /studies/cetypes") {
      it("should list a single collection event type") {
        running(fakeApplication) {
          val appRepositories = new AppRepositories

          val study = factory.createDisabledStudy
          appRepositories.studyRepository.put(study)

          val cet = factory.createCollectionEventType
          appRepositories.collectionEventTypeRepository.put(cet)

          val idJson = Json.obj("id" -> study.id.id)
          val json = makeJsonRequest(GET, "/studies/cetypes", json = idJson)
          val jsonList = json.as[List[JsObject]]
          jsonList should have size 1
          compareObj(jsonList(0), cet)
        }
      }
    }

    describe("GET /studies/cetypes") {
      it("should list multiple collection event types") {
        running(fakeApplication) {
          val appRepositories = new AppRepositories

          val study = factory.createDisabledStudy
          appRepositories.studyRepository.put(study)

          val cet1 = factory.createCollectionEventType.copy(
            specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
            annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))

          val cet2 = factory.createCollectionEventType.copy(
            specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
            annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))

          val cetypes = List(cet1, cet2)
          cetypes map { cet => appRepositories.collectionEventTypeRepository.put(cet) }

          val idJson = Json.obj("id" -> study.id.id)
          val json = makeJsonRequest(GET, "/studies/cetypes", json = idJson)
          val jsonList = json.as[List[JsObject]]

          jsonList should have size cetypes.size
            (jsonList zip cetypes).map { item => compareObj(item._1, item._2) }
          ()
        }
      }
    }

    describe("POST /studies/cetypes") {
      it("should add a collection event type") {
        running(fakeApplication) {
          val appRepositories = new AppRepositories

          val study = factory.createDisabledStudy
          appRepositories.studyRepository.put(study)

          val sg = factory.createSpecimenGroup
          appRepositories.specimenGroupRepository.put(sg)

          val annotType = factory.createCollectionEventAnnotationType
          appRepositories.collectionEventAnnotationTypeRepository.put(annotType)

          val cet = factory.createCollectionEventType.copy(
            specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
            annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))

          val cmdJson = Json.obj(
            "type"                 -> "AddCollectionEventTypeCmd",
            "studyId"              -> cet.studyId.id,
            "name"                 -> cet.name,
            "description"          -> cet.description,
            "recurring"            -> cet.recurring,
            "specimenGroupData"    -> Json.arr(
              Json.obj(
                "specimenGroupId"  -> cet.specimenGroupData(0).specimenGroupId,
                "maxCount"         -> cet.specimenGroupData(0).maxCount,
                "amount"           -> Some(cet.specimenGroupData(0).amount)
              )),
            "annotationTypeData"   -> Json.arr(
              Json.obj(
                "annotationTypeId" -> cet.annotationTypeData(0).annotationTypeId,
                "required"         -> cet.annotationTypeData(0).required
              ))
          )

          val json = makeJsonRequest(POST, "/studies/cetypes", json = cmdJson)

          (json \ "message").as[String] should include ("collection event type added")
        }
      }
    }

    describe("POST /studies/cetypes") {
      it("should not add a collection event type to an enabled study") {
        running(fakeApplication) {
          addOnNonDisabledStudy(
            new AppRepositories,
            factory.createDisabledStudy.enable(Some(0), DateTime.now, 1, 1) | fail)
        }
      }
    }

    describe("POST /studies/cetypes") {
      it("should not add a collection event type to an retired study") {
        running(fakeApplication) {
          addOnNonDisabledStudy(
            new AppRepositories,
            factory.createDisabledStudy.retire(Some(0), DateTime.now) | fail)
        }
      }
    }

    describe("PUT /studies/cetypes") {
      it("should update a collection event type") {
        running(fakeApplication) {
          val appRepositories = new AppRepositories

          val study = factory.createDisabledStudy
          appRepositories.studyRepository.put(study)

          val sg = factory.createSpecimenGroup
          appRepositories.specimenGroupRepository.put(sg)

          val annotType = factory.createCollectionEventAnnotationType
          appRepositories.collectionEventAnnotationTypeRepository.put(annotType)

          val cet = factory.createCollectionEventType
          appRepositories.collectionEventTypeRepository.put(cet)

          val cet2 = factory.createCollectionEventType.copy(
            specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
            annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))

          val cmdJson = Json.obj(
            "type"                 -> "UpdateCollectionEventTypeCmd",
            "studyId"              -> cet.studyId.id,
            "id"                   -> cet.id.id,
            "expectedVersion"      -> Some(cet.version),
            "name"                 -> cet2.name,
            "description"          -> cet2.description,
            "recurring"            -> cet2.recurring,
            "specimenGroupData"    -> Json.arr(
              Json.obj(
                "specimenGroupId"  -> cet2.specimenGroupData(0).specimenGroupId,
                "maxCount"         -> cet2.specimenGroupData(0).maxCount,
                "amount"           -> Some(cet2.specimenGroupData(0).amount)
              )),
            "annotationTypeData"   -> Json.arr(
              Json.obj(
                "annotationTypeId" -> cet2.annotationTypeData(0).annotationTypeId,
                "required"         -> cet2.annotationTypeData(0).required
              ))
          )

          val json = makeJsonRequest(PUT, s"/studies/cetypes/${cet.id.id}", json = cmdJson)

          (json \ "message").as[String] should include ("collection event type updated")
        }
      }
    }

    describe("PUT /studies/cetypes") {
      it("should not update a collection event type on an enabled study") {
        running(fakeApplication) {
          updateOnNonDisabledStudy(
            new AppRepositories,
            factory.createDisabledStudy.enable(Some(0), DateTime.now, 1, 1) | fail)
        }
      }
    }

    describe("PUT /studies/cetypes") {
      it("should not update a collection event type on an retired study") {
        running(fakeApplication) {
          updateOnNonDisabledStudy(
            new AppRepositories,
            factory.createDisabledStudy.retire(Some(0), DateTime.now) | fail)
        }
      }
    }

    describe("DELETE /studies/cetypes") {
      it("should remove a collection event type") {
        running(fakeApplication) {
          val appRepositories = new AppRepositories

          val study = factory.createDisabledStudy
          appRepositories.studyRepository.put(study)

          val sg = factory.createSpecimenGroup
          appRepositories.specimenGroupRepository.put(sg)

          val annotType = factory.createCollectionEventAnnotationType
          appRepositories.collectionEventAnnotationTypeRepository.put(annotType)

          val cet = factory.createCollectionEventType.copy(
            specimenGroupData = List(factory.createCollectionEventTypeSpecimenGroupData),
            annotationTypeData = List(factory.createCollectionEventTypeAnnotationTypeData))
          appRepositories.collectionEventTypeRepository.put(cet)

          val cmdJson = Json.obj(
            "type"            -> "RemoveCollectionEventTypeCmd",
            "studyId"         -> cet.studyId.id,
            "id"              -> cet.id.id,
            "expectedVersion" -> Some(cet.version)
          )

          val json = makeJsonRequest(DELETE, s"/studies/cetypes/${cet.id.id}", json = cmdJson)

          (json \ "message").as[String] should include ("collection event type removed")
        }
      }
    }

    describe("DELETE /studies/cetypes") {
      it("should not remove a collection event type on an enabled study") {
        running(fakeApplication) {
          removeOnNonDisabledStudy(
            new AppRepositories,
            factory.createDisabledStudy.enable(Some(0), DateTime.now, 1, 1) | fail)
        }
      }
    }

    describe("DELETE /studies/cetypes") {
      it("should not remove a collection event type on an retired study") {
        running(fakeApplication) {
          removeOnNonDisabledStudy(
            new AppRepositories,
            factory.createDisabledStudy.retire(Some(0), DateTime.now) | fail)
        }
      }
    }
  }

}