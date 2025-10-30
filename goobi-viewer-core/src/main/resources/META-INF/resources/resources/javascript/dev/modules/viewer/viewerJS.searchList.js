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
			viewerJS.helper.initBsFeatures;
            
            // focus save search modal input on show
            $( _defaults.saveSearchModalSelector ).on( 'shown.bs.modal', function() {
                $( _defaults.saveSearchInputSelector ).focus();
            } );

            /**
             * Keep trigger buttons for modal dialogs in sync with the modal state.
             * Applies a dedicated modifier class instead of Bootstrap's `.active`
             * to avoid conflicting styles and ensures the focus outline/hover
             * feedback resets correctly after the modal closes.
             *
             * @param {string} triggerSelector  Button selector used to open the modal
             * @param {string} modalSelector    CSS selector of the modal element
             */
            function bindModalTriggerState(triggerSelector, modalSelector) {
                var $modal = $(modalSelector);
                if ($modal.length === 0) {
                    return;
                }

                var $triggers = $(triggerSelector)
                    .attr('aria-pressed', false);

                function setTriggerState(isOpen) {
                    $triggers = $(triggerSelector);
                    if ($triggers.length === 0) {
                        return;
                    }
                    $triggers
                        .toggleClass('is-modal-trigger-open', isOpen)
                        .attr('aria-pressed', isOpen);

                    if (!isOpen) {
                        $triggers.removeClass('suppress-hover no-hover');
                        setTimeout(function() {
                            $triggers.each(function() {
                                this.blur();
                            });
                        }, 0);
                    } else {
                        $triggers.each(function() {
                            var $trigger = $(this);
                            $trigger.closest('[data-toggle="tooltip"]').tooltip('hide');
                            $trigger.find('[data-toggle="tooltip"]').tooltip('hide');
                        });
                    }
                }

                $modal.on('show.bs.modal', function() {
                    setTriggerState(true);
                }).on('hidden.bs.modal', function() {
                    setTriggerState(false);
                });

                $(document).on('click', triggerSelector, function() {
                    var $btn = $(this);
                    var btnEl = this;
                    $btn.addClass('suppress-hover');
                    setTimeout(function() {
                        $btn.removeClass('suppress-hover');
                    }, 200);
                    setTimeout(function() {
                        if (btnEl && typeof btnEl.blur === 'function') {
                            btnEl.blur();
                        }
                    }, 40);
                });
            }

            bindModalTriggerState('.search-list__help .btn.btn--icon', '#searchHelpModal');
            bindModalTriggerState('.search-list__save-search .btn.btn--icon', _defaults.saveSearchModalSelector);

            /**
             * Blur icon buttons that act as external links after pointer activation.
             * Prevents sticky hover/focus styles when a new browser tab/window opens.
             *
             * Keyboard activation keeps focus for accessibility.
             *
             * @param {string} selector
             */
            function blurOnPointerClick(selector) {
                $(document).on('click', selector, function(event) {
                    if (event && typeof event.detail === 'number' && event.detail === 0) {
                        return;
                    }

                    var el = this;
                    $(el).closest('[data-toggle="tooltip"]').tooltip('hide');

                    window.requestAnimationFrame(function() {
                        el.blur();
                    });
                });
            }

            blurOnPointerClick('.search-list__rss .btn.btn--icon');
            
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
                       
            //** DEFINE DIFFERENT LIST STYLES FUNCTIONS */      
                       
            function setListViewDetails() {
            	var $views = $( '.search-list__views button' );
            	$views.removeClass( 'active' ).attr('aria-pressed','false');
            	var $active = $('[data-view="search-list-default"]').addClass( 'active' );
            	$active.attr('aria-pressed','true');
            	$( '[data-toggle="hit-content"]' ).show();
				$( '.search-list__hit-thumbnail' ).css({'height': 'auto'});
            	$( '.search-list__hits' ).css( "opacity", 0 ).removeClass( 'grid' ).removeClass( 'list-view' );
            	
            	// set list style in local storage
            	// sessionStorage.setItem( 'searchListStyle', 'default' );
            	
            	// remove header background
            	$( '.search-list__hit-thumbnail' ).css( 'background-image', 'none' );
            	
            	$( '.search-list__hits' ).fadeTo(300,1);  
			}
			
            function setListViewGrid() {
                var $views = $( '.search-list__views button' );
                $views.removeClass( 'active' ).attr('aria-pressed','false');
                var $active = $('[data-view="search-list-grid"]').addClass( 'active' );
                $active.attr('aria-pressed','true');
                $( '[data-toggle="hit-content"]' ).hide();
				$( '.search-list__hit-thumbnail' ).css({'height': 'auto'});
                $( '.search-list__hits' ).css( "opacity", 0 ).removeClass( 'list-view' ).addClass( 'grid' );
                
                // set list style in local storage
                // sessionStorage.setItem( 'searchListStyle', 'grid' );
                
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
			}
			
            function setListViewList() {
                var $views = $( '.search-list__views button' );
                $views.removeClass( 'active' ).attr('aria-pressed','false');
                var $active = $('[data-view="search-list-list"]').addClass( 'active' );
                $active.attr('aria-pressed','true');
                $( '[data-toggle="hit-content"]' ).hide();
				$( '.search-list__hit-thumbnail' ).css({'height': '0'});
                $( '.search-list__hits' ).css( "opacity", 0 ).removeClass( 'grid' ).addClass( 'list-view' );
                
                // set list style in local storage
                // sessionStorage.setItem( 'searchListStyle', 'list-view' );
                
                $( '.search-list__hits' ).fadeTo(300,1);
			}         
                       

            // get/set list style from local storage
            
            if(!_defaults.listStyle) {
				
	            if ( sessionStorage.getItem( 'searchListStyle' ) == undefined ) {
	                sessionStorage.setItem( 'searchListStyle', 'default' );
	                if(_debug) {
	               		console.log('no default view set, so it is set now');
	                }
	            }
	            _searchListStyle = sessionStorage.getItem( 'searchListStyle' );
			} else if (typeof cmsSearchCustomView !== 'undefined' && (sessionStorage.getItem(cmsSearchViewlistStyleID) !== null)) {
			    _searchListStyle = sessionStorage.getItem(cmsSearchViewlistStyleID);
	                if(_debug) {
						console.log("view for THIS special search page (based on page id): " + sessionStorage.getItem(cmsSearchViewlistStyleID)); 
	                }
			     
			} else { 
				_searchListStyle = _defaults.listStyle;
	            if(_debug) {
					console.log('default value used (can be based on CMS comp), nothing else defined: ' + _defaults.listStyle);
				}
			}


            // load thumbnails before appying search list style
           // console.log("Load search hits with style " + _searchListStyle);
            switch ( _searchListStyle ) { 
                case 'default':
				case 'details':
					setListViewDetails();
                    break;
                case 'grid':
					setListViewGrid();
                    break;
                case 'list-view':
					setListViewList();
                    break;
            }

            // set default style  on button click
            $( '[data-view="search-list-default"]' ).on( 'click', function() {
				setListViewDetails();
				sessionStorage.setItem( 'searchListStyle', 'default' );
				if (cmsSearchCustomView == true) {
				    sessionStorage.setItem(cmsSearchViewlistStyleID, 'default' );
					    if(_debug) {
					    	console.log('set custom view list: default/details');
					    }
				} 
            } );

            // set grid style on button click
            $( '[data-view="search-list-grid"]' ).on( 'click', function() {
				setListViewGrid();
				sessionStorage.setItem( 'searchListStyle', 'grid' );
				if (typeof cmsSearchCustomView !== 'undefined' && cmsSearchCustomView == true) {
				    sessionStorage.setItem(cmsSearchViewlistStyleID, 'grid' );
					    if(_debug) {
					    	console.log('set custom view list: grid');
					    }
				}
            } );
            // set list style  on button click
            $( '[data-view="search-list-list"]' ).on( 'click', function() {
				setListViewList();
				sessionStorage.setItem( 'searchListStyle', 'list-view' );
				if (typeof cmsSearchCustomView !== 'undefined' && cmsSearchCustomView == true) {
					sessionStorage.setItem(cmsSearchViewlistStyleID, 'list-view' );
	           			if(_debug) {
					    	console.log('set custom view list: list');
					    }
				}
            } );

			this.initSubHits();
            
            // init thumbnail toggle            
            let $thumbToggle = $('[data-action="toggle-thumbs"]');
            if($thumbToggle.length > 0) {
                if ( sessionStorage.getItem( 'searchListShowThumbs' ) == undefined ) {
                    sessionStorage.setItem( 'searchListShowThumbs', false );
                }
                _searchListShowThumbs = sessionStorage.getItem( 'searchListShowThumbs' ).toLowerCase() === "true";
                this.showSearchListThumbs(_searchListShowThumbs);
                                
                $thumbToggle.on("click", () => {
                    $thumbToggle.trigger( "blur" );
                    _searchListShowThumbs = !_searchListShowThumbs;
                    this.showSearchListThumbs(_searchListShowThumbs);
                    sessionStorage.setItem( 'searchListShowThumbs', _searchListShowThumbs );
                    $thumbToggle.tooltip('show');
                });
            }
        },
        showSearchListThumbs: function(show) {
			const $thumbToggle = $('[data-action="toggle-thumbs"]');
			const activeTitle = $thumbToggle.attr("data-title-active");
            const inactiveTitle = $thumbToggle.attr("data-title-inactive");
            if(show) {                    
            	$thumbToggle
                .addClass("-active")
                .attr("title", activeTitle)
                .attr('aria-checked', true)
				.off('mouseleave.tooltip')
				.off('mouseenter.tooltip');
                $(".search-list__subhit-thumbnail").show();
                $('[data-toggle="tooltip"]').tooltip('dispose');
                viewerJS.helper.initBsFeatures();

             } else {
                $thumbToggle
                .removeClass("-active")
                .attr("title", inactiveTitle)
                .attr('aria-checked', false)
				.off('mouseleave.tooltip')
				.off('mouseenter.tooltip');
                $(".search-list__subhit-thumbnail").hide();
                $('[data-toggle="tooltip"]').tooltip('dispose');
                viewerJS.helper.initBsFeatures();
             }
		},
        initSubHits: function() {
            document.querySelectorAll('[data-toggle="hit-content"]')
            .forEach(button => {
                if(parseInt(button.dataset.childhits) <= _defaults.maxChildHitsToRenderOnStart ) {
                    this.openChildHits(button);
                }
            });

		},
		openChildHits: function(button) {
            // console.log("open child hits"); 
			var $currBtn = $( button );
                
            let scriptName = button.dataset.loadHitsScript;
            let toggleArea = document.querySelector( "div[data-toggle-id='"+button.dataset.toggleId+"']"  );
            let hitsPopulated = toggleArea.querySelector("[data-hits-populated]").dataset.hitsPopulated;
            if(_debug) {
				console.log("clicked hit-content", button, scriptName, toggleArea, hitsDisplayed, _defaults.childHitsToLoadOnExpand);
			}

			$currBtn.toggleClass( 'in' );
			$(toggleArea).slideToggle();
            if(_debug)console.log("call script ", scriptName, hitsPopulated);
			if(hitsPopulated <  _defaults.childHitsToLoadOnExpand) {
				//show loader now already. Otherwise it will only be shown when the post request starts and the request are carried out sequentially
				this.showAjaxLoader(toggleArea.querySelector(".search-list__loader").id);
				window[scriptName](); //execute commandScript to load child hits
			}
		},
		    
	    showAjaxLoader: function(loaderId) {
	    	let loader = document.getElementById(loaderId);
	    	loader.classList.remove("d-none");
	    },
	    hideAjaxLoader: function(loaderId) {
	    	let loader = document.getElementById(loaderId);
	    	loader.classList.add("d-none");
	    },
	    initChildHitThumbs: function(dataToggleId) {
			let showThumbs = this.isShowChildHitThumbs();
			if(this.isShowChildHitThumbs()) {
				$("[data-toggle-id="+dataToggleId+"]").find(".search-list__subhit-thumbnail").show();
			} else {
				$("[data-toggle-id="+dataToggleId+"]").find(".search-list__subhit-thumbnail").hide();
			}
		},
		isShowChildHitThumbs() {
			return sessionStorage.getItem( 'searchListShowThumbs' ).toLowerCase() === "true" ? true : false;
		}
    };



    
    return viewer;
    
} )( viewerJS || {}, jQuery );
