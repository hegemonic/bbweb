// Jasmine test suite
//
define(['angular', 'angularMocks', 'biobankApp'], function(angular, mocks) {
  'use strict';

  describe('Controller: StudiesCtrl', function() {
    var scope;

    var studies = [
      {name: 'ST1'},
      {name: 'ST2'}
    ];

    // function generatePagedResults() {
    //   return {
    //     items: studies,
    //     page: 1,
    //     pageSize: 5,
    //     maxPages: 1,
    //     prev: null,
    //     next: 2,
    //     offset: 0,
    //     total: 10
    //   };
    // }

    beforeEach(mocks.module('biobankApp'));

    beforeEach(inject(function($controller, $rootScope) {
      scope = $rootScope.$new();

      $controller('StudiesCtrl as vm', {
        $scope: scope,
        studyCount: 1
      });

      scope.$digest();
    }));

    // FIXME - this test no longer valid
    xit('should contain all studies on startup', function() {
      //expect(_.difference(studies, scope.studies)).toEqual([]);

      expect(scope.vm.studies).toEqual(studies);
    });

  });

});