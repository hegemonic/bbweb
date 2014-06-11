/**
 * User package module.
 * Manages all sub-modules so other RequireJS modules only have to import the package.
 */
define(['angular', './routes', './services', './directives/studyTabs'], function(angular) {
  'use strict';

  return angular.module('biobank.study', [
    'ngCookies',
    'ui.bootstrap',
    'ngRoute',
    'study.routes',
    'study.services',
    'study.directives.studySummaryTabContent']);
});
