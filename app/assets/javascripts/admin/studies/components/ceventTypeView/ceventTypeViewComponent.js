/**
 *
 */
define(function (require) {
  'use strict';

  var _ = require('lodash');

  var component = {
    templateUrl : '/assets/javascripts/admin/studies/components/ceventTypeView/ceventTypeView.html',
    controller: CeventTypeViewController,
    controllerAs: 'vm',
    bindings: {
        study:      '=',
        ceventType: '='
    }
  };

  CeventTypeViewController.$inject = [
    '$scope',
    '$state',
    'gettextCatalog',
    'modalService',
    'modalInput',
    'domainNotificationService',
    'notificationsService',
    'CollectionEventAnnotationTypeModals'
  ];

  /*
   * Controller for this component.
   */
  function CeventTypeViewController($scope,
                                    $state,
                                    gettextCatalog,
                                    modalService,
                                    modalInput,
                                    domainNotificationService,
                                    notificationsService,
                                    CollectionEventAnnotationTypeModals) {
    var vm = this;

    _.extend(vm, new CollectionEventAnnotationTypeModals());

    vm.isPanelCollapsed     = false;

    vm.editName                  = editName;
    vm.editDescription           = editDescription;
    vm.editRecurring             = editRecurring;
    vm.editSpecimenDescription   = editSpecimenDescription;
    vm.editAnnotationType        = editAnnotationType;
    vm.removeAnnotationType      = removeAnnotationType;
    vm.addAnnotationType         = addAnnotationType;
    vm.removeSpecimenDescription = removeSpecimenDescription;
    vm.addSpecimenDescription    = addSpecimenDescription;
    vm.addSpecimenDescription    = addSpecimenDescription;
    vm.panelButtonClicked        = panelButtonClicked;
    vm.removeCeventType          = removeCeventType;

    //--

    function postUpdate(message, title, timeout) {
      timeout = timeout || 1500;
      return function (ceventType) {
        vm.ceventType = ceventType;
        notificationsService.success(message, title, timeout);
      };
    }

    function editName() {
      modalInput.text(gettextCatalog.getString('Edit Event Type name'),
                      gettextCatalog.getString('Name'),
                      vm.ceventType.name,
                      { required: true, minLength: 2 }).result
        .then(function (name) {
          vm.ceventType.updateName(name)
            .then(function (ceventType) {
              $scope.$emit('collection-event-type-name-changed', ceventType);
              postUpdate(gettextCatalog.getString('Name changed successfully.'),
                         gettextCatalog.getString('Change successful'))(ceventType);
            })
            .catch(notificationsService.updateError);
        });
    }

    function editDescription() {
      modalInput.textArea(gettextCatalog.getString('Edit Event Type description'),
                          gettextCatalog.getString('Description'),
                          vm.ceventType.description
                         ).result
        .then(function (description) {
          vm.ceventType.updateDescription(description)
            .then(postUpdate(gettextCatalog.getString('Description changed successfully.'),
                             gettextCatalog.getString('Change successful')))
            .catch(notificationsService.updateError);
      });
    }

    function editRecurring() {
      modalInput.boolean(gettextCatalog.getString('Edit Event Type recurring'),
                         gettextCatalog.getString('Recurring'),
                         vm.ceventType.recurring.toString()
                        ).result
        .then(function (recurring) {
          vm.ceventType.updateRecurring(recurring === 'true')
            .then(postUpdate(gettextCatalog.getString('Recurring changed successfully.'),
                             gettextCatalog.getString('Change successful')))
            .catch(notificationsService.updateError);
      });
    }

    function addAnnotationType() {
      $state.go('home.admin.studies.study.collection.ceventType.annotationTypeAdd');
    }

    function addSpecimenDescription() {
      $state.go('home.admin.studies.study.collection.ceventType.specimenDescriptionAdd');
    }

    function editSpecimenDescription(specimenDescription) {
      $state.go('home.admin.studies.study.collection.ceventType.specimenDescriptionView',
                { specimenDescriptionId: specimenDescription.id });
    }

    function removeSpecimenDescription(specimenDescription) {
      if (!vm.study.isDisabled()) {
        throw new Error('modifications not allowed');
      }

      return domainNotificationService.removeEntity(
        removePromiseFunc,
        gettextCatalog.getString('Remove specimen'),
        gettextCatalog.getString('Are you sure you want to remove specimen {{name}}?',
                                 { name: specimenDescription.name }),
        gettextCatalog.getString('Remove failed'),
        gettextCatalog.getString('Specimen {{name} cannot be removed',
                                 { name: specimenDescription.name }));

      function removePromiseFunc() {
        return vm.ceventType.removeSpecimenDescription(specimenDescription)
          .then(function () {
            notificationsService.success(gettextCatalog.getString('Specimen removed'));
          });
      }
    }

    function editAnnotationType(annotType) {
      $state.go('home.admin.studies.study.collection.ceventType.annotationTypeView',
                { annotationTypeId: annotType.id });
    }

    function removeAnnotationType(annotationType) {
      if (_.includes(vm.annotationTypeIdsInUse, annotationType.id)) {
        vm.removeInUseModal(annotationType, vm.annotationTypeName);
      } else {
        if (!vm.study.isDisabled()) {
          throw new Error('modifications not allowed');
        }

        vm.remove(annotationType, function () {
          return vm.ceventType.removeAnnotationType(annotationType)
            .then(function () {
              notificationsService.success(gettextCatalog.getString('Annotation removed'));
            });
        });
      }
    }

    function panelButtonClicked() {
      vm.isPanelCollapsed = !vm.isPanelCollapsed;
    }

    function removeCeventType() {
      vm.ceventType.inUse().then(function (inUse) {
        if (inUse) {
          modalService.modalOk(
            gettextCatalog.getString('Collection event in use'),
             gettextCatalog.getString(
                'This collection event cannot be removed since one or more participants are using it. ' +
                   'If you still want to remove it, the participants using it have to be modified ' +
                   'to no longer use it.'));
        } else {
          domainNotificationService.removeEntity(
            promiseFn,
            gettextCatalog.getString('Remove collection event'),
             gettextCatalog.getString(
                'Are you sure you want to remove collection event with name <strong>{{name}}</strong>?',
                { name: vm.ceventType.name }),
            gettextCatalog.getString('Remove failed'),
            gettextCatalog.getString('Collection event with name {{name}} cannot be removed',
                    { name: vm.ceventType.name }));
        }
      });

      function promiseFn() {
        return vm.ceventType.remove().then(function () {
          notificationsService.success(gettextCatalog.getString('Collection event removed'));
          $state.go('home.admin.studies.study.collection', {}, { reload: true });
        });
      }
    }
  }

  return component;
});