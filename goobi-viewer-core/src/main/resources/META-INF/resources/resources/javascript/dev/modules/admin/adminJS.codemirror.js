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
    
    var _debug = false;

    admin.codemirror = {
        /**
         * @description Method which initializes the codemirror editor in the backend.
         * @method init
         */
        init: function() {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.codemirror.init' );
                console.log( '##############################' );
            }
           
			var configFileTextArea;
			var type;
			var configFileEditor;
			var readOnly;
			var selected_row;
			var activeLineToggler;
			var cmEditor;
			//var nightMode;
			var theme;
			
			
			function initTextArea() {
				
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
				cmEditor = CodeMirror.fromTextArea(targetTextArea, {
						lineNumbers: true,
						mode: type,
						theme: theme,
						readOnly: readOnly,
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
								document.querySelector('[data-cm="save"]').click();
							},
							"Ctrl-E": "findPersistent",
						}
				});
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
			
			}; 
			
			function setEditable(editable, number, isButton) {
				selected_row = document.getElementById('row'+number);
				
				readOnly = !editable;
				if (readOnly && isButton) {
					alert("Die Rechte im Dateisystem müssen korrigiert werden um diese Datei zu bearbeiten.");
				}
			}
			
			initTextArea();

        }
	}
	
	return admin;
    
} )( adminJS || {}, jQuery );