/**
 * Test module
 *
 * Provides factories for various client side domain objects.
 *
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2015 Canadian BioSample Repository (CBSR)
 */
define(function (require) {
  'use strict';

  var angular = require('angular');

  return angular.module('biobank.test', [])
    .service('testDomainEntities',
             require('../../../assets/javascripts/test/testDomainEntities'))
    .service('factory',
             require('../../../assets/javascripts/test/factory'))
    .service('testUtils',
             require('../../../assets/javascripts/test/testUtils'))
    .factory('EntityTestSuite',
             require('../../../assets/javascripts/test/EntityTestSuite'))
    .factory('ModalTestSuiteMixin',
             require('../../../assets/javascripts/test/ModalTestSuiteMixin'))
    .factory('TestSuiteMixin',
             require('../../../assets/javascripts/test/TestSuiteMixin'))
    .factory('ServerReplyMixin',
             require('../../../assets/javascripts/test/ServerReplyMixin'))
    .factory('AnnotationsEntityTestSuiteMixin',
             require('../../../assets/javascripts/test/AnnotationsEntityTestSuiteMixin'))
    .factory('ComponentTestSuiteMixin',
             require('../../../assets/javascripts/test/ComponentTestSuiteMixin'))
    .factory('DirectiveTestSuiteMixin',
             require('../../../assets/javascripts/test/DirectiveTestSuiteMixin'))
    .factory('ShippingComponentTestSuiteMixin',
             require('../../../assets/javascripts/test/ShippingComponentTestSuiteMixin'))
    .factory('MebershipSpecCommon',
             require('../../../assets/javascripts/test/MembershipSpecCommon'));
});
