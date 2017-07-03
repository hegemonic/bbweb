/**
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2017 Canadian BioSample Repository (CBSR)
 */
define(function (require) {
  'use strict';

  var _ = require('lodash');

  var component = {
    templateUrl: '/assets/javascripts/admin/components/annotationTypeView/annotationTypeView.html',
    controller: AnnotationTypeViewController,
    controllerAs: 'vm',
    bindings: {
      study:          '=',
      annotationType: '=',
      returnState:    '@',
      onUpdate:       '&'
    }
  };

  AnnotationTypeViewController.$inject = [
    '$state',
    'gettextCatalog',
    'modalInput',
    'annotationTypeUpdateModal'
  ];

  /*
   * Controller for this component.
   */
  function AnnotationTypeViewController($state,
                                        gettextCatalog,
                                        modalInput,
                                        annotationTypeUpdateModal) {
    var vm = this;

    vm.annotationTypeValueTypeLabel = vm.annotationType.getValueTypeLabel();
    updateRequiredLabel(vm.annotationType);

    vm.editName             = editName;
    vm.editRequired         = editRequired;
    vm.editDescription      = editDescription;
    vm.editSelectionOptions = editSelectionOptions;
    vm.back                 = back;

    //--

    function editName() {
      modalInput.text(gettextCatalog.getString('Edit Annotation name'),
                      gettextCatalog.getString('Name'),
                      vm.annotationType.name,
                      { required: true, minLength: 2 }).result
        .then(function (name) {
          vm.annotationType.name = name;
          vm.onUpdate()(vm.annotationType);
        });
    }

    function editRequired() {
      modalInput.boolean(gettextCatalog.getString('Edit Annotation required'),
                         gettextCatalog.getString('Required'),
                         vm.annotationType.required.toString(),
                         { required: true }).result
        .then(function (required) {
          vm.annotationType.required = (required === 'true' );
          updateRequiredLabel(vm.annotationType);
          vm.onUpdate()(vm.annotationType);
        });
    }

    function editDescription() {
      modalInput.textArea(gettextCatalog.getString('Edit Annotation description'),
                          gettextCatalog.getString('Description'),
                          vm.annotationType.description)
        .result.then(function (description) {
          var annotationType = _.extend({}, vm.annotationType, { description: description });
          vm.onUpdate()(annotationType);
        });
    }

    function editSelectionOptions() {
      annotationTypeUpdateModal.openModal(vm.annotationType).result.then(function (options) {
        var annotationType = _.extend({}, vm.annotationType, { options: options });
        vm.onUpdate()(annotationType);
      });
    }

    function back() {
      $state.go(vm.returnState, {}, { reload: true });
    }

    function updateRequiredLabel(annotationType) {
      vm.requiredLabel = vm.annotationType.required ?
        gettextCatalog.getString('Yes') : gettextCatalog.getString('No');
    }
  }

  return component;
});