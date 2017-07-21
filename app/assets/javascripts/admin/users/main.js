/**
 * Study module.
 *
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2015 Canadian BioSample Repository (CBSR)
*/
define(function (require) {
  'use strict';

  var angular = require('angular'),
      name = 'biobank.admin.users',
      module;

  module = angular.module(name, [ 'biobank.studies' ]);

  module
    .config(require('./states'))
    .component('manageUsers',     require('./components/manageUsers/manageUsersComponent'))
    .component('userAdmin',       require('./components/userAdmin/userAdminComponent'))
    .component('usersPagedList',  require('./components/usersPagedList/usersPagedListComponent'))
    .component('userProfile',     require('./components/userProfile/userProfileComponent'))
    .component('userRoles',       require('./components/userRoles/userRolesComponent'))
    .component('userMemberships', require('./components/userMemberships/userMembershipsComponent'))
    .component('nameEmailStateFilters',
               require('./components/nameEmailStateFilters/nameEmailStateFiltersComponent'));

  return {
    name: name,
    module: module
  };
});
