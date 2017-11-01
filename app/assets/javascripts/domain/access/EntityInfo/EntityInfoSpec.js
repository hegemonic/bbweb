/**
 * @author Nelson Loyola <loyola@ualberta.ca>
 * @copyright 2017 Canadian BioSample Repository (CBSR)
 */
/* global angular */

import _ from 'lodash'
import ngModule from '../../index'

describe('EntityInfo', function() {

  beforeEach(() => {
    angular.mock.module(ngModule, 'biobank.test')
    angular.mock.inject(function(EntityTestSuiteMixin) {
      _.extend(this, EntityTestSuiteMixin)

      this.injectDependencies('$rootScope',
                              'EntityInfo',
                              'Factory',
                              'TestUtils')

      this.TestUtils.addCustomMatchers()

      this.jsonObj = () => ({
        id: this.Factory.stringNext(),
        name: this.Factory.stringNext()
      })

      this.createEntityInfoFrom = (obj) => new this.EntityInfo(obj)

      this.createEntityInfo = () => this.EntityInfo.ceate(this.jsonObj())

    })
  })

  it('constructor with no parameters has default values', function() {
    const entityInfo = new this.EntityInfo()
    expect(entityInfo.id).toBeNull()
    expect(entityInfo.name).toBeNull()
  })

  describe('for creating', function() {

    it('can create from JSON', function() {
      const json = this.jsonObj(),
          entityInfo = this.EntityInfo.create(json)
      expect(entityInfo.id).toBe(json.id)
      expect(entityInfo.name).toBe(json.name)
    })

    it('create fails for an invalid object', function() {
      const invalidFields = [
        { id: 1 },
        { name: 1 }
      ]
      this.invalidFieldsTest(invalidFields,
                             invalidField => Object.assign(this.jsonObj(), invalidField),
                             this.EntityInfo.create)
    })

    it('fails when required fields are missing', function() {
      this.missingFieldsTest(this.EntityInfo.SCHEMA.required,
                             (requiredField) => _.omit(this.jsonObj(), requiredField),
                             this.EntityInfo.create)
    })

  })

  describe('for creating asynchronously', function() {

    it('can create from JSON', function() {
      var json = this.jsonObj()
      this.EntityInfo.asyncCreate(json)
        .then((entityInfo) => {
          expect(entityInfo.id).toBe(json.id)
          expect(entityInfo.name).toBe(json.name)
        })
        .catch(() => {
          fail('should not invoked')
        })
      this.$rootScope.$digest()
    })

    it('fails when required fields are missing', function() {
      this.missingFieldsTestAsync(this.EntityInfo.SCHEMA.required,
                                  (requiredField) => _.omit(this.jsonObj(), requiredField),
                                  this.EntityInfo.asyncCreate)
      this.$rootScope.$digest()
    })

  })

})