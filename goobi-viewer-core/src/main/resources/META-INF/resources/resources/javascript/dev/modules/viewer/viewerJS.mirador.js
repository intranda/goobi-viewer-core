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
    
    var idPattern = /.*\/mirador\/id\/(\d+)\/?/i;
    var keyPattern = /.*\/mirador\/key\/(\w+)\/?/i;
    
    // variables
    var _debug = true;
    var _sessionBookmarkList = '';
    var _defaults = {
    	root: '',
    	restEndpoint: undefined,
    	userLoggedIn: false,
    	bookmarkListId: null,
    };
    
    var dataObject = {
        location: "Goobi viewer" 
    }
    var windowObject = {
        thumbnailNavigationPosition: 'far-bottom',    
        canvasIndex: 1      
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
            
            var manifests = _getManifestsFromUrlQuery(_defaults);
            
            var translator = new viewerJS.Translator(_defaults.restEndpoint, "#{navigationHelper.localeString}");
            
            let miradorConfigPromise = null;
            console.log("Manifests ", manifests);
            console.log("_getBookmarkListId()", _getBookmarkListId());
            if (manifests.length > 0) {
            	// URL identifiers
                miradorConfigPromise = _getMiradorConfigForManifestUrls(manifests, _defaults);
            } else if ( _getBookmarkListId() != null ) {
            	// User bookmarks
                miradorConfigPromise = _getUserMiradorObjects( _defaults.restEndpoint, _getBookmarkListId() )
                .then( response => response.json())
                .then( json => json.members.filter(manifest => manifest["@type"] == "sc:Manifest" || manifest.type == "Manifest") )
                .then( members => members.map(manifest => manifest["@id"] ? manifest["@id"] : manifest.id).filter(id => id != undefined) )
                .then( ids => _getMiradorConfigForManifestUrls(ids, _defaults) );
            } else if ( _getBookmarkListKey() != null ) {
                //public/shared bookmarks
                miradorConfigPromise = _getSharedMiradorObjects( _defaults.restEndpoint, _getBookmarkListKey() )
                .then( response => response.json())
                .then( json => json.members.filter(manifest => manifest["@type"] == "sc:Manifest" || manifest.type == "Manifest") )
                .then( members => members.map(manifest => manifest["@id"] ? manifest["@id"] : manifest.id).filter(id => id != undefined) )
                .then( ids => _getMiradorConfigForManifestUrls(ids, _defaults) );
            } else {
            	// Session mark list
                miradorConfigPromise = _getMiradorSessionObjects( _defaults.restEndpoint )
                .then( response => response.json())
                .then( json => json.members.filter(manifest => manifest["@type"] == "sc:Manifest" || manifest.type == "Manifest") )
                .then( members => members.map(manifest => manifest["@id"] ? manifest["@id"] : manifest.id).filter(id => id != undefined) )
                .then( ids => _getMiradorConfigForManifestUrls(ids, _defaults) );
            }
            
            if(miradorConfigPromise) {  
                miradorConfigPromise       
                .then( elements => {                    
                        console.log("elements ", elements)
                        this.miradorConfig = elements;
                        this.mirador = Mirador.viewer(this.miradorConfig);
                })
                .then(() => translator.init(messageKeys))
                .then( () => {
                    // Override window title if more than one record
                    if (this.miradorConfig.manifests.length > 1) {
                        document.title = translator.translate('viewMiradorComparison');
                    }
                }).catch(function(error) {
                    console.error('ERROR - _getMiradorObjects: ', error);
                });
            } else {
                console.error("ERROR - no manifests to load");
            }
        }
    };
    
    
    function _getBookmarkListId() {
        let location = window.location.href;
        if(location.match(idPattern)) {
            let id = location.match(idPattern)[1];
            return id;
        }
        return null;
    }
    
    function _getBookmarkListKey() {
        let location = window.location.href;
        if(location.match(keyPattern)) {
            let key = location.match(keyPattern)[1];
            return key;
        }
        return null;
    }
    
    /* ######## GET (READ) ######## */
    /**
	 * Method to get the mirador objects.
	 * 
	 * @method _getMiradorObjects
	 * @param {String} root The application root path.
	 * @returns {Object} An JSON-Object which contains all session elements.
	 */
	function _getUserMiradorObjects( restUrl, id ) {
		if ( _debug ) { 
			console.log( '---------- _getSessionElementCount() ----------' );
			console.log( '_getSessionElementCount: restUrl - ', restUrl );
			console.log( '_getSessionElementCount: id - ', id );
		}

		let url = restUrl + "bookmarks/" + id + "/collection.json";
		console.log("rest url ", url);
		return fetch(url);
	}
	
	   function _getSharedMiradorObjects( restUrl, key ) {
	        if ( _debug ) { 
	            console.log( '---------- _getSharedMiradorObjects() ----------' );
	            console.log( '_getSessionElementCount: restUrl - ', restUrl );
	            console.log( '_getSessionElementCount: id - ', key );
	        }

	        let url = restUrl + "bookmarks/shared/" + key + "/collection.json";
	        return fetch(url);
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
		
		let url = restUrl + "bookmarks/0/collection.json";
        return fetch(url);
	}

	
	function _getManifestsFromUrlQuery(_defaults) {
        var manifests = _getQueryVariable("manifest").split("$").filter(man => man.length > 0);
        var pis = _getQueryVariable("pi").split("$").filter(man => man.length > 0);
        var piManifests = pis.map(pi => _defaults.restEndpoint + "records/" + pi + "/manifest/");
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
                manifests: manifests.map(man => {
                    var dataObj = Object.assign({}, dataObject);
                    dataObj.manifestUri = man;
                    return dataObj;
                }),
                windows: manifests.map(man => {
                    var winObj = Object.assign({}, windowObject);
                    winObj.loadedManifest = man;
                    return winObj;
                }),
                annotations: {  
             		//'sc:painting' excluded to hide fulltext annotations
             		//'oa:describing' must be included to display viewer (crowdsourcing) annotations
            	    filteredMotivations: ['oa:commenting', 'oa:tagging','oa:describing', 'commenting', 'tagging'],
                }
        }
        return Q(Promise.resolve(miradorConfig));
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
