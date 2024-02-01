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
    var _debug = false;
    var _sessionBookmarkList = '';
    var _defaults = {
    	root: '',
    	restEndpoint: undefined,
    	manifestEndpoint: undefined,
    	userLoggedIn: false,
    	bookmarkListId: null,
    };
    
    var dataObject = {
        location: "Goobi viewer" 
    }
    var windowObject = {
        thumbnailNavigationPosition: 'far-bottom',    
        canvasIndex: 0,
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
            
            this.config = $.extend( true, {}, _defaults, config );
            if(!this.config.manifestEndpoint) {
            	this.config.manifestEndpoint = this.config.restEndpoint;
            }
            
            var manifests = _getManifestsFromUrlQuery(this.config);
            
            var translator = new viewerJS.Translator(this.config.restEndpoint, "#{navigationHelper.localeString}");
            
            let miradorConfigPromise = null;
            if (manifests.length > 0) {
            	// URL identifiers
				console.log("loading manifests ", manifests);
                miradorConfigPromise = _getMiradorConfigForManifestUrls(manifests, this.config);
            } else if ( _getBookmarkListId() != null ) {
            	// User bookmarks
            	console.log("Load bookmarklist ", _getBookmarkListId())
                miradorConfigPromise = _getUserMiradorObjects( this.config.restEndpoint, _getBookmarkListId() )
                .then( response => response.json())
                .then( json => json.members.filter(manifest => manifest["@type"] == "sc:Manifest" || manifest.type == "Manifest") )
                //.then( members => members.map(manifest => manifest["@id"] ? manifest["@id"] : manifest.id).filter(id => id != undefined) )
                .then( members => _getMiradorConfigForManifestUrls(members, this.config) );
            } else if ( _getBookmarkListKey() != null ) {
                //public/shared bookmarks
                console.log("Load bookmarklist with key ", _getBookmarkListKey())
                miradorConfigPromise = _getSharedMiradorObjects( this.config.restEndpoint, _getBookmarkListKey() )
                .then( response => response.json())
                .then( json => json.members.filter(manifest => manifest["@type"] == "sc:Manifest" || manifest.type == "Manifest") )
//                .then( members => members.map(manifest => manifest["@id"] ? manifest["@id"] : manifest.id).filter(id => id != undefined) )
                .then( ids => _getMiradorConfigForManifestUrls(ids, this.config) );
            } else if(_getBookmarkListId() !== null) {
            	// Session mark list
            	console.log("load session bookmark list")
                miradorConfigPromise = _getMiradorSessionObjects( this.config.restEndpoint )
                .then( response => response.json())
                .then( json => json.members.filter(manifest => manifest["@type"] == "sc:Manifest" || manifest.type == "Manifest") )
                //.then( members => members.map(manifest => manifest["@id"] ? manifest["@id"] : manifest.id).filter(id => id != undefined) )
                .then( ids => _getMiradorConfigForManifestUrls(ids, this.config) );
            } else {
				console.log("TODO: Load empty Mirador");
				miradorConfigPromise = Promise.resolve({
					id: "miradorViewer"
				});
			}
            
            if(miradorConfigPromise) {  
                miradorConfigPromise       
                .then( elements => {            
						      
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
	function _getMiradorSessionObjects( restUrl ) {
		if ( _debug ) { 
			console.log( '---------- _getMiradorSessionObjects() ----------' );
			console.log( '_getMiradorSessionObjects: root - ', root );
		}
		
		let url = restUrl + "bookmarks/0/collection.json";
        return fetch(url);
	}

	
	function _getManifestsFromUrlQuery(config) {
        var manifests = _getQueryVariables("manifest");
        var pis = _getQueryVariables("pi");
        var piManifests = pis.map(pi => config.manifestEndpoint + "records/" + pi + "/manifest/");
        manifests = manifests.concat(piManifests);
        return manifests;
	}
	
	function _getQueryVariables(variable) {
		const params = new URLSearchParams(window.location.search);
		return params.getAll(variable).flatMap(s => s.split(/[,$]/)).filter(s => s && s.length > 0);
	}
	
	   
    function _getMiradorConfigForManifestUrls(manifests, config) {
        var columns = Math.ceil(Math.sqrt(manifests.length));
        var rows = Math.ceil(manifests.length/columns);
        var miradorConfig = { 
                id: "miradorViewer",
                manifests: manifests.map(man => {
                    var dataObj = Object.assign({}, dataObject);
                    dataObj.manifestUri = viewerJS.iiif.getId(man);
                    return dataObj;
                }),
                windows: manifests.map(man => {
                    var winObj = Object.assign({}, windowObject);
                    if(config.startPage) {
						winObj.canvasIndex = config.startPage-1;
					} else if(man.sequences && man.sequences.length > 0 && man.sequences[0].startCanvas) {
                    	let startId = man.sequences[0].startCanvas["@id"];
                    	let match = startId.match(/pages\/(\d+)\/canvas/);
                    	if(match && match.length > 1) {
                    		let pageNo = parseInt(match[1]);
                    		winObj.canvasIndex = pageNo-1;
                    	}
                    }
                    winObj.loadedManifest = viewerJS.iiif.getId(man);
                    return winObj;
                }),
                window: {
                	defaultView: 'single'
                },
                annotations: {  
             		//'sc:painting' and 'supplementing' excluded to hide fulltext annotations
             		//'oa:describing' must be included to display viewer (crowdsourcing) annotations
            	    filteredMotivations: ['oa:commenting', 'oa:tagging','oa:describing', 'commenting', 'tagging', 'describing'],
                }
        }
        return Promise.resolve(miradorConfig);
    }

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
