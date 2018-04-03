/**
 * Handle jQuery plugin naming conflict between jQuery UI and Bootstrap
 */
$.widget.bridge( 'uibutton', $.ui.button );
$.widget.bridge( 'uitooltip', $.ui.tooltip );
