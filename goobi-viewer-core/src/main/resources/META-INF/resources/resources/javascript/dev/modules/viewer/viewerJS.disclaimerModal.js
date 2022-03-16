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
 * @description Module which handles the disclaimer modal. 
 * @version 3.4.0
 * @module viewerJS.disclaimerModal
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _defaults = {
    	lastEdited: '',
    	active : false,
    	storage : 'local',
    	daysToLive : '14',
    };
    
    viewer.disclaimerModal = {
        /**
         * Method to initialize the cookie banner.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.disclaimerModal.init' );
                console.log( '##############################' );
                console.log( 'viewer.disclaimerModal.init: config - ', config );
            }
            
            this.config = $.extend(true, {}, _defaults, config );
            if(_debug)console.log("init disclaimer modal with config", this.config);
            if(this.config.active) {
            	this.settings = this.getStoredSettings(this.config.storage);
            	if(this.settings.lastAccepted) {
            		let lastEditedDate = new Date(this.config.lastEdited);
            		let lastAcceptedDate = new Date(this.settings.lastAccepted);
            		if(_debug)console.log("disclaimer last edited ", lastEditedDate);
            		if(_debug)console.log("disclaimer last accepted ", lastAcceptedDate);
            		if(_debug)console.log("disclaimer valid for session ", this.settings.sessionId, this.config.sessionId);
            		if(this.config.storage.toLowerCase() === 'session' && this.config.sessionId !== this.settings.sessionId) { //disclaimer valid for session and stored session id differs from current session
            			this.showDisclaimer();
            		} else if(this.settings.lastAccepted < this.config.lastEdited) { //accepted disclaimer before last disclaimer update --> need to accept again
            			this.showDisclaimer();
            		} else { //check if daysToLive days have passed since last edited
            			let timeoutDate = new Date(this.settings.lastAccepted);
            			timeoutDate.setDate(timeoutDate.getDate() + this.config.daysToLive);
            			if(_debug)console.log("disclaimer timeout date", timeoutDate, timeoutDate.getTime(), Date.now());
            			if(timeoutDate.getTime() < Date.now()) { //now is later than the timeout day of the disclaimer --> need to accept again
            				this.showDisclaimer();
            			}
            		}
            	} else {	//either the disclaimer has never been shown in this browser or within this session
            		this.showDisclaimer();
            	}
			}
        },
        showDisclaimer() {
        	Swal.fire({
        		html : this.config.disclaimerText,
        		allowOutsideClick: false,
        		customClass: {
        			popup: 'disclaimer-modal__alert',
			    	confirmButton: 'btn btn--full'
			    },
				buttonsStyling: false,
        	})
        	.then(() => {
        		// console.log("accepted disclaimer");
        		this.setStoredSettings({lastAccepted : Date.now(), sessionId: this.config.sessionId}, this.config.storage);        		
        	});
        	
        },
        getStoredSettings(location) {
        	let string = undefined;
        	if(location && location.toLowerCase() === 'session') {
        		string = sessionStorage.getItem("goobi.viewer.disclaimer.settings");
        	} else {
        		string = localStorage.getItem("goobi.viewer.disclaimer.settings");
        	}
        	if(_debug)console.log("retrieve disclaimer setings ", string, " from ", location);
        	if(string) {
        		try {
	        		return JSON.parse(string);
				} catch(e) {
					console.error("Error loading disclaimer settings from " + location, e);
					return {};
				}        		
        	} else {
        		return {};
        	}
        },
        setStoredSettings(settings, location) {
        	if(_debug)console.log("save disclaimer setings ", settings, " to ", location);
        	if(location && location.toLowerCase() === 'session') {
        		sessionStorage.setItem("goobi.viewer.disclaimer.settings", JSON.stringify(settings));
        	} else {
        		localStorage.setItem("goobi.viewer.disclaimer.settings", JSON.stringify(settings));
        	}
        },
	    
    };
    

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
