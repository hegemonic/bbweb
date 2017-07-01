/**
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2017 Canadian BioSample Repository (CBSR)
 */
define(function () {
  'use strict';

  var component = {
    templateUrl: '/assets/javascripts/collection/components/participantGet/participantGet.html',
    controller: ParticipantGetController,
    controllerAs: 'vm',
    bindings: {
      study: '='
    }
  };

  ParticipantGetController.$inject = [
    '$q',
    '$log',
    '$state',
    'gettextCatalog',
    'modalService',
    'Participant',
    'breadcrumbService'
  ];

  var patientDoesNotExistRe = /EntityCriteriaNotFound: participant with unique ID does not exist/;

  var studyMismatchRe = /EntityCriteriaError: participant not in study/i;

  /*
   * Controller for this component.
   */
  function ParticipantGetController($q,
                                    $log,
                                    $state,
                                    gettextCatalog,
                                    modalService,
                                    Participant,
                                    breadcrumbService) {
    var vm = this;

    vm.breadcrumbs = [
      breadcrumbService.forState('home'),
      breadcrumbService.forState('home.collection'),
      breadcrumbService.forStateWithFunc('home.collection.study', function () {
        return vm.study.name;
      })
    ];

    vm.uniqueId = '';
    vm.onSubmit = onSubmit;

    //--

    function onSubmit() {
      if (vm.uniqueId.length > 0) {
        Participant.getByUniqueId(vm.study.id, vm.uniqueId)
          .then(function (participant) {
            $state.go('home.collection.study.participant.summary', { participantId: participant.id });
          })
          .catch(participantGetError);
      }
    }

    function participantGetError(error) {
      if (error.status !== 'error') {
        $log.error('expected an error reply: ', JSON.stringify(error));
        return;
      }

      if (error.message.match(patientDoesNotExistRe)) {
        createParticipantModal(vm.uniqueId);
      } else if (error.message.match(studyMismatchRe)) {
        modalService.modalOk(
          gettextCatalog.getString('Duplicate unique ID'),
          gettextCatalog.getString(
          'Unique ID <strong>{{id}}</strong> is already in use by a participant ' +
              'in another study. Please use a different one.',
            { id: vm.uniqueId }))
          .then(function () {
            vm.uniqueId = '';
          });
      } else {
          $log.error('could not get participant by uniqueId: ', JSON.stringify(error));
      }
    }

    function createParticipantModal(uniqueId) {
      modalService.modalOkCancel(
        gettextCatalog.getString('Create participant'),
        gettextCatalog.getString(
          'Would you like to create participant with unique ID <strong>{{id}}</strong>?',
          { id: uniqueId })
      ).then(function() {
        $state.go('home.collection.study.participantAdd', { uniqueId: uniqueId });
      }).catch(function() {
        $state.reload();
      });
    }
  }

  return component;
});