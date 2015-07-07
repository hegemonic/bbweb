/**
 * Jasmine test suite
 *
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2015 Canadian BioSample Repository (CBSR)
 */
define([
  'angular',
  'angularMocks',
  'underscore',
  'faker',
  'moment',
  'biobank.testUtils',
  'biobankApp'
], function(angular, mocks, _, faker, moment, testUtils) {
  'use strict';

  describe('CollectionEvent', function() {

    var httpBackend,
        CollectionEvent,
        Participant,
        Annotation,
        AnnotationValueType,
        CollectionEventType,
        CollectionEventAnnotationType,
        bbwebConfig,
        fakeEntities,
        serverStudy,
        cetFromServer;

    beforeEach(mocks.module('biobankApp', 'biobank.test'));

    beforeEach(inject(function($httpBackend,
                               _CollectionEvent_,
                               _Annotation_,
                               _AnnotationValueType_,
                               _Participant_,
                               _CollectionEventType_,
                               _CollectionEventAnnotationType_,
                               _bbwebConfig_,
                               fakeDomainEntities,
                               extendedDomainEntities) {
      httpBackend                   = $httpBackend;
      Participant                   = _Participant_;
      CollectionEventType           = _CollectionEventType_;
      CollectionEvent               = _CollectionEvent_;
      Annotation                    = _Annotation_;
      AnnotationValueType           = _AnnotationValueType_;
      CollectionEventAnnotationType = _CollectionEventAnnotationType_;
      bbwebConfig                   = _bbwebConfig_;
      fakeEntities                  = fakeDomainEntities;

      testUtils.addCustomMatchers();

      serverStudy = fakeEntities.study();

      serverStudy.specimenGroups = _.map(_.range(2), function() {
        return fakeEntities.specimenGroup(serverStudy);
      });

      serverStudy.specimenGroupsById = _.indexBy(serverStudy.specimenGroups, 'id');

      cetFromServer = fakeEntities.collectionEventType(serverStudy);
    }));

    it('constructor with no parameters has default values', function() {
      var collectionEvent = new CollectionEvent();

      expect(collectionEvent.id).toBeNull();
      expect(collectionEvent.version).toBe(0);
      expect(collectionEvent.timeAdded).toBeNull();
      expect(collectionEvent.timeModified).toBeNull();
      expect(collectionEvent.timeCompleted).toBeNull();
      expect(collectionEvent.visitNumber).toBeNull();
    });

    it('constructor with annotation parameter has valid values', function() {
      var annotationData    = generateAnnotationTypesAndServerAnnotations(serverStudy),
          serverAnnotations = _.pluck(annotationData, 'serverAnnotation'),
          annotationTypes   = _.pluck(annotationData, 'annotationType'),
          annotationTypeDataById,
          ceventType;

      cetFromServer = fakeEntities.collectionEventType(serverStudy, {
        specimenGroups:  serverStudy.specimenGroups,
        annotationTypes: annotationTypes
      });

      ceventType = CollectionEventType.create(cetFromServer);
      annotationTypeDataById = _.indexBy(ceventType.annotationTypeData, 'annotationTypeId');

      var collectionEvent = new CollectionEvent({ annotations: serverAnnotations },
                                                ceventType,
                                                annotationTypes);


      _.each(annotationData, function (annotationItem) {
        var annotation = _.findWhere(collectionEvent.annotations,
                                     { annotationTypeId: annotationItem.annotationType.id });
        expect(annotation).toEqual(jasmine.any(Annotation));
        annotation.compareToServerEntity(annotationItem.serverAnnotation);
        expect(annotation.required)
          .toBe(annotationTypeDataById[annotationItem.annotationType.id].required);
      });
    });

    it('constructor with NO annotation type parameters has valid values', function() {
      var annotationData    = generateAnnotationTypesAndServerAnnotations(serverStudy),
          annotationTypes   = _.pluck(annotationData, 'annotationType'),
          annotationTypeDataById,
          ceventType;

      cetFromServer = fakeEntities.collectionEventType(serverStudy, {
        specimenGroups:  serverStudy.specimenGroups,
        annotationTypes: annotationTypes
      });

      ceventType = CollectionEventType.create(cetFromServer);
      annotationTypeDataById = _.indexBy(ceventType.annotationTypeData, 'annotationTypeId');

      var collectionEvent = new CollectionEvent({ }, ceventType, annotationTypes);

      expect(collectionEvent.annotations).toBeArrayOfSize(annotationData.length);
      _.each(annotationData, function (annotationItem) {
        var annotation = _.findWhere(collectionEvent.annotations,
                                     { annotationTypeId: annotationItem.annotationType.id });
        expect(annotation).toEqual(jasmine.any(Annotation));
        expect(annotation.required)
          .toBe(annotationTypeDataById[annotationItem.annotationType.id].required);
      });
    });

    it('constructor with invalid annotation parameter throws error', function() {
      var serverAnnotation = {},
          ceventType;

      var annotationType = new CollectionEventAnnotationType(
        fakeEntities.studyAnnotationType(serverStudy, { valueType: AnnotationValueType.TEXT() }));

      // put an invalid value in serverAnnotation.annotationTypeId
      _.extend(
        serverAnnotation,
        fakeEntities.annotation(fakeEntities.valueForAnnotation(annotationType), annotationType),
        { annotationTypeId: fakeEntities.stringNext() });

      cetFromServer = fakeEntities.collectionEventType(serverStudy,
                                                       { annotationTypes: [annotationType] });

      ceventType = CollectionEventType.create(cetFromServer);

      expect(function () {
        return new CollectionEvent({ annotations: [ serverAnnotation ] },
                                   ceventType,
                                   [ annotationType ]);
      }).toThrow(new Error('annotations with invalid annotation type IDs found: ' +
                           serverAnnotation.annotationTypeId));
    });

    it('fails when using constructor with invalid collection event type', function() {
      var serverCollectionEvent,
          ceventType;

      serverCollectionEvent =
        _.extend(fakeEntities.collectionEvent(),
                 {
                   collectionEventTypeId: fakeEntities.domainEntityNameNext(
                     fakeEntities.ENTITY_NAME_COLLECTION_EVENT_TYPE())
                 });
      ceventType = CollectionEventType.create(
        fakeEntities.collectionEventType(serverStudy));

      expect(function () {
        return new CollectionEvent(serverCollectionEvent, ceventType);
      }).toThrow(new Error('invalid collection event type'));
    });

    it('fails when creating from a non object', function() {
      expect(CollectionEvent.create(1))
        .toEqual(new Error('invalid object from server: has the correct keys'));
    });

    it('fails when creating from an object with invalid keys', function() {
      var serverObj = { tmp: 1 };
      expect(CollectionEvent.create(serverObj))
        .toEqual(new Error('invalid object from server: has the correct keys'));
    });

    it('fails when creating from an object with annotation with invalid keys', function() {
      var serverCollectionEvent,
          annotationType,
          ceventType;

      serverCollectionEvent = _.extend(fakeEntities.collectionEvent(),
                                       { annotations: [{ tmp: 1 }] });
      annotationType = fakeEntities.studyAnnotationType(serverStudy,
                                                        { valueType: AnnotationValueType.TEXT() });
      ceventType = CollectionEventType.create(
        fakeEntities.collectionEventType(serverStudy,
                                         { annotationTypes: [annotationType] }));

      expect(CollectionEvent.create(serverCollectionEvent))
        .toEqual(new Error('invalid annotation object from server'));
    });

    it('has valid values when creating from a server response', function() {
      var annotationData    = generateAnnotationTypesAndServerAnnotations(serverStudy),
          annotationTypes   = _.pluck(annotationData, 'annotationType'),
          serverCollectionEvent = fakeEntities.collectionEvent({annotationTypes: annotationTypes});

      var collectionEvent = CollectionEvent.create(serverCollectionEvent);
      collectionEvent.compareToServerEntity(serverCollectionEvent);
    });

    it('can retrieve a single collection event', function(done) {
      var participant = fakeEntities.participant(),
          collectionEvent = fakeEntities.collectionEvent({
            participantId: participant.id
          });

      httpBackend.whenGET(uri(participant.id) + '?ceventId=' + collectionEvent.id)
        .respond(serverReply(collectionEvent));

      CollectionEvent.get(participant.id, collectionEvent.id).then(function (reply) {
        expect(reply).toEqual(jasmine.any(CollectionEvent));
        reply.compareToServerEntity(collectionEvent);
        done();
      });
      httpBackend.flush();
    });

    it('can retrieve a single collection event by visit number', function(done) {
      var entities              = getCollectionEventEntities(true),
          participant           = entities.participant,
          serverCollectionEvent = entities.serverCollectionEvent;

      httpBackend.whenGET(uri(participant.id) + '/visitNumber/' + serverCollectionEvent.visitNumber)
        .respond(serverReply(serverCollectionEvent));

      CollectionEvent.getByVisitNumber(participant.id,
                                       serverCollectionEvent.visitNumber,
                                       entities.collectionEventType,
                                       entities.annotationTypes)
        .then(function (reply) {
          expect(reply).toEqual(jasmine.any(CollectionEvent));
          reply.compareToServerEntity(serverCollectionEvent);
          done();
        });
      httpBackend.flush();
    });

    it('can add a collectionEvent', function(done) {
      var participant = fakeEntities.participant(),
          baseCollectionEvent = fakeEntities.collectionEvent({
            participantId: participant.id
          }),
          collectionEvent     = new CollectionEvent(_.omit(baseCollectionEvent, 'id')),
          cmd                 = addCommand(collectionEvent);

      httpBackend.expectPOST(uri(participant.id), cmd).respond(201, serverReply(baseCollectionEvent));

      collectionEvent.addOrUpdate().then(function(replyCollectionEvent) {
        _.extend(collectionEvent, { id: replyCollectionEvent.id });
        collectionEvent.compareToServerEntity(replyCollectionEvent);
        done();
      });
      httpBackend.flush();
    });

    it('can add a collection event with annotations', function(done) {
      var entities = getCollectionEventEntities(true),
          cmd      = addCommand(entities.collectionEvent);

      httpBackend.expectPOST(uri(entities.collectionEvent.participantId), cmd)
        .respond(201, serverReply(entities.serverCollectionEvent));

      entities.collectionEvent.addOrUpdate().then(function(replyCollectionEvent) {
        expect(replyCollectionEvent.id).toEqual(entities.serverCollectionEvent.id);
        expect(replyCollectionEvent.version).toEqual(0);
        expect(replyCollectionEvent.participantId).toEqual(entities.participant.id);
        expect(replyCollectionEvent.timeCompleted).toEqual(entities.serverCollectionEvent.timeCompleted);
        expect(replyCollectionEvent.visitNumber).toEqual(entities.serverCollectionEvent.visitNumber);
        expect(replyCollectionEvent.annotations)
          .toBeArrayOfSize(entities.serverCollectionEvent.annotations.length);
        done();
      });
      httpBackend.flush();
    });

    it('can not add a collection event with empty required annotations', function() {
      var serverAnnotationTypes = fakeEntities.allStudyAnnotationTypes(serverStudy);

      _.each(serverAnnotationTypes, function (serverAnnotationType) {
        var annotationType, ceventType, serverCevent, collectionEvent;

        annotationType = new CollectionEventAnnotationType(serverAnnotationType);
        ceventType = CollectionEventType.create(fakeEntities.collectionEventType(
          serverStudy, { annotationTypes: [ annotationType ] } ));
        ceventType.annotationTypeData[0].required = true;
        serverCevent = _.omit(fakeEntities.collectionEvent(), 'id)');
        collectionEvent = new CollectionEvent(serverCevent, ceventType, [ annotationType ]);

        _.each(collectionEvent.annotations, function (annotation) {
          expect(annotation.getValue()).toBeFalsy();
        });

        expect(function () {
          collectionEvent.addOrUpdate();
        }).toThrow();
      });
    });

    it('can update a collectionEvent', function(done) {
      var entities        = getCollectionEventEntities(false),
          collectionEvent = entities.collectionEvent,
          cmd             = updateCommand(collectionEvent),
          reply           = replyCollectionEvent(entities.serverCollectionEvent);

      httpBackend.expectPUT(uri(collectionEvent.participantId, collectionEvent.id), cmd)
        .respond(201, serverReply(reply));

      collectionEvent.addOrUpdate().then(function(replyCollectionEvent) {
        expect(replyCollectionEvent.id).toEqual(collectionEvent.id);
        expect(replyCollectionEvent.version).toEqual(collectionEvent.version + 1);
        expect(replyCollectionEvent.participantId).toEqual(collectionEvent.participantId);
        expect(replyCollectionEvent.timeCompleted).toEqual(collectionEvent.timeCompleted);
        expect(replyCollectionEvent.visitNumber).toEqual(collectionEvent.visitNumber);
        expect(replyCollectionEvent.annotations)
          .toBeArrayOfSize(collectionEvent.annotations.length);
        expect(replyCollectionEvent.annotations)
          .toContainAll(collectionEvent.annotations);
        done();
      });
      httpBackend.flush();
    });

    it('can not update a collectionEvent with empty required annotations', function() {
      var entities = getCollectionEventEntities(false);

      _.each(entities.serverAnnotationTypes, function (serverAnnotationType) {
        var annotationType,
            collectionEvent;

        entities.serverCollectionEvent.annotations =
          _.map(entities.serverCollectionEvent.annotations,
                function (annotation) {
                  return {
                    annotationTypeId: annotation.annotationTypeId,
                    selectedValues: []
                  };
                });

        annotationType = new CollectionEventAnnotationType(serverAnnotationType);
        collectionEvent = new CollectionEvent(entities.serverCollectionEvent,
                                              entities.collectionEventType,
                                              [ annotationType ]);

        _.each(collectionEvent.annotations, function (annotation) {
          annotation.required = true;
          expect(annotation.getValue()).toBeFalsy();
        });

        expect(function () {
          collectionEvent.addOrUpdate();
        }).toThrow();
      });
    });

    it('should be able to remove a collection event', function() {
      var entities = getCollectionEventEntities(false),
          collectionEvent = entities.collectionEvent;

      httpBackend.expectDELETE(uri(collectionEvent.participantId,
                                   collectionEvent.id,
                                   collectionEvent.version))
        .respond(201, serverReply(true));

      collectionEvent.remove();
      httpBackend.flush();
    });

    function getCollectionEventEntities(isNew) {
      var study,
          participant,
          collectionEventType,
          serverAnnotationTypes,
          serverCollectionEvent,
          initServerCollectionEvent,
          annotationTypes,
          collectionEvent;

      study = fakeEntities.study();
      participant = fakeEntities.participant(study);
      serverAnnotationTypes = fakeEntities.allStudyAnnotationTypes(study);
      annotationTypes = _.map(serverAnnotationTypes, function (serverAnnotationType) {
        return new CollectionEventAnnotationType(serverAnnotationType);
      });

      collectionEventType = CollectionEventType.create(
        fakeEntities.collectionEventType(serverStudy, {
          specimenGroups:  serverStudy.specimenGroups,
          annotationTypes: annotationTypes
        }));

      serverCollectionEvent = fakeEntities.collectionEvent({
        participantId:         participant.id,
        collectionEventTypeId: collectionEventType.id,
        annotationTypes:       serverAnnotationTypes
      });
      initServerCollectionEvent = isNew ?
        _.omit(serverCollectionEvent, 'id'): serverCollectionEvent;

      collectionEvent = new CollectionEvent(initServerCollectionEvent,
                                            collectionEventType,
                                            annotationTypes);

      return {
        serverStudy:           study,
        participant:           participant,
        collectionEventType:   collectionEventType,
        serverAnnotationTypes: serverAnnotationTypes,
        serverCollectionEvent: serverCollectionEvent,
        annotationTypes:       annotationTypes,
        collectionEvent:       collectionEvent
      };
    }

    function generateAnnotationTypesAndServerAnnotations(serverStudy) {
      var annotationTypes = fakeEntities.allStudyAnnotationTypes(serverStudy);

      return _.map(annotationTypes, function (annotationType) {
        var value = fakeEntities.valueForAnnotation(annotationType);
        var serverAnnotation = fakeEntities.annotation(value, annotationType);

        return {
          annotationType: new CollectionEventAnnotationType(annotationType),
          serverAnnotation: serverAnnotation
        };
      });
    }

    function annotationsForCommand(collectionEvent) {
      return _.map(collectionEvent.annotations, function (annotation) {
        return annotation.getServerAnnotation();
      });
    }

    var addCommandKeys = [
      'participantId',
      'collectionEventTypeId',
      'timeCompleted',
      'visitNumber'
    ];

    var updateCommandKeys = addCommandKeys.concat('id');

    function addCommand(collectionEvent) {
      return _.extend(_.pick(collectionEvent, addCommandKeys),
                      { annotations: annotationsForCommand(collectionEvent) } );
    }

    function updateCommand(collectionEvent) {
      return _.extend(_.pick(collectionEvent, updateCommandKeys),
                      { annotations: annotationsForCommand(collectionEvent) },
                      testUtils.expectedVersion(collectionEvent.version));
    }

    function replyCollectionEvent(collectionEvent, newValues) {
      newValues = newValues || {};
      return new CollectionEvent(_.extend({}, collectionEvent, newValues, {version: collectionEvent.version + 1}));
    }

    function serverReply(event) {
      return { status: 'success', data: event };
    }

    function uri(/* participantId, collectionEventId, version */) {
      var participantId,
          collectionEventId,
          version,
          result = '/participants',
          args = _.toArray(arguments);

      if (args.length < 1) {
        throw new Error('participant id not specified');
      }

      participantId = args.shift();
      result += '/' + participantId + '/cevents';

      if (args.length > 0) {
        collectionEventId = args.shift();
        result += '/' + collectionEventId;
      }

      if (args.length > 0) {
        version = args.shift();
        result += '/' + version;
      }
      return result;
    }
  });

});
