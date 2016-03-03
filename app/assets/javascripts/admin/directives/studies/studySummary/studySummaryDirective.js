/**
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2015 Canadian BioSample Repository (CBSR)
 */
define(['underscore'], function(_) {
  'use strict';

  /**
   *
   */
  function studySummaryDirective() {
    var directive = {
      restrict: 'E',
      scope: {},
      bindToController: {
        study: '='
      },
      templateUrl : '/assets/javascripts/admin/directives/studies/studySummary/studySummary.html',
      controller: StudySummaryCtrl,
      controllerAs: 'vm'
    };

    return directive;
  }

  StudySummaryCtrl.$inject = [
    '$state',
    'modalService',
    'notificationsService'
  ];

  function StudySummaryCtrl($state,
                            modalService,
                            notificationsService) {

    var validStatusActions = ['disable', 'enable', 'retire', 'unretire'],
        vm = this;

    vm.descriptionToggleLength = 100;
    vm.changeStatus = changeStatus;
    vm.editName = editName;
    vm.editDescription = editDescription;

    //--

    function changeStatus(statusAction) {
      var modalOptions = {
        closeButtonText: 'Cancel',
        headerHtml: 'Confirm study ',
        bodyHtml: 'Are you sure you want to '
      };

      if (!_.contains(validStatusActions, statusAction)) {
        throw new Error('invalid status: ' + statusAction);
      }

      modalOptions.headerHtml += statusAction;
      modalOptions.bodyHtml += statusAction + ' study ' + vm.study.name + '?';

      modalService.showModal({}, modalOptions).then(function () {
        vm.study[statusAction]().then(function (study) {
          vm.study = study;
          notificationsService.success('The study\'s status has been updated.', null, 2000);
        });
      });
    }

    function postUpdate(message, title, timeout) {
      return function (study) {
        vm.study = study;
        notificationsService.success(message, title, timeout);
      };
    }

    function editName() {
      modalService.modalTextAreaInput('Edit name', 'Name', vm.study.name)
        .then(function (name) {
          vm.study.updateName(name)
            .then(postUpdate('Name changed successfully.',
                             'Change successful',
                             1500))
            .catch(notificationsService.updateError);
        });
    }

    function editDescription() {
      modalService.modalTextAreaInput('Edit description', 'Description', vm.study.description)
        .then(function (description) {
          vm.study.updateDescription(description)
            .then(postUpdate('Description changed successfully.',
                             'Change successful',
                             1500))
            .catch(notificationsService.updateError);
        });
    }
  }

  return studySummaryDirective;

});