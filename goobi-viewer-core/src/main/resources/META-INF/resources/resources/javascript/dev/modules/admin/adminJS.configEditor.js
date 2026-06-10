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
 * @module adminJS.configEditor
 * @requires jQuery
 * @description Module for the page resources/admin/views/adminConfigEditor.xhtml
 */
var adminJS = ( function( admin ) {
    'use strict';
    
    const _debug = false;
    const _default = {
    	currentFileIsReadable: false,
    	currentFileIsWritable: false,
    	currentFilePath: undefined,
    }

    admin.configEditor = {
    	cmEditor: undefined,
    	dirty: false,
        /**
         * @description Method which initializes the codemirror editor in the backend.
         * @method init
         */
        init: function(config) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'adminJS.configEditor.init' );
                console.log( '##############################' );
            }
            
            this.config = $.extend(true, {}, _default, config);
            if(_debug) {console.log("configEditor config", this.config)};

			this.initTextArea();
			this.initOnBeforeUnload();
			this.initWebsocket();
			this.initTooltipHelpers();
        },
        initWebsocket: function() {
            // Only writable, actually-open files take an edit lock; reading must not lock the file for others.
            if (!this.config.currentFilePath || this.isReadOnly()) {
                return;
            }
            this._wantConnection = true;
            this._reconnectDelay = 1000;
            this._reconnectScheduled = false;
            this._connectWebsocket();
            // Teardown on real page hide (not beforeunload, which can be cancelled by the dirty dialog).
            // Register once even if init() somehow runs twice, so listeners do not accumulate.
            if (!this._pagehideRegistered) {
                window.addEventListener('pagehide', () => this._releaseAndTeardown());
                this._pagehideRegistered = true;
            }
        },
        _connectWebsocket: function() {
            this._reconnectScheduled = false;
            this._disconnectSocket(); // drop any previous instance so only one socket is ever live
            const socket = new viewerJS.WebSocket(window.location.host, window.currentPath, viewerJS.WebSocket.PATH_CONFIG_EDITOR_SOCKET);
            socket._closedByUs = false; // distinguishes intentional close (teardown/reconnect) from a real drop
            this.socket = socket;
            socket.onOpen.subscribe(() => {
                socket.sendMessage(JSON.stringify({ fileToLock: this.config.currentFilePath }));
                this._startHeartbeat();
                // Treat the connection as healthy only after it has held for STABLE_MS, then reset the backoff.
                this._stableTimer = setTimeout(() => { this._reconnectDelay = 1000; }, 5000);
            });
            socket.onMessage.subscribe((evt) => this._onLockStatus(evt));
            socket.onClose.subscribe((evt) => {
                // Ignore closes we triggered ourselves (teardown / reconnect-swap); otherwise the old instance's
                // late native close event would schedule a second, competing reconnect chain.
                if (socket._closedByUs) {
                    return;
                }
                this._stopHeartbeat();
                if (this._stableTimer) { clearTimeout(this._stableTimer); this._stableTimer = null; }
                const policyClose = evt && (evt.code === 1008);
                if (this._wantConnection && !policyClose && !this._reconnectScheduled) {
                    this._reconnectScheduled = true;
                    setTimeout(() => this._connectWebsocket(), this._reconnectDelay);
                    this._reconnectDelay = Math.min(this._reconnectDelay * 2, 10000);
                }
            });
            socket.onError.subscribe(() => {}); // errors surface as a close; nothing extra to do
        },
        _disconnectSocket: function() {
            this._stopHeartbeat();
            if (this._stableTimer) { clearTimeout(this._stableTimer); this._stableTimer = null; }
            if (this.socket) {
                this.socket._closedByUs = true; // suppress the reconnect that the resulting onClose would otherwise schedule
                try { this.socket.close(); } catch (e) { /* already closed */ }
                this.socket = undefined;
            }
        },
        _startHeartbeat: function() {
            this._stopHeartbeat();
            this._heartbeatTimer = setInterval(() => {
                if (this.socket && this.socket.isOpen()) {
                    this.socket.sendMessage(JSON.stringify({ heartbeat: true }));
                }
            }, 20000);
        },
        _stopHeartbeat: function() {
            if (this._heartbeatTimer) { clearInterval(this._heartbeatTimer); this._heartbeatTimer = null; }
        },
        _onLockStatus: function(evt) {
            var data;
            try { data = JSON.parse(evt.data); } catch (e) { return; }
            if (data && data.lockStatus === 'lost') {
                // Lock was lost while the page stayed open: make the editor read-only and warn, so edits are not lost silently.
                this._wantConnection = false;
                this._disconnectSocket();
                if (this.cmEditor) { this.cmEditor.setOption('readOnly', true); }
                var banner = document.getElementById('configEditorLockLost');
                if (banner) { banner.style.display = 'block'; }
            }
        },
        teardownWebsocket: function() {
            // Stop reconnecting and close; the server lease then expires via TTL.
            this._wantConnection = false;
            this._disconnectSocket();
        },
        _releaseAndTeardown: function() {
            // Real page unload (navigation/close): proactively release the lock so a file switch frees the old file
            // immediately instead of waiting for the TTL. Best-effort send before close; TTL is the fallback if the
            // frame does not make it out (e.g. a hard tab crash). Only sent here, never on idle/reconnect teardown.
            if (this.socket && this.socket.isOpen() && this.config.currentFilePath) {
                try { this.socket.sendMessage(JSON.stringify({ release: true })); } catch (e) { /* unload best-effort */ }
            }
            this.teardownWebsocket();
        },
        initOnBeforeUnload: function() {
            window.addEventListener('beforeunload', (event) => {
                if (this.dirty) {
                    event.returnValue = false;
                }
            });
        },
        isReadOnly: function() {
        	return this.config.currentFileIsReadable && !this.config.currentFileIsWritable;

        },
		initTooltipHelpers: function () {
			var _this = this;
			$(document).ready( () => {
				  $('.-isNotReadable').tooltip({title: _this.config.fileNotReadableMsgKey, placement: "top"});
			});
			
			$( ".admin__config-editor-backup-single-entry" ).hover(
					  function() {
					    $(this).find('.admin__config-editor-backup-single-entry-icon .fa-download').tooltip('show');
					  }, function() {
					    $(this).find('.admin__config-editor-backup-single-entry-icon .fa-download').tooltip('hide');
					  }
			);
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
				if (type == 'xsl') {
					type = "xml"; // use XML highlighting for .xsl files
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
				this.cmEditor = adminJS.codemirror(targetTextArea, type, this.isReadOnly(), {
					theme: theme,
					styleActiveLine: activeLineToggler,
				}).codemirror;
				
				// check if readOnly mode for current file should be active
				if (this.isReadOnly() == true) {
					this.cmEditor.setOption("readOnly", true);
				}
				
				if ( _debug ) {
					console.log("CodeMirror Editor constructed!");
				}
			
				// CLEAR EDITOR AND SHOW AN OVERLAY IF NO FILE SELECT
				if (fileTypeElement.innerHTML.trim() == '') {
					this.cmEditor.setValue("");
					this.cmEditor.clearHistory();
					$('[data-cm="overlay"]').show();
				}
				else {
					this.cmEditor.focus();
					$('[data-cm="overlay"]').hide();
				}
				// listen for CodeMirror changes
				var startEditorValue = this.cmEditor.getValue();
				var debounce = null;
				
				this.cmEditor.on('change', () => {
					// debounce for good performance
				   	clearTimeout(debounce);
					   debounce = setTimeout(() => {
						var newEditorValue = this.cmEditor.getValue();                  
						if ((this.cmEditor.doc.changeGeneration() == 1) || (startEditorValue == newEditorValue)) {
							if ( _debug ) {
								console.log('editor is clean');
							}
							this.dirty = false;
							 this.hideOverlayBar();
						} else {
							if ( _debug ) {
							console.log('editor not clean');
							}
							this.dirty = true;
							this.showOverlayBar();
						}
						if ( _debug ) {
							console.log('debounced');
						}
				   }, 350);               
				}); 
			
				// SAVE BUTTON FUNCTIONAL
					$('[data-cm="save"]').on('click', () => {
						this.cmEditor.save();
						this.dirty = false;
						if ( _debug ) {
							console.log('editor is saved by clicked button');
						}
					});
					
				// CANCEL BUTTON FUNCTION
				// RESETS ALL EDITS
				$('[data-cm="cancel"]').on('click', () => {
					var startContent = this.cmEditor.getTextArea().value;
					this.cmEditor.setValue(startContent);
					this.cmEditor.clearHistory();
				});
			
			},
		showOverlayBar: function(fixed) {
			$('.admin__overlay-bar').addClass('-slideIn');
			if(fixed) {
				$('.admin__overlay-bar').addClass('-fixed');
			}
		},
		hideOverlayBar: function() {
			if(!$('.admin__overlay-bar').hasClass("-fixed")) {
				$('.admin__overlay-bar').removeClass('-slideIn');
				$('.admin__overlay-bar').addClass('-slideOut');
				$('.admin__overlay-bar').on('animationend webkitAnimationEnd', function() { 
					$('.admin__overlay-bar').removeClass('-slideOut');
				});
			}
		},
		loadBackup: function(data) {
			console.log("load backup ", data.status);
			if(data.status === "success") {
				$(document).ready(() => {						
					setTimeout(adminJS.configEditor.showOverlayBar(true));
				})
			}
		}
	}
	
	return admin;
    
} )( adminJS || {}, jQuery );