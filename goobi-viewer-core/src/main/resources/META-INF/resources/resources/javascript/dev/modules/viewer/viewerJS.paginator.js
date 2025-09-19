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
 * Module to scroll a page back to top animated.
 * 
 * @version 3.2.0
 * @module viewerJS.paginator
 * @requires jQuery
 */
var viewerJS = (function(viewer) {
	'use strict';

	var _rightKey = 39;
	var _leftKey = 37;
	var _forwardKey;
    var _backwardKey;
	var _debug = false;
	var _defaults = {
		maxDoubleClickDelay : 250, // ms
		firstItem: 1,
		lastItem: 10000,
		currentItem: 0,
		targetUrlPrefix: "",
		targetUrlSuffix: ""
	}

	viewer.paginator = {
		
		

		/**
		 * Initializes keyboard bindings for paginator
		 * 
		 * @method init
		 * @param {String} obj The selector of the jQuery object.
		 * @param {String} anchor The name of the anchor to scroll to.
		 */
		init : function(config) {
			this.lastKeypress = 0;
			this.lastkeycode = 0;
			this.config = jQuery.extend(true, {}, _defaults); //copy defaults
			jQuery.extend(true, this.config, config); //merge config
			
			if (_debug) {
				console.log("Init paginator with config ", viewer.paginator.config);
			}

			if(this.config.rightToLeft === 'true') {
			    _forwardKey = _leftKey;
			    _backwardKey = _rightKey;
			} else {
			    _forwardKey = _rightKey;
                _backwardKey = _leftKey;
			}
			$(document.body).on("keyup", viewer.paginator.keypressHandler);
			
			$( document ).ready(function() {
			
			// Make the input field the same size as the text/numbers element
			var $numericPaginatorLabelWidth = $('[data-paginator="label"]').outerWidth();
			var $numericPaginatorLabelWidthPX = $numericPaginatorLabelWidth + 'px';
			var $numericPaginatorInputWrapper = $("[data-paginator='input']");
			
			if (_debug) {
				console.log($numericPaginatorLabelWidthPX);
			}	
			
			$($numericPaginatorInputWrapper).css('width', $numericPaginatorLabelWidthPX);
			
			});

		},
		keypressHandler : function(event) {
			if (event.originalEvent) {
				event = event.originalEvent;
			}

			if (_debug) {
				console.log("event from ", event.target.tagName.toLowerCase());
			}
			
			// don't handle if the actual target is an input field
			if (event.target.tagName.toLowerCase().match(/input|textarea/)) {
				return true;
			}
			
			if(viewer.paginator.timer) {
			    clearTimeout(viewer.paginator.timer);
			}

			var keyCode = event.keyCode;
			var now = Date.now();

			// this is a double key press if the last entered keycode is the same as the current one and the last key press is less than maxDoubleClickDelay ago
			var doubleKeypress = (viewer.paginator.lastKeycode == keyCode && now - viewer.paginator.lastKeyPress <= viewer.paginator.config.maxDoubleClickDelay);
			viewer.paginator.lastKeycode = keyCode;
			viewer.paginator.lastKeyPress = now;

			viewer.paginator.timer = setTimeout(function() {
			    
			    if (_debug) {
			        console.log("key pressed ", keyCode);
			        if (doubleKeypress) {
			            console.log("double key press");
			        }
			    }
			    switch (keyCode) {
					case _backwardKey:
					    if (doubleKeypress && viewer.paginator.config.first) {
					        viewer.paginator.config.first();
					    } 
					    else if (doubleKeypress) {
					        const fallbackFirst = document.querySelector('[data-target="paginatorFirstPage"]');
					        if (fallbackFirst) {
					            fallbackFirst.click();
					        }
					    }
					    else if (viewer.paginator.config.previous) {
					        viewer.paginator.config.previous();
					    } 
					    else {
					        const fallbackPrevious = document.querySelector('[data-target="paginatorPrevPage"]');
					        if (fallbackPrevious) {
					            fallbackPrevious.click();
					        }
					    }
					    break;
					case _forwardKey:
					    if (doubleKeypress && viewer.paginator.config.last) {
					        viewer.paginator.config.last();
					    } 
					    else if (doubleKeypress) {
					        const fallbackLast = document.querySelector('[data-target="paginatorLastPage"]');
					        if (fallbackLast) {
					            fallbackLast.click();
					        }
					    }
					    else if (viewer.paginator.config.next) {
					        viewer.paginator.config.next();
					    } 
					    else {
					        const fallbackNext = document.querySelector('[data-target="paginatorNextPage"]');
					        if (fallbackNext) {
					            fallbackNext.click();
					        }
					    }
					    break;
			    }
			    
			}, viewer.paginator.config.maxDoubleClickDelay);
			
		},
		close : function() {
			$(document.body).off("keyup", viewer.paginator.keypressHandler);
		},
		showPageNumberInput: function(e) {
			let $paginator = $(e.target).parents(".inputfield-paginator");
		    $paginator.find("[data-paginator='label']").hide();
		    $paginator.find("[data-paginator='input']").css('display', 'flex');
		    $paginator.find("[data-paginator='input-field']").trigger('focus');

			// Fill the actual page number on click
			var $actualPageNumber = $('#paginatorActualPageNumber').html();
			$paginator.find("[data-paginator='input-field']").val($actualPageNumber);
			
			// Automatically select the actual input value (user friendly behaviour)
			$paginator.find("[data-paginator='input-field']").select();

		},
		showPageNumberLabel: function(e) {
			let $paginator = $(e.target).parents(".inputfield-paginator");
		    $paginator.find("[data-paginator='input']").hide();
		    $paginator.find("[data-paginator='label']").show();
		},
		changePageNumber: function(e) {
			if (_debug) {
				console.log("changePageNumber",e);
			}
			let $paginator = $(e.target).parents(".inputfield-paginator");
			let $inputField = $paginator.find("[data-paginator='input-field']");
		    var targetPageNumber = parseInt($inputField.val());
			var $lastPageNumber = $('#paginatorLastPageNumber').html();

			if (targetPageNumber > $lastPageNumber) targetPageNumber = $lastPageNumber;

		    if (_debug) {
		    	console.log("targetPageNumber",targetPageNumber);
		    }
		    if(targetPageNumber != this.config.currentItem && targetPageNumber >= this.config.firstItem && targetPageNumber <= this.config.lastItem) {
		        let targetUrl = this.config.targetUrlPrefix + targetPageNumber + this.config.targetUrlSuffix;
		        if (_debug) {
			        console.log("navigate to", targetUrl);
		        }
		        window.location.href = targetUrl;
		    }
			else {
				$inputField.val($lastPageNumber);
			}
		}
	};

	return viewer;

})(viewerJS || {}, jQuery);