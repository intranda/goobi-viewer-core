/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 * 
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @version 22.08
 * @module adminJS.codemirror
 * @requires jQuery
 * @description Module for the page resources/admin/views/adminConfigEditor.xhtml
 */
var adminJS = ( function( admin ) {
    'use strict';
    
    const _debug = true;
    const _default = {
    	currentFileIsReadable: false,
    	currentFileIsWritable: false
    }

    admin.codemirror = {
        /**
         * @description Method which initializes the codemirror editor in the backend.
         * @method init
         */
        init: function(config) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.codemirror.init' );
                console.log( '##############################' );
            }
            
            this.config = $.extend(true, {}, _default, config);
            if(_debug) {console.log("codemirror config", this.config)};

			this.initTextArea();
			this.initOnBeforeUnload();

        },
        initOnBeforeUnload: function() {
        	window.onbeforeunload = () => {
        		console.log("unload page");
        		return false;
        	}
        },
        isReadOnly: function() {
        	return this.config.currentFileIsReadable && !this.config.currentFileIsWritable;
        },
        initTextArea: function() {
      
				var activeLineToggler;
				var type;
				var theme;
				
				// GET THE CURRENT FILE TYPE OF CHOSEN FILE
				let fileTypeElement = document.getElementById("currentConfigFileType");
				if ( _debug ) {
					console.info("Loaded file type = " + fileTypeElement.innerHTML.trim());
				}
				if (fileTypeElement !== null){
					type = fileTypeElement.innerHTML.trim(); // "properties" or "xml"
				} else {
					type = "xml";
				}
				if (typeof type == "undefined") {
					type = "xml";
				}
				if (typeof theme == "undefined") {
					theme = "default";
				}
				if ( _debug ) {
					console.log("type changed to = " + type);
				}
				// TARGETED TEXTAREA WITH CODE CONTENT
				var targetTextArea = document.getElementById('editor-form:editor');
				
				if (fileTypeElement.innerHTML.trim() == ''){
					activeLineToggler = false;
				} else {
					activeLineToggler = true;
				}
			
				// INIT EDITOR MAIN
				this.cmEditor = CodeMirror.fromTextArea(targetTextArea, {
						lineNumbers: true,
						mode: type,
						theme: theme,
						autofocus: false,
						indentUnit: 2,
						tabSize: 2,
						styleActiveLine: activeLineToggler,
						indentWithTabs: true,
						extraKeys: {
							"F11": function(cm) {
								cm.setOption("fullScreen", !cm.getOption("fullScreen"));
							},
							"Esc": function(cm) {
								if (cm.getOption("fullScreen")) {
									cm.setOption("fullScreen", false);
								}
							},
							"Ctrl-D": function(cm) {
								cm.setOption("theme", cm.getOption("theme") == "default" ? "dracula" : "default");
							},
							"Ctrl-S": function(cm) {
								if ( _debug ) {
									console.log('manually saved with key combo');
								}
								if (this.isReadOnly() == false) {
									document.querySelector('[data-cm="save"]').click();
								}
							},
							"Ctrl-E": "findPersistent",
						}
				});
				
				// check if readOnly mode for current file should be active
				if (this.isReadOnly() == true) {
					cmEditor.setOption("readOnly", true);
				}
				
				if ( _debug ) {
					console.log("CodeMirror Editor constructed!");
				}
			
				// CLEAR EDITOR AND SHOW AN OVERLAY IF NO FILE SELECT
				if (fileTypeElement.innerHTML.trim() == '') {
					cmEditor.setValue("");
					cmEditor.clearHistory();
					$('[data-cm="overlay"]').show();
				}
				else {
					cmEditor.focus();
					$('[data-cm="overlay"]').hide();
				}
				// listen for CodeMirror changes
				var startEditorValue = cmEditor.getValue();
				var debounce = null;
				
				cmEditor.on('change', function(){
					
					// debounce for good performance
				   	clearTimeout(debounce);
					   debounce = setTimeout(function(){
						var newEditorValue = cmEditor.getValue();                  
						if ((cmEditor.doc.changeGeneration() == 1) || (startEditorValue == newEditorValue)) {
							if ( _debug ) {
								console.log('editor is clean');
							}
							 $('.admin__overlay-bar').removeClass('-slideIn');
							 $('.admin__overlay-bar').addClass('-slideOut');
							 $('.admin__overlay-bar').on('animationend webkitAnimationEnd', function() { 
							    $('.admin__overlay-bar').removeClass('-slideOut');
							 });
						} else {
							if ( _debug ) {
							console.log('editor not clean');
							}
							 $('.admin__overlay-bar').addClass('-slideIn');
						}
						if ( _debug ) {
							console.log('debounced');
						}
				   }, 350);               
				}); 
			
				// SAVE BUTTON FUNCTIONAL
					$('[data-cm="save"]').on('click', function() {
						cmEditor.save();
						if ( _debug ) {
							console.log('editor is saved by clicked button');
						}
					});
					
				// CANCEL BUTTON FUNCTION
				// RESETS ALL EDITS
				$('[data-cm="cancel"]').on('click', function() {
					var startContent = cmEditor.getTextArea().value;
					cmEditor.setValue(startContent);
					cmEditor.clearHistory();
				});
			
			},
	}
	
	return admin;
    
} )( adminJS || {}, jQuery );