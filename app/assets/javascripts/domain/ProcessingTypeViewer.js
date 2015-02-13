define(['./module'], function(module) {
  'use strict';

  module.factory('ProcessingTypeViewer', ProcessingTypeViewerFactory);

  ProcessingTypeViewerFactory.$inject = ['EntityViewer'];

  /**
   * Displays a processing type in a modal. The information is displayed in an ng-table.
   */
  function ProcessingTypeViewerFactory(EntityViewer) {

    function ProcessingTypeViewer(processingType) {
      var entityViewer = new EntityViewer(processingType, 'Processing Type');

      entityViewer.addAttribute('Name', processingType.name);
      entityViewer.addAttribute('Description', processingType.description);
      entityViewer.addAttribute('Enabled', processingType.enabled ? 'Yes' : 'No');

      entityViewer.showModal();
    }

    return ProcessingTypeViewer;
  }

});
