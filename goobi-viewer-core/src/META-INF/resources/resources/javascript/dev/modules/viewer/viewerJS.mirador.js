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
 * Module which manages the download view.
 * 
 * @version 3.4.0
 * @module viewerJS.mirador
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _sessionBookmarkList = '';
    var _defaults = {
    	root: '',
    	restEndpoint: undefined,
    	userLoggedIn: false,
    };
    
    var dataObject = {
        location: "Goobi viewer" 
    }
    var windowObject = {
        bottomPanel: false,
        sidePanel: true,
        sidePanelVisible: false,
        viewType: "ImageView"                            
    }
    
    var messageKeys = ["viewMirador", "viewMiradorComparison"];

    viewer.mirador = {
        /**
         * Method to initialize the mirador viewer.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.mirador.init' );
                console.log( '##############################' );
                console.log( 'viewer.mirador.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            if(!_defaults.restEndpoint) {
                _defaults.restEndpoint = _defaults.root + "/rest/"
            }
            
            var manifests = _getManifestsFromUrlQuery(_defaults);
            
            var translator = new viewerJS.Translator(
                    messageKeys, _defaults.restEndpoint, 
                    "#{navigationHelper.localeString}");
            
            if (manifests.length > 0) {
            	// URL identifiers
                console.log("manifests", manifests);
                var miradorConfig = _getMiradorConfigForManifestUrls(manifests, _defaults);
                console.log("miradorConfig", miradorConfig);
                Mirador( miradorConfig );
                // Override window title if more than one record
                translator.init()
                .then(function() {
                	if (miradorConfig.data.length > 1) {
                		document.title = translator.translate('viewMiradorComparison');
                	}
                });
            } else 
            // check login status
            // logged in
            if ( _defaults.userLoggedIn ) {
            	// User bookmarks
                _sessionBookmarkList = sessionStorage.getItem( 'bookmarkListId' );
                _getMiradorObjects( _defaults.root, _sessionBookmarkList ).then( function( elements ) {                    
                    $( function() {
                        console.log("elements ", elements)
                        Mirador( elements );
                        // Override window title if more than one record
                        translator.init()
                        .then(function() {
                        	if (elements.data.length > 1) {
                        		document.title = translator.translate('viewMiradorComparison');
                        	}
                        });
                    });
                }).fail(function(error) {
                    console.error('ERROR - _getMiradorObjects: ', error.responseText);
                });
            }
            // not logged in
            else {
            	// Session mark list
                _getMiradorSessionObjects( _defaults.root ).then( function( elements ) {                    
                    $( function() {
                    	 console.log("elements ", elements)
                        Mirador( elements );
                    	// Override window title if more than one record
                        translator.init()
                        .then(function() {
                        	if (elements.data.length > 1) {
                        		document.title = translator.translate('viewMiradorComparison');
                        	}
                        });
                    });
                }).fail(function(error) {
                    console.error('ERROR - _getMiradorSessionObjects: ', error.responseText);
                });
            }
        }
    };
    
    /* ######## GET (READ) ######## */
    /**
	 * Method to get the mirador objects.
	 * 
	 * @method _getMiradorObjects
	 * @param {String} root The application root path.
	 * @returns {Object} An JSON-Object which contains all session elements.
	 */
	function _getMiradorObjects( root, id ) {
		if ( _debug ) { 
			console.log( '---------- _getSessionElementCount() ----------' );
			console.log( '_getSessionElementCount: root - ', root );
			console.log( '_getSessionElementCount: id - ', id );
		}

		var promise = Q($.ajax({
			url : root + '/rest/bookmarks/user/mirador/' + id + '/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
	}
	/**
	 * Method to get the mirador session objects.
	 * 
	 * @method _getMiradorSessionObjects
	 * @param {String} root The application root path.
	 * @returns {Object} An JSON-Object which contains all session elements.
	 */
	function _getMiradorSessionObjects( root ) {
		if ( _debug ) { 
			console.log( '---------- _getMiradorSessionObjects() ----------' );
			console.log( '_getMiradorSessionObjects: root - ', root );
		}

		var promise = Q($.ajax({
			url : root + '/rest/bookmarks/session/mirador/',
			type : "GET",
			dataType : "JSON",
			async : true
		}));

		return promise
	}

	
	function _getManifestsFromUrlQuery(_defaults) {
        var manifests = _getQueryVariable("manifest").split("$").filter(man => man.length > 0);
        var pis = _getQueryVariable("pi").split("$").filter(man => man.length > 0);
        var piManifests = pis.map(pi => _defaults.restEndpoint + "iiif/manifests/" + pi + "/manifest");
        manifests = manifests.concat(piManifests);
        return manifests;
	}
	
	function _getQueryVariable(variable) {
	       var query = window.location.search.substring(1);
	       var vars = query.split("&");
	       for (var i=0;i<vars.length;i++) {
	               var pair = vars[i].split("=");
	               if(pair[0] == variable){return pair[1];}
	       }
	       return '';
	}
	
	   
    function _getMiradorConfigForManifestUrls(manifests, _defaults) {
        var columns = Math.ceil(Math.sqrt(manifests.length));
        var rows = Math.ceil(manifests.length/columns);
        
        var miradorConfig = {
                buildPath : _defaults.root + "/resources/javascript/libs/mirador/",
                id: "miradorViewer",
                layout: rows + "x" + columns,
                data: manifests.map(man => {
                    var dataObj = Object.assign({}, dataObject);
                    dataObj.manifestUri = man;
                    return dataObj;
                }),
                windowObjects: manifests.map(man => {
                    var winObj = Object.assign({}, windowObject);
                    winObj.loadedManifest = man;
                    return winObj;
                })
        }
        return miradorConfig;
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
