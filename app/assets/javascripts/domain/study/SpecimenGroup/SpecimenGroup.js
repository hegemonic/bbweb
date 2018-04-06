/*
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2018 Canadian BioSample Repository (CBSR)
 */

import _ from 'lodash'

/**
 *
 */
/* @ngInject */
function SpecimenGroupFactory(funutils,
                              validationService,
                              biobankApi,
                              ConcurrencySafeEntity,
                              DomainError) {

  var requiredKeys = [
    'studyId',
    'id',
    'version',
    'timeAdded',
    'name',
    'units',
    'anatomicalSourceType',
    'preservationType',
    'preservationTemperature',
    'specimenType'
  ];

  var validateObj = funutils.partial(
    validationService.condition1(
      validationService.validator('must be a map', _.isObject),
      validationService.validator('has the correct keys',
                                  validationService.hasKeys.apply(null, requiredKeys))),
    _.identity);

  function SpecimenGroup(obj) {
    var self = this,
        defaults = {
          studyId:                     null,
          name:                        '',
          description:                 null,
          units:                       '',
          anatomicalSourceType:        '',
          preservationType:            '',
          preservationTemperature: '',
          specimenType:                ''
        };

    obj = obj || {};
    ConcurrencySafeEntity.call(self, obj);
    Object.assign(this, defaults, _.pick(obj, Object.keys(defaults)));
  }

  SpecimenGroup.prototype = Object.create(ConcurrencySafeEntity.prototype);

  /**
   * Used by promise code, so it must return an error rather than throw one.
   */
  SpecimenGroup.create = function (obj) {
    var validation = validateObj(obj);
    if (!_.isObject(validation)) {
      return new Error('invalid object from server: ' + validation);
    }
    return new SpecimenGroup(obj);
  };

  SpecimenGroup.get = function(studyId, id) {
    return biobankApi.get(uri(studyId) + '?sgId=' + id)
      .then(function(reply) {
        return SpecimenGroup.create(reply);
      });
  };

  SpecimenGroup.list = function (studyId) {
    return biobankApi.get(uri(studyId))
      .then((reply) => reply.map((cet) => SpecimenGroup.create(cet)));
  };

  SpecimenGroup.prototype.addOrUpdate = function () {
    var self = this,
        cmd = Object.assign(_.pick(self,
                              'studyId',
                              'name',
                              'units',
                              'anatomicalSourceType',
                              'preservationType',
                              'preservationTemperature',
                              'specimenType'),
                       funutils.pickOptional(self, 'description'));

    return addOrUpdateInternal().then(function(reply) {
      return SpecimenGroup.create(reply);
    });

    // --

    function addOrUpdateInternal() {
      if (self.isNew()) {
        return biobankApi.post(uri(self.studyId), cmd);
      }
      Object.assign(cmd, { id: self.id, expectedVersion: self.version });
      return biobankApi.put(uri(self.studyId, self.id), cmd);
    }
  };

  SpecimenGroup.prototype.remove = function () {
    return biobankApi.del(uri(this.studyId, this.id, this.version));
  };

  /**
   * Utility function that fetches a specimen group's unit field.
   *
   * @param {Array} specimenGroups - array of specime group objects.
   *
   * @param {String} id - the id of the specimen group.
   *
   * @return the units field for the requested specimen group.
   */
  SpecimenGroup.getUnits = function (specimenGroups, id) {
    if (!id) { return 'Amount'; }

    var sg = _.find(specimenGroups, { id: id });
    if (sg) {
      return sg.units;
    }
    throw new DomainError('specimen group ID not found: ' + id);
  };

  function uri(/* studyId, specimenGroupId, version */) {
    var studyId,
        specimenGroupId,
        version,
        result = '/studies',
        args = _.toArray(arguments);

    if (args.length < 1) {
      throw new DomainError('study id not specified');
    }

    studyId = args.shift();
    result += '/' + studyId + '/sgroups';

    if (args.length > 0) {
      specimenGroupId = args.shift();
      result += '/' + specimenGroupId;
    }

    if (args.length > 0) {
      version = args.shift();
      result += '/' + version;
    }
    return result;
  }

  return SpecimenGroup;
}

export default ngModule => ngModule.factory('SpecimenGroup', SpecimenGroupFactory)
