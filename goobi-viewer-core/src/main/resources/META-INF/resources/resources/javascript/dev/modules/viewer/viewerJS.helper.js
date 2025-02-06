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
 * Module which includes mostly used helper functions.
 * 
 * @version 3.2.0
 * @module viewerJS.helper
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
        
    // default variables
    var _debug = false;
    
    viewer.helper = {
        /**
         * saveSidebarTocPosition Method to truncate a string to a given length.
         * @method truncateString
         * @param {String} str The string to truncate.
         * @param {Number} size The number of characters after the string should be
         * croped.
         * @returns {String} The truncated string.
         * @example
         * 
         * <pre>
         * viewerJS.helper.truncateString( $( '.something' ).text(), 75 );
         * </pre>
         */
        truncateString: function( str, size ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.truncateString() ----------' );
                console.log( 'viewer.helper.truncateString: str = ', str );
                console.log( 'viewer.helper.truncateString: size = ', size );
            }
            
            var strSize = parseInt( str.length );
            
            if ( strSize > size ) {
                return str.substring( 0, size ) + '...';
            }
            else {
                return str;
            }
        },
        /**
         * @description Returns an JSON object from a API call. 
         * @method getRemoteData
         * @param {String} url The API call URL.
         * @returns {Object} A promise object, which tells about the success of receiving
         * data.
         * @example
         * 
         * <pre>
         * viewerJS.helper.getRemoteData( dataURL );
         * </pre>
         */
        getRemoteData: function( url ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.getRemoteData() ----------' );
                console.log( 'viewer.helper.getRemoteData: url = ', url );
            }
            
            //alternative using fetch
            /*var promise = fetch(decodeURI(url),  {
            	headers: {
				  'Content-Type': 'application/json'
				},
            })
            .then(resp => resp.json());*/
            
            
            var promise = $.ajax( {
                url: decodeURI( url ),
                type: "GET",
                dataType: "JSON",
                async: true
            } );
            
            return promise;
        },
        /**
         * @description Returns a BS Modal with dynamic content.
         * @method renderModal
         * @param {Object} config An config object which includes the content of the
         * modal.
         * @param {String} config.id The ID of the modal.
         * @param {String} config.label The label of the modal.
         * @param {Object} config.string An object of strings for the modal content.
         * @param {String} config.string.title The title of the modal.
         * @param {String} config.string.body The content of the modal as HTML.
         * @param {String} config.string.closeBtn Buttontext
         * @param {String} config.string.saveBtn Buttontext
         * @returns {String} A HTML-String which renders the modal.
         */
        renderModal: function( config ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.renderModal() ----------' );
                console.log( 'viewer.helper.renderModal: config = ', config );
            }
            var _defaults = {
                id: 'myModal',
                label: 'myModalLabel',
                closeId: 'closeModal',
                submitId: 'submitModal',
                string: {
                    title: 'Modal title',
                    body: '',
                    closeBtn: 'Close',
                    saveBtn: 'Save changes',
                }
            };
            
            $.extend( true, _defaults, config );
            var modal = '';
            
            modal += '<div class="modal fade" id="' + _defaults.id + '" tabindex="-1" role="dialog" aria-labelledby="' + _defaults.label + '">';
            modal += '<div class="modal-dialog" role="document">';
            modal += '<div class="modal-content">';
            modal += '<div class="modal-header">';
            modal += '<h3 class="modal-title" id="' + _defaults.label + '">' + _defaults.string.title + '</h3>';
            modal += '<button type="button" class="fancy-close" data-dismiss="modal" aria-label="' + _defaults.string.closeBtn + '">';
            modal += '<span aria-hidden="true">x</span>';
            modal += '</button>';
            modal += '</div>';
            modal += '<div class="modal-body">' + _defaults.string.body + '</div>';
            modal += '<div class="modal-footer">';
            modal += '<button type="button" id="' + _defaults.closeId + '"  class="btn" data-dismiss="modal">' + _defaults.string.closeBtn + '</button>';
            modal += '<button type="button" id="' + _defaults.submitId + '" class="btn btn--success">' + _defaults.string.saveBtn + '</button>';
            modal += '</div></div></div></div>';
            
            return modal;
        },

        /**
         * @description Method to return a BS Alert with dynamic content.
         * @method renderAlert
         * @param {String} type The type of the alert.
         * @param {String} content The content of the alert.
         * @param {Boolean} dismissable Sets the option to make the alert dismissable,
         * true = dismissable.
         * @returns {String} A HTML-String which renders the alert.
         */
        renderAlert: function( type, content, dismissable ) {
            if ( _debug ) {
                console.log( '---------- viewer.helper.renderAlert() ----------' );
                console.log( 'viewer.helper.renderAlert: type = ', type );
                console.log( 'viewer.helper.renderAlert: content = ', content );
                console.log( 'viewer.helper.renderAlert: dismissable = ', dismissable );
            }
            var bsAlert = '';
            
            bsAlert += '<div role="alert" class="alert ' + type + ' alert-dismissible fade in show">';
            if ( dismissable ) {
                bsAlert += '<button aria-label="Close" data-dismiss="alert" class="close" type="button"><span aria-hidden="true">×</span></button>';
            }
            bsAlert += content;
            bsAlert += '</div>';
            
            return bsAlert;
        },
        /**
         * @description Method to get the version number of the used MS Internet Explorer.
         * @method detectIEVersion
         * @returns {Number} The browser version.
         */
        detectIEVersion: function() {
            var ua = window.navigator.userAgent;
            
            // IE 10 and older
            var msie = ua.indexOf( 'MSIE ' );
            if ( msie > 0 ) {
                // IE 10 or older => return version number
                return parseInt( ua.substring( msie + 5, ua.indexOf( '.', msie ) ), 10 );
            }
            
            // IE 11
            var trident = ua.indexOf( 'Trident/' );
            if ( trident > 0 ) {
                // IE 11 => return version number
                var rv = ua.indexOf( 'rv:' );
                return parseInt( ua.substring( rv + 3, ua.indexOf( '.', rv ) ), 10 );
            }
            
            // IE 12+
            var edge = ua.indexOf( 'Edge/' );
            if ( edge > 0 ) {
                // Edge (IE 12+) => return version number
                return parseInt( ua.substring( edge + 5, ua.indexOf( '.', edge ) ), 10 );
            }
            
            // other browser
            return false;
        },        
        /**
         * @description Method to check if it´s possible to write to local Storage.
         * @method checkLocalStorage
         * @returns {Boolean} true or false
         */
        checkLocalStorage: function() {
            if ( typeof localStorage === 'object' ) {
                try {
                    sessionStorage.setItem( 'testLocalStorage', 1 );
                    sessionStorage.removeItem( 'testLocalStorage' );
                    
                    return true;
                }
                catch ( error ) {
                    console.error( 'Not possible to write in local Storage: ', error );
                    
                    return false;
                }
            }
        },        
        /**
         * @description Method to render a warning popover.
         * @method renderWarningPopover
         * @param {String} msg The message to show in the popover.
         * @returns {Object} An jQuery Object to append to DOM.
         */
        renderWarningPopover: function( msg ) {
            var popover = $( '<div />' );
            var popoverText = $( '<p />' );
            var popoverButton = $( '<button />' );
            var popoverButtonIcon = $( '<i aria-hidden="true" />' );
            
            popover.addClass( 'warning-popover' );
            
            // build button
            popoverButton.addClass( 'btn btn--clean' );
            popoverButton.attr( 'data-toggle', 'warning-popover' );
            popoverButtonIcon.addClass( 'fa fa-times' );
            popoverButton.append( popoverButtonIcon );
            popover.append( popoverButton );
            
            // build text
            popoverText.html( msg );
            popover.append( popoverText );
            
            return popover;
        },
        /**
         * @description Method to validate the reCAPTCHA response.
         * @method validateReCaptcha
         * @param {String} wrapper The reCAPTCHA widget wrapper.
         * @param {String} key The reCAPTCHA site key.
         * @returns {Boolean} Returns true if the response is valid.
         */
        validateReCaptcha: function( wrapper, key ) {
            var widget = grecaptcha.render( wrapper, {
                sitekey: key,
                callback: function() {
                    var response = grecaptcha.getResponse( widget );
                    
                    if ( response == 0 ) {
                        return false;
                    }
                    else {
                        return true;
                    }
                }
            } );
        },        
        /**
         * @description Method to initialize Bootstrap features (tooltips).
         * @method initBsFeatures
         */
        initBsFeatures: function() {
        	if ( _debug ) {
        	    console.log( 'EXECUTE: viewerJS.helper.initBsFeatures' );
            }
        	
//        	// (re)-enable BS tooltips
//        	$( '.tooltip' ).remove();
//	    	$( '[data-toggle="tooltip"]' ).tooltip('dispose');
//            $( '[data-toggle="tooltip"]' ).tooltip( {
//                trigger : 'hover'
//            } );
            
            // (re)-enable BS tooltips
            
			/* The manually determine when an item should show and hide a tool tip. */
			$('[data-toggle="tooltip"]').tooltip({ trigger: "manual" })
			.on( "mouseenter.tooltip", event => {
				$('[data-toggle="tooltip"]').tooltip("hide");
				$(event.currentTarget).tooltip("show");
				$(".tooltip").on("mouseleave", function() {
					$(event.currentTarget).tooltip("hide");
				})
			
			})
			.on('mouseleave.tooltip', event => {
				setTimeout(() => {
					if (!$(".tooltip:hover").length) $(event.currentTarget).tooltip("hide");
				}, 100);
			})
			
			// show tooltips on (keyboard) focus
			.focus(event => {
				$(event.currentTarget).tooltip("show");
			})
			.blur(event => {
				$(event.currentTarget).tooltip("hide");
			});

			/* Listen for the "escape key" so tool tips can easily be hidden */
			$("body").keydown(event => {
			  if (event.keyCode === 27) {
			    $('[data-toggle="tooltip"]').tooltip("hide");
			  }
			});
            
            
            if ( window.matchMedia( '(max-width: 768px)' ).matches ) {
            	$( '[data-toggle="tooltip"]' ).tooltip( 'dispose' );
            }
            
            

            // enable bootstrap popovers
            $( '[data-toggle="popover"]' ).popover( {
            	placement: 'bottom',
	            trigger: 'hover',
	            html: true
            } );

			// append all bootstrap modals to body
			  $('.modal').not('.user-login-modal').appendTo("body");
	
			// remove tooltips for deactivated admin cms move order buttons
	    	$( document ).ready(function() {
				$( 'button[class^="admin__content-component-order-arrow"][disabled="disabled"][data-toggle="tooltip"]' ).tooltip('dispose');
	    	});

        },
        
        initNumberOnlyInput: function() {
            $("body").on("keypress", "[data-input='number']", (event) => {
                if(!validateNumber(event)) {
                    event.preventDefault();
                }
            })
            
            function validateNumber(event) {
                var key = window.event ? event.keyCode : event.which;
                if (event.keyCode === 8 || event.keyCode === 46 || event.keyCode === 13) {
                    return true;
                } else if ( key < 48 || key > 57 ) {
                    return false;
                } else {
                    return true;
                }
            };
        },
        
        /**
         * @description Method to get the current year.
         * @method getCurrentYear
         * @returns {Number} The current year.
         * */
        getCurrentYear: function() {
        	if ( _debug ) {
                console.log( 'EXECUTE: viewerJS.helper.getCurrentYear' );
            }
        	
        	return new Date().getFullYear(); 
        },
        /**
         * Get css style values based on one or more class names
         * 
         * @param classes   A string containing all class names determining the styles
         * @param styles    An array containing all style attributes to get
         * @return  An object containing the requested styles
         */
        getCss: function(classes, styles) {
            let $template = $("<span class='"+classes+"'/>");
            $template.appendTo($("body"));
            let values = $template.css(styles);
            $template.remove();
            return values;
        },
        
        getFragmentHash: function( ) {
            let coordsRegex = /xywh=((?:percent:)?[\d\.\-\+]+,[\d\.\-\+]+,[\d\.\-\+]+,[\d\.\-\+]+)/;
            let hash = window.location.hash;
            let fragments = [];
            if(hash) {
                let match;// = fragment.match(coordsRegex);
                let count = 0;
                while ((match = coordsRegex.exec(hash)) && count++ < 100) { 
                    let coords = match[1];
                    fragments.push(coords);
                    hash = hash.replace(match[0], "");
                }
            }
            if(fragments.length == 1) {
                return fragments[0];
            } else if(fragments.length > 1) {
                return fragments;
            } else {
                return undefined;
            }
        },
        
        executeFunctionByName: function(functionName, context /*, args */) {
            var args = Array.prototype.slice.call(arguments, 2);
            var namespaces = functionName.split(".");
            var func = namespaces.pop();
            for(var i = 0; i < namespaces.length; i++) {
              context = context[namespaces[i]];
            }
            return context[func].apply(context, args);
         },
         getFunctionByName: function(functionName, context) {
             var namespaces = functionName.split(".");
             var func = namespaces.pop();
             for(var i = 0; i < namespaces.length; i++) {
               context = context[namespaces[i]];
             }
             return context[func];
          },
          getUrlSearchParamMap: function() {
              let searchParams = document.location.search.substr(1).split('&').filter(p => p != undefined && p.length > 0);
              let paramMap = new Map();
              searchParams.forEach(param => {
                  let parts = param.split("=");
                  paramMap.set(parts[0], parts[1]);
              })
              return paramMap;
          },
          getUrlSearchParam: function(key) {
          	return this.getUrlSearchParamMap().get(key);
          },
          setUrlSearchParams: function(map) {
              let paramList = [];
              map.forEach((value, key) => {
                  paramList.push(key + "=" + value);
              })
              document.location.search = paramList.join("&");
          }
         

    };
    
    viewer.localStoragePossible = viewer.helper.checkLocalStorage();
    
    viewer.setUrlQuery = function(param, value) {
        let paramMap = viewer.helper.getUrlSearchParamMap();
        if(!value || value.length == 0) {
            paramMap.delete(param);
        } else {
            paramMap.set(param, value);
        }
        viewer.helper.setUrlSearchParams(paramMap);
        return true;
    }
    
    viewer.setUrlHash = function(hash) {
        document.location.hash = "#" + hash;
        return true;
    }
    
    viewer.getMapBoxToken = function() {
        if(typeof this.mapBoxConfig != "undefined" && typeof this.mapBoxConfig.token != "undefined") {
            return this.mapBoxConfig.token;
        } else if(typeof mapBoxToken != "undefined") {
            return mapBoxToken;
        } else {
            return undefined; 
        }
    }
        
    viewer.getMetadataValue = function(object, language) {
        if(typeof object === "string") {
            return object;
        } else if(Array.isArray(object) && object.length > 0 && typeof object[0] === "string") {
            return object.join(" ");
        }
        return viewer.getOrElse([language, 0], object);
    }   

    viewer.getOrElse = function(p, o) {  
        var reducer = function(xs, x) {
            return (xs && xs[x]) ? xs[x] : ((xs && xs[Object.keys(xs)[0]]) ? xs[Object.keys(xs)[0]] : null);
        }
        return p.reduce(reducer , o);
    };
    
    /**
     * Check if a variable is a string
     */
    viewer.isString = function(variable) {
        return typeof variable === 'string' || variable instanceof String
    }
    
    if(!Array.prototype.includes) {
        Array.prototype.includes = function(element) {
            for ( var int = 0; int < this.length; int++ ) {
                var listEle = this[int];
                if(listEle == element) {
                    return true;
                }
            }
            return false;
        }
    }
    
    if(!Array.prototype.flatMap) {
        Array.prototype.flatMap = function(callback) {
            var res = [];
            for ( var int = 0; int < this.length; int++ ) {
                var listEle = this[int];
                let value = callback(listEle, int, this);
                res.push(value);
            }
            return res;
        }
    }
    
    /**
     * Filter function to keep only unique values in arrays.
     * Use as array.filter(viewerJS.unique);
     */
    viewer.unique = (value, index, self) => {
        return self.indexOf(value) === index;
    } 
    
    /**
    * Create a map from the given object by using the property names as keys and their values as values
    * a missing or empty object results in an empty map
    */
    viewer.parseMap = (object) => {
    	let map = new Map();
    	if(object) {
    		Object.keys(object).forEach(key => {
    			map.set(key, object[key]);
    		});
    	}
    	return map;
    }
 

	viewer.helper.compareNumerical = (a, b) => {
	    let ia = parseInt(a);
	    let ib = parseInt(b);
	    if(isNaN(ia) && isNaN(ib)) {
	        return 0;
	    } else if(isNaN(ia)) {
	        return 1;
	    } else if(isNaN(ib)) {
	    	return -1;
	    } else {
	    	return ia-ib;
	    }
	}
	
	viewer.helper.compareAlphanumerical = (a, b) => {
	    if(a && b) {
	        return a.localeCompare(b);
	    } else if(a) {
	        return 1;
	    } else {
	        return -1;
	    }
	} 

	viewer.helper.initRestrictedInputFields = () => {
		let $inputs = $("[data-input-restricted='url']");
		$inputs.each( (index, element) => {
			let notAllowedMessage = $(element).attr("data-input-restricted-message");
			$(element).on("keypress", (e) => {
				let character = e.originalEvent.key;
				if(character) { 
					if(character.match(/[\s#?]/)) {
						e.preventDefault();
						let message = notAllowedMessage.replace("{}", character);
						viewer.swaltoasts.error(message);
					}
				}
			});
		});
	}
	
	/**
	 * Creates a repeating cancellable promise with a specified delay between repetitions.
	 *
	 * @param {Promise} promise - The promise to repeat.
	 * @param {number} delay - The delay (in milliseconds) between repetitions. The delay timer starts after a promise is returned
	 * @returns {object} - An object with methods for cancellation and result handling.
	 */
	viewer.helper.repeatPromise = (promise, delay) => {
		const cancelSubject = new rxjs.Subject();
		const observable = rxjs.of(null).pipe(rxjs.operators.flatMap(promise), rxjs.operators.delay(delay), rxjs.operators.repeat(), rxjs.operators.takeUntil(cancelSubject));
		return {
			/**
		     * Cancels the repeating promise, preventing further repetitions.
		     */
			cancel: () => cancelSubject.next(),
			/**
		     * Subscribes to the observable and executes the provided function
		     * with the result of the promise each time it resolves.
		     *
		     * @param {function} f - The function to execute with the result of the promise.
		     */
			then: f => observable.subscribe(result => f(result))
		}
	}

viewer.helper.ArrayComparator = function(array, includeUnsortedBefore, valueFunction) {
    this.comparatorArray = array;
    this.notIncludedOrdering = includeUnsortedBefore == true ? 1 : (includeUnsortedBefore == false ? -1 : 0);
    this.valueFunction = valueFunction == undefined ? (x => x) : valueFunction;
    this.compare = (a,b) => {
        let valA = this.valueFunction(a);
        let valB = this.valueFunction(b);
        if(this.comparatorArray.includes(valA) && this.comparatorArray.includes(valB)) {
            return this.comparatorArray.indexOf(valA) - this.comparatorArray.indexOf(valB);
        } else if(this.comparatorArray.includes(valA)) {
            return this.notIncludedOrdering;
        } else if (this.comparatorArray.includes(valB)) {
            return -this.notIncludedOrdering;
        } else {
            return valA > valB ? 1 : (valB > valA ? -1 : 0);
        }
    }
}

    
    return viewer;
    
} )( viewerJS || {}, jQuery );
