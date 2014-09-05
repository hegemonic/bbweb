/**
 * User administration controllers.
 */
define(['angular', 'underscore', 'common'], function(angular, _, common) {
  'use strict';

  var mod = angular.module('admin.users.controllers', ['users.services']);

  /**
   * Displays a list of users in a table.
   */
  mod.controller('UsersTableCtrl', [
    '$rootScope', '$scope', '$state', '$filter', 'ngTableParams', 'userService',
    function($rootScope, $scope, $state, $filter, ngTableParams, userService) {
      $rootScope.pageTitle = 'Biobank users';
      $scope.users = [];
      userService.getAllUsers().then(function(data) {
        $scope.users = data;

        /* jshint ignore:start */
        $scope.tableParams = new ngTableParams({
          page: 1,            // show first page
          count: 10,          // count per page
          sorting: {
            name: 'asc'       // initial sorting
          }
        }, {
          counts: [], // hide page counts control
          total: $scope.users.length,
          getData: function($defer, params) {
            var orderedData = params.sorting()
              ? $filter('orderBy')($scope.users, params.orderBy())
              : $scope.users;
            $defer.resolve(orderedData.slice(
              (params.page() - 1) * params.count(),
              params.page() * params.count()));
          }
        });
        /* jshint ignore:end */

        $scope.userInformation = function(user) {
          $state.go("admin.users.user", { userId: user.id });
        };

        $scope.update = function(user) {
          $state.go("admin.users.user", { userId: user.id });
        };
      });
    }]);

  /**
   * Displays a list of users in a table.
   */
  mod.controller('UserUpdateCtrl', [
    '$rootScope', '$scope', '$state', '$filter', 'userService', 'modalService', 'userToModify',
    function($rootScope, $scope, $state, $filter, userService, modalService, userToModify) {

      var onError = function (error) {
        var modalOptions = {
          closeButtonText: 'Cancel',
          actionButtonText: 'OK'
        };

        if (error.message.indexOf("expected version doesn't match current version") > -1) {
          /* concurrent change error */
          modalOptions.headerText = 'Modified by another user';
          modalOptions.bodyText = 'Another user already made changes to this user. Press OK to make ' +
            ' your changes again, or Cancel to dismiss your changes.';
        } else {
          /* some other error */
          modalOptions.headerText = 'Cannot update user';
          modalOptions.bodyText = error.message;
        }

        modalService.showModal({}, modalOptions).then(
          function (result) {
            stateHelper.reloadAndReinit();
          },
          function () {
            $state.go("admin.users");
          });
      };

      $scope.form = {
        user: userToModify,
        password: '',
        confirmPassword: '',
        submit: function(user, password) {
          userService.update(user, password).then(
            function(event) {
              $state.go("admin.users");
            },
            onError
          );
        },
        cancel: function() {
        }
      };
    }]);

  return mod;
});