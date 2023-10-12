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
 * Module which sets up the functionality for search list.
 * 
 * @version 3.2.0
 * @module viewerJS.searchList
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _promise = null;
    var _childHits = null;
    var _searchListStyle = '';
    var _searchListShowThumbs = false;
    var _defaults = {
        contextPath: '',
        maxChildHitsToRenderOnStart: 5,
        resetSearchSelector: '#resetCurrentSearch',
        searchInputSelector: '#currentSearchInput',
        searchTriggerSelector: '#slCurrentSearchTrigger',
        saveSearchModalSelector: '#saveSearchModal',
        saveSearchInputSelector: '#saveSearchInput',
        excelExportSelector: '.excel-export-trigger',
        excelExportLoaderSelector: '.excel-export-loader',
        risExportSelector: '.ris-export-trigger',
        risExportLoaderSelector: '.ris-export-loader',
        hitContentLoaderSelector: '.search-list__loader',
        hitContentSelector: '.search-list__hit-content',
        listStyle: '',
        msg: {
            getMoreChildren: 'Mehr Treffer laden',
        }
    };
    
    viewer.searchList = {
        /**
         * Method to initialize the search list features.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.searchList.init' );
                console.log( '##############################' );
                console.log( 'viewer.searchList.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // init bs tooltips
            $( '[data-toggle="tooltip"]' ).tooltip( {
                trigger : 'hover'
            } );
            
            // focus save search modal input on show
            $( _defaults.saveSearchModalSelector ).on( 'shown.bs.modal', function() {
                $( _defaults.saveSearchInputSelector ).focus();
            } );
            
            // set focus class if searchfield is focused
            $( '#currentSearchInput' ).on( {
            	focus: function() {
            		$( this ).prev().addClass( 'focus' );
            	},
            	blur: function() {
            		$( this ).prev().removeClass( 'focus' );
            	},
            } );
            
            // reset current search and redirect to standard search
            $( _defaults.resetSearchSelector ).on( 'click', function() {
                $( _defaults.searchInputSelector ).val( '' );
                location.href = _defaults.contextPath + '/search/';
            } );
            
            // show/hide loader for excel export
            $( _defaults.excelExportSelector ).on( 'click', function() {
                var trigger = $( this ); 
                var excelLoader = $( _defaults.excelExportLoaderSelector );
                
                trigger.hide();
                excelLoader.show();
                
                var url = _defaults.contextPath + '/api/v1/tasks/';
                let downloadFinished = false;
                rxjs.interval(1000)
                .pipe(rxjs.operators.flatMap(() => fetch(url)),
                        rxjs.operators.flatMap(response => response.json()),
                        rxjs.operators.takeWhile(json => {
                            let waiting = json.some(job => job.status != "COMPLETE" && job.status != "ERROR");
                            return waiting;
                        }),
                        rxjs.operators.last()
                )
                .subscribe((jobs) => {
                    //console.log("all jobs finished ", jobs.map(j => j.status));
                    excelLoader.hide();
                    trigger.show();
                })
               
            } );

            // show/hide loader for RIS export
            $( _defaults.risExportSelector ).on( 'click', function() {
                var trigger = $( this ); 
                var risLoader = $( _defaults.risExportLoaderSelector );
                
                trigger.hide();
                risLoader.show();
                
                var url = _defaults.contextPath + '/api/v1/tasks/';
                let downloadFinished = false;
                rxjs.interval(1000)
                .pipe(rxjs.operators.flatMap(() => fetch(url)),
                        rxjs.operators.flatMap(response => response.json()),
                        rxjs.operators.takeWhile(json => {
                            let waiting = json.some(job => job.status != "COMPLETE" && job.status != "ERROR");
                            return waiting;
                        }),
                        rxjs.operators.last()
                )
                .subscribe((jobs) => {
                    //console.log("all jobs finished ", jobs.map(j => j.status));
                    risLoader.hide();
                    trigger.show();
                })
               
            } );
                        
            // get/set list style from local storage
            if(!_defaults.listStyle) {				
	            if ( sessionStorage.getItem( 'searchListStyle' ) == undefined ) {
	                sessionStorage.setItem( 'searchListStyle', 'default' );
	            }
	            _searchListStyle = sessionStorage.getItem( 'searchListStyle' );
			} else {
				_searchListStyle = _defaults.listStyle;
			}
            
            // load thumbnails before appying search list style
           // console.log("Load search hits with style " + _searchListStyle);
            switch ( _searchListStyle ) { 
                case 'default':
				case 'details':
                    $( '.search-list__views button' ).removeClass( 'active' );
                    $( '[data-view="search-list-default"]' ).addClass( 'active' );
                    $( '.search-list__hits' ).removeClass( 'grid' ).removeClass( 'list-view' ).fadeTo(300,1);
                    $( '[data-toggle="hit-content"]' ).show();
                    
                    break;
                case 'grid':
                    $( '.search-list__views button' ).removeClass( 'active' );
                    $( '[data-view="search-list-grid"]' ).addClass( 'active' );
                    $( '.search-list__hits' ).removeClass( 'list-view' ).addClass( 'grid' );
                    $( '[data-toggle="hit-content"]' ).hide();
                    
                    // hide thumbnail and set src to header background
                    $( '.search-list__hit-thumbnail img' ).each( function() {
                        $( this ).on( 'load', function( event ) {
                            var imgUrl = $( event.currentTarget ).attr( 'src' );
                            if(imgUrl) {
                                $( event.currentTarget ).parents( '.search-list__hit-thumbnail' ).css( 'background-image', 'url("' + imgUrl + '")' );
                            }
                        }); 
                        
                        if ( this.complete ) {
                        	var imgUrl = $( this ).attr( 'src' );
                            if(imgUrl) {                                
                                $( this ).parents( '.search-list__hit-thumbnail' ).css( 'background-image', 'url("' + imgUrl + '")' );
                            }
                        }
                    } );
                    
                    $( '.search-list__hits' ).fadeTo(300,1);
                    
                    break;
                case 'list-view':
                    $( '.search-list__views button' ).removeClass( 'active' );
                    $( '[data-view="search-list-list"]' ).addClass( 'active' );
                    $( '.search-list__hits' ).removeClass( 'grid' ).addClass( 'list-view' ).fadeIn( 'fast' );
                    $( '[data-toggle="hit-content"]' ).hide();
                    
                    $( '.search-list__hits' ).fadeTo(300,1);
                    
                    break;
            }
            
            // set searchlist views
            // set default style
            $( '[data-view="search-list-default"]' ).on( 'click', function() {
            	$( '.search-list__views button' ).removeClass( 'active' );
            	$( this ).addClass( 'active' );
            	$( '[data-toggle="hit-content"]' ).show();
				$( '.search-list__hit-thumbnail' ).css({'height': 'auto'});
            	$( '.search-list__hits' ).css( "opacity", 0 ).removeClass( 'grid' ).removeClass( 'list-view' );
            	
            	// set list style in local storage
            	sessionStorage.setItem( 'searchListStyle', 'default' );
            	
            	// remove header background
            	$( '.search-list__hit-thumbnail' ).css( 'background-image', 'none' );
            	
            	$( '.search-list__hits' ).fadeTo(300,1);
            } );
            // set grid style
            $( '[data-view="search-list-grid"]' ).on( 'click', function() {
                $( '.search-list__views button' ).removeClass( 'active' );
                $( this ).addClass( 'active' );
                $( '[data-toggle="hit-content"]' ).hide();
				$( '.search-list__hit-thumbnail' ).css({'height': 'auto'});
                $( '.search-list__hits' ).css( "opacity", 0 ).removeClass( 'list-view' ).addClass( 'grid' );
                
                // set list style in local storage
                sessionStorage.setItem( 'searchListStyle', 'grid' );
                
                // hide thumbnail and set src to header background
                $( '.search-list__hit-thumbnail img' ).each( function() {
                    var imgUrl = $( this ).attr( 'src' );
                    $( this ).parents( '.search-list__hit-thumbnail' ).css( 'background-image', 'url("' + imgUrl + '")' );
                } );
                
                $( '.search-list__hits' ).fadeTo(300,1);
            } );
            // set list style
            $( '[data-view="search-list-list"]' ).on( 'click', function() {
                $( '.search-list__views button' ).removeClass( 'active' );
                $( this ).addClass( 'active' );
                $( '[data-toggle="hit-content"]' ).hide();
				$( '.search-list__hit-thumbnail' ).css({'height': '0'});
                $( '.search-list__hits' ).css( "opacity", 0 ).removeClass( 'grid' ).addClass( 'list-view' );
                
                // set list style in local storage
                sessionStorage.setItem( 'searchListStyle', 'list-view' );
                
                $( '.search-list__hits' ).fadeTo(300,1);
            } );

			this.initSubHits();
            
            //init thumbnail toggle            
            let $thumbToggle = $('[data-action="toggle-thumbs"]');
            if($thumbToggle.length > 0) {
                if ( sessionStorage.getItem( 'searchListShowThumbs' ) == undefined ) {
                    sessionStorage.setItem( 'searchListShowThumbs', false );
                }
                _searchListShowThumbs = sessionStorage.getItem( 'searchListShowThumbs' ).toLowerCase() === "true" ? true : false;
                
                let activeTitle = $thumbToggle.attr("data-title-active");
                let inactiveTitle = $thumbToggle.attr("data-title-inactive");
                if(_searchListShowThumbs) {                    
                    $thumbToggle
                        .addClass("-active")
                        .attr("title", activeTitle)
                        .tooltip('_fixTitle')
                        .attr('aria-checked', true);
                } else {
                    $thumbToggle
                    .removeClass("-active")
                    .attr("title", inactiveTitle)
                    .tooltip('_fixTitle')
                    .attr('aria-checked', false);
                }
                
                $thumbToggle.on("click", (event) => {
                    $thumbToggle.blur();
                    _searchListShowThumbs = !_searchListShowThumbs;
                    if(_searchListShowThumbs && !$thumbToggle.hasClass("-active")) {
                        $thumbToggle
                            .addClass("-active")
                            .attr("title", activeTitle)
                            .tooltip('_fixTitle')
                            .tooltip('show')
                            .attr('aria-checked', true);
                        $(".search-list__subhit-thumbnail").show();
                    } else {
                        $thumbToggle
                            .removeClass("-active")
                            .attr("title", inactiveTitle)
                            .tooltip('_fixTitle')
                            .tooltip('show')
                            .attr('aria-checked', false);
                        $(".search-list__subhit-thumbnail").hide();
                    }
                    sessionStorage.setItem( 'searchListShowThumbs', _searchListShowThumbs );
                });
            }
        },
        initSubHits: function() {
			            
            // get child hits            
            $( '[data-toggle="hit-content"]' )
            .filter( (index, button) => parseInt(button.dataset.childhits) <= _defaults.maxChildHitsToRenderOnStart )
            .each( (index, button) => this.openChildHits(button));
		},
		openChildHits: function(button) {
			var $currBtn = $( button );
                
            let scriptName = button.dataset.loadHitsScript;
            let toggleArea = document.querySelector( "div[data-toggle-id='"+button.dataset.toggleId+"']"  );
            let hitsDisplayed = $(toggleArea).find(".search-list__hit-content-set").length;
            if(_debug) {
				console.log("clicked hit-content", button, scriptName, toggleArea, hitsDisplayed);
			}

			$currBtn.toggleClass( 'in' );
			$(toggleArea).slideToggle();
			if(hitsDisplayed == 0) {
				window[scriptName](); //execute commandScript to load child hits
			}
		},
		    
	    showAjaxLoader: function(loaderId) {
	    	let loader = document.getElementById(loaderId);
	    	$(loader).show();
	    },
	    hideAjaxLoader: function(loaderId) {
	    	let loader = document.getElementById(loaderId);
	    	$(loader).hide();
	    }
    };



    
    return viewer;
    
} )( viewerJS || {}, jQuery );
