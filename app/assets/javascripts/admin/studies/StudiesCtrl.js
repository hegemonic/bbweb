define(['../module'], function(module) {
  'use strict';

  module.controller('StudiesCtrl', StudiesCtrl);

  StudiesCtrl.$inject = ['studiesService', 'paginatedStudies'];

  /**
   * Displays a list of studies with each in its own mini-panel.
   *
   */
  function StudiesCtrl(studiesService, paginatedStudies) {
    var vm = this;
    vm.studies = [];
    vm.paginatedStudies = paginatedStudies;

    vm.haveConfiguredStudies = (paginatedStudies.total > 0);
    vm.nameFilter       = '';
    vm.possibleStatuses = [
      { id: 'all',      title: 'All' },
      { id: 'disabled', title: 'Disabled' },
      { id: 'enabled',  title: 'Enabled' },
      { id: 'retired',  title: 'Retired' }
    ];
    vm.status           = vm.possibleStatuses[0];

    vm.nameFilterUpdated = nameFilterUpdated;
    vm.statusFilterUpdated = statusFilterUpdated;

    updateStudies();

    function updateMessage() {
      if (vm.paginatedStudies.total <= 0) {
        vm.message = 'No studies match the criteria. ';
      } else if (vm.paginatedStudies.total === 1) {
        vm.message = 'There is 1 study that matches the criteria. ';
      } else {
        vm.message = 'There are ' + vm.paginatedStudies.total + ' studies that match the criteria. ';
        if (vm.paginatedStudies.total > vm.studies.length) {
          vm.message += 'Displaying the first ' + vm.studies.length + '.';
        }
      }
    }

    function updateStudies() {
      studiesService.getStudies(vm.nameFilter,
                                vm.status.id,
                                1,
                                6,
                                'name',
                                'ascending')
        .then(function (paginatedStudies) {
          vm.studies = paginatedStudies.items;
          vm.paginatedStudies = paginatedStudies;
          updateMessage();
        });
    }

    /**
     * Called when user enters text into the 'name filter'.
     */
    function nameFilterUpdated() {
      updateStudies();
    }

    /**
     * Called when user selects a status from the 'status filter' select.
     */
    function statusFilterUpdated() {
      updateStudies();
    }
  }

});
