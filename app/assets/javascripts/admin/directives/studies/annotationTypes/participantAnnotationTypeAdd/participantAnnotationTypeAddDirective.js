/**
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2016 Canadian BioSample Repository (CBSR)
 */
define(function () {
  'use strict';

  /**
   *
   */
  function participantAnnotationTypeAddDirective() {
    var directive = {
      restrict: 'E',
      scope: {},
      bindToController: {
        study: '='
      },
      templateUrl : '/assets/javascripts/admin/directives/studies/annotationTypes/participantAnnotationTypeAdd/participantAnnotationTypeAdd.html',
      controller: ParticipantAnnotationTypeAddCtrl,
      controllerAs: 'vm'
    };

    return directive;
  }

  ParticipantAnnotationTypeAddCtrl.$inject = [
    'annotationTypeAddService'
  ];

  var returnState = 'home.admin.studies.study.participants';

  function ParticipantAnnotationTypeAddCtrl(annotationTypeAddService) {
    var vm = this;

    vm.onSubmit        = onSubmit;
    vm.onAddsuccessful = annotationTypeAddService.onAddSuccessful(returnState);
    vm.onCancel        = annotationTypeAddService.onCancel(returnState);

    //--

    function onSubmit(annotationType) {
      vm.study.addAnnotationType(annotationType)
        .then(vm.onAddsuccessful).catch(annotationTypeAddService.addFailed);
    }
  }

  return participantAnnotationTypeAddDirective;

});
