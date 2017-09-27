/**
 * Jasmine test suite
 *
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2016 Canadian BioSample Repository (CBSR)
 */
define(function (require) {
  'use strict';

  var mocks = require('angularMocks'),
      _     = require('lodash'),
      filtersSharedBehaviour = require('../../../test/filtersSharedBehaviour');

  describe('Component: nameFilter', function() {

    function SuiteMixinFactory(ComponentTestSuiteMixin) {

      function SuiteMixin() {
        ComponentTestSuiteMixin.call(this);
      }

      SuiteMixin.prototype = Object.create(ComponentTestSuiteMixin.prototype);
      SuiteMixin.prototype.constructor = SuiteMixin;

      SuiteMixin.prototype.createController = function (bindings) {
      var self = this,
          defaultBindings = {},
          actualBindings = {};

      self.nameFilterUpdated = jasmine.createSpy().and.returnValue(null);
      self.filtersCleared = jasmine.createSpy().and.returnValue(null);

      defaultBindings = {
        stateData:            [ 'enabled', 'disbled' ],
        onNameFilterUpdated:  self.nameFilterUpdated,
        onStateFilterUpdated: self.stateFilterUpdated,
        onFiltersCleared:     self.filtersCleared
      };

        _.extend(actualBindings, defaultBindings, bindings);

        ComponentTestSuiteMixin.prototype.createController.call(
          this,
          [
            '<name-filter ',
            '    on-name-filter-updated="vm.onNameFilterUpdated" ',
            '    on-filters-cleared="vm.onFiltersCleared"> ',
            '</name-filter>'
          ].join(''),
          actualBindings,
          'nameFilter');
      };

      return SuiteMixin;
    }

    beforeEach(mocks.module('biobankApp', 'biobank.test'));

    beforeEach(inject(function(ComponentTestSuiteMixin) {
      _.extend(this, new SuiteMixinFactory(ComponentTestSuiteMixin).prototype);

      this.injectDependencies('$q', '$rootScope', '$compile', 'factory');
      this.putHtmlTemplates(
        '/assets/javascripts/common/components/nameFilter/nameFilter.html',
        '/assets/javascripts/common/components/debouncedTextInput/debouncedTextInput.html');
    }));

    describe('for name filter', function() {
      var context = {};

      beforeEach(function() {
        context.createController = this.createController.bind(this);
      });

      filtersSharedBehaviour.nameFiltersharedBehaviour(context);

    });

  });

});