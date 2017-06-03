/**
 * Jasmine test suite
 *
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2015 Canadian BioSample Repository (CBSR)
 */
define(function (require) {
  'use strict';

  var angular                = require('angular'),
      mocks                  = require('angularMocks'),
      _                      = require('lodash'),
      entityUpdateSharedSpec = require('../../../../test/entityUpdateSharedSpec');

  describe('Directive: studySummaryDirective', function() {

    beforeEach(mocks.module('biobankApp', 'biobank.test'));

    beforeEach(inject(function(TestSuiteMixin) {
      var self = this,
          specimenDescription,
          ceventType;

      _.extend(self, TestSuiteMixin.prototype);

      self.injectDependencies('$q',
                              '$rootScope',
                              '$compile',
                              '$state',
                              'Study',
                              'CollectionEventType',
                              'CollectionSpecimenDescription',
                              'modalService',
                              'notificationsService',
                              'stateHelper',
                              'factory');

      specimenDescription = self.factory.collectionSpecimenDescription();
      ceventType = self.CollectionEventType.create(
        self.factory.collectionEventType({ specimenDescriptions: [ specimenDescription ]}));

      spyOn(self.CollectionEventType, 'list').and.returnValue(self.$q.when([ ceventType ]));
      spyOn(self.modalService, 'showModal').and.returnValue(self.$q.when(true));

      self.study = new self.Study(self.factory.study());

      self.putHtmlTemplates(
        '/assets/javascripts/admin/studies/directives/studySummary/studySummary.html',
        '/assets/javascripts/common/directives/truncateToggle/truncateToggle.html',
        '/assets/javascripts/common/directives/statusLine/statusLine.html',
        '/assets/javascripts/common/modalInput/modalInput.html');

      self.createController = function () {
        self.element = angular.element('<study-summary study="vm.study"></study-summary>');
        self.scope = self.$rootScope.$new();
        self.scope.vm = { study: self.study };

        self.eventRxFunc = jasmine.createSpy().and.returnValue(null);
        self.scope.$on('tabbed-page-update', self.eventRxFunc);

        self.$compile(self.element)(self.scope);
        self.scope.$digest();
        self.controller = self.element.controller('studySummary');
      };

      spyOn(self.stateHelper, 'updateBreadcrumbs').and.returnValue(null);
    }));

    it('initialization is valid', function() {
      this.createController();
      expect(this.controller.study).toBe(this.study);
      expect(this.controller.descriptionToggleLength).toBeDefined();
      expect(this.controller.hasSpecimenDescriptions).toBeTrue();
      expect(this.eventRxFunc).toHaveBeenCalled();
    });

    it('should have valid settings when study has no collection event types', function() {
      this.CollectionEventType.list = jasmine.createSpy().and.returnValue(this.$q.when([ ]));
      this.createController(this);
      expect(this.controller.hasSpecimenDescriptions).toBeFalse();
    });

    describe('updates to name', function () {

      var context = {};

      beforeEach(inject(function () {
        context.entity             = this.Study;
        context.createController   = this.createController.bind(this);
        context.updateFuncName     = 'updateName';
        context.controllerFuncName = 'editName';
        context.modalInputFuncName = 'text';
      }));

      entityUpdateSharedSpec(context);

    });

    describe('updates to description', function () {

      var context = {};

      beforeEach(inject(function () {
        context.entity             = this.Study;
        context.createController   = this.createController.bind(this);
        context.updateFuncName     = 'updateDescription';
        context.controllerFuncName = 'editDescription';
        context.modalInputFuncName = 'textArea';
      }));

      entityUpdateSharedSpec(context);

    });

    describe('enabling a study', function() {
      var context = {};

      beforeEach(inject(function () {
        context.state = 'enable';
      }));

      sharedStudyStateBehaviour(context);
    });

    describe('disabling a study', function() {
      var context = {};

      beforeEach(inject(function () {
        context.state = 'disable';
      }));

      sharedStudyStateBehaviour(context);
    });

    describe('retiring a study', function() {
      var context = {};

      beforeEach(inject(function () {
        context.state = 'retire';
      }));

      sharedStudyStateBehaviour(context);
    });

    describe('unretiring a study', function() {
      var context = {};

      beforeEach(inject(function () {
        context.state = 'unretire';
      }));

      sharedStudyStateBehaviour(context);
    });


    function sharedStudyStateBehaviour(context) {

      describe('(shared) study state', function () {

        it('change state', function () {
          spyOn(this.modalService, 'modalOkCancel').and.returnValue(this.$q.when('ok'));
          spyOn(this.Study, 'get').and.returnValue(this.$q.when(this.study));
          spyOn(this.Study.prototype, context.state).and.returnValue(this.$q.when(this.study));

          this.createController();
          this.controller.changeState(context.state);
          this.scope.$digest();
          expect(this.Study.prototype[context.state]).toHaveBeenCalled();
        });

      });
    }

    it('should throw error for when trying to change to an invalid state', function () {
      var self = this,
          badState = 'xxx';

      this.createController();
      expect(function () {
        self.controller.changeState(badState);
      }).toThrow(new Error('invalid state: ' + badState));
    });
  });

});
