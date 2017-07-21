/**
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2017 Canadian BioSample Repository (CBSR)
 */
define(function () {
  'use strict';

  /*
   * Allows the logged in user to modify another user's roles.
   */
  var component = {
    templateUrl: '/assets/javascripts/admin/users/components/userRoles/userRoles.html',
    controller: UserRolesController,
    controllerAs: 'vm',
    bindings: {
      user: '<'
    }
  };

   UserRolesController.$inject = ['breadcrumbService', 'gettextCatalog'];

  /*
   * Controller for this component.
   */
   function UserRolesController(breadcrumbService, gettextCatalog) {
    var vm = this;
    vm.$onInit = onInit;

    //--

    function onInit() {
      vm.breadcrumbs = [
        breadcrumbService.forState('home'),
        breadcrumbService.forState('home.admin'),
        breadcrumbService.forState('home.admin.users'),
        breadcrumbService.forState('home.admin.users.roles'),
      ];
    }

  }

  return component;
});
