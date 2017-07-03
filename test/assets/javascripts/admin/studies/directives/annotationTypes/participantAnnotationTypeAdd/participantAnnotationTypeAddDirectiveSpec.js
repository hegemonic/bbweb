/**
 * Jasmine test suite
 */
define(function(require) {
  'use strict';

  var angular                              = require('angular'),
      mocks                                = require('angularMocks'),
      _                                    = require('lodash'),
      annotationTypeAddComponentSharedSpec = require('../../../../../test/annotationTypeAddComponentSharedSpec');

  describe('Directive: participantAnnotationTypeAddDirective', function() {

    var createController = function () {
      this.element = angular.element([
        '<participant-annotation-type-add',
        '  study="vm.study"',
        '</participant-annotation-type-add>'
      ].join(''));

      this.scope = this.$rootScope.$new();
      this.scope.vm = { study: this.study };
      this.$compile(this.element)(this.scope);
      this.scope.$digest();
      this.controller = this.element.controller('participantAnnotationTypeAdd');
    };

    beforeEach(mocks.module('biobankApp', 'biobank.test'));

    beforeEach(inject(function(TestSuiteMixin) {
      var self = this;

      _.extend(self, TestSuiteMixin.prototype);
      self.injectDependencies('$rootScope',
                              '$compile',
                              'Study',
                              'factory');

      self.study = new self.Study(self.factory.study());

      self.putHtmlTemplates(
        '/assets/javascripts/admin/studies/directives/annotationTypes/participantAnnotationTypeAdd/participantAnnotationTypeAdd.html',
        '/assets/javascripts/admin/components/annotationTypeAdd/annotationTypeAdd.html');
    }));

    it('should have  valid scope', function() {
      createController.call(this);
      expect(this.controller.study).toBe(this.study);
    });

    describe('for onSubmit and onCancel', function () {
      var context = {};

      beforeEach(inject(function () {
        context.createController          = createController;
        context.scope                     = this.scope;
        context.controller                = this.controller;
        context.entity                    = this.Study;
        context.addAnnotationTypeFuncName = 'addAnnotationType';
        context.returnState               = 'home.admin.studies.study.participants';
      }));

      annotationTypeAddComponentSharedSpec(context);
    });

  });

});
