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
 * @description Basic module to setup viewerJS.
 * @version 3.4.0
 * @module viewerJS
 * @requires jQuery
 */

var viewerJS = (function () {
    'use strict';

    var _debug = false;
    var _defaults = {
        currentPage: '',
        browser: '',
        theme: '',
        sidebarSelector: '#sidebar',
        contentSelector: '#main',
        messageBoxSelector: '.messages .alert',
        messageBoxInterval: 1000,
        messageBoxTimeout: 8000,
        pageScrollSelector: '.icon-totop',
        pageScrollAnchor: '#top',
        widgetNerSidebarRight: false,
        accessDeniedImage: '',
        notFoundImage: '',
        activateDrilldownFilter: true
    };
    

    var viewer = {};
    viewer.initialized = new rxjs.Subject();

    viewer.init = function (config) {
        if (_debug) {
            console.log('Initializing: viewerJS.init');
            console.log('--> config = ', config);
        }

        $.extend(true, _defaults, config);
        if(_debug)console.log("init ", _defaults);
        // detect current browser
        _defaults.browser = viewerJS.helper.getCurrentBrowser();

        // write theme name to viewer object so submodules can use it
        viewer.theme = _defaults.theme;

        // throw some console infos about the page
        console.info('Current Browser = ', _defaults.browser);
        console.info('Current Theme = ', _defaults.theme);
        console.info('Current Page = ', _defaults.currentPage);

        //init websocket
        viewer.webSocket = new viewerJS.WebSocket(window.location.host, currentPath, viewerJS.WebSocket.PATH_SESSION_SOCKET);
  
        // init Bootstrap features
        viewerJS.helper.initBsFeatures();

        // init mobile Toggles for old responisive themes
        viewerJS.mobileToggles.init();

        // init save scroll positions
        viewerJS.scrollPositions.init();
        
        viewerJS.helper.initNumberOnlyInput();

        // init user login
        viewerJS.userLogin.init();
        
        viewerJS.popovers.init();
	    viewerJS.userDropdown.init();
	    
	    //init toggle hide/show
	    viewerJS.toggle.init();

       
        // init bookmarks if enabled
        if ( bookmarksEnabled ) { 
            viewerJS.bookmarks.init( {
                root: rootURL,
                rest: this.getRestApiUrl(),
                userLoggedIn: userLoggedIn,
                language: currentLang
                
            });
        } 

        // init scroll page animated
        this.pageScroll.init(_defaults.pageScrollSelector, _defaults.pageScrollAnchor);
        this.pageScroll.scrollToFragment();
        
        // init some image methods
        viewer.loadThumbnails();
        viewer.initFragmentNavigation();
        viewer.initStoreScrollPosition();

        // AJAX Loader Eventlistener
        viewerJS.jsfAjax.init(_defaults);
        
        //input validation status
        viewerJS.validationStatus.init();

        // render warning if local storage is not useable
        if (!viewer.localStoragePossible) {
            var warningPopover = this.helper
                .renderWarningPopover('Ihr Browser befindet sich im privaten Modus und unterstützt die Möglichkeit Informationen zur Seite lokal zu speichern nicht. Aus diesem Grund sind nicht alle Funktionen des viewers verfügbar. Bitte verlasen Sie den privaten Modus oder benutzen einen alternativen Browser. Vielen Dank.');

            $('body').append(warningPopover);

            $('body').on('click', '[data-toggle="warning-popover"]', function () {
                $('.warning-popover').fadeOut('fast', function () {
                    $('.warning-popover').remove();
                });
            });
        }
        
        // toggle work title body
        $('body').on( 'click', '.title__header h2', function () {
        	$( this ).find( '.fa' ).toggleClass( 'in' );
        	$( '.title__body' ).slideToggle( 'fast' );        	
        } );

        // toggle collapseable widgets
        $('body').on('click', '.widget__title.collapseable', function () {
            $(this).toggleClass('in').next().slideToggle('fast');
        });

        // fade out message box if it exists
        (function () {
            var fadeoutScheduled = false;

            setInterval(function () {
                if ($(_defaults.messageBoxSelector).length > 0) {
                    if (!fadeoutScheduled) {
                        fadeoutScheduled = true;
                        var messageTimer = setTimeout(function () {
                            $(_defaults.messageBoxSelector).each(function () {
                                $(this).fadeOut('slow', function () {
                                    $(this).remove();
                                })
                            });

                            fadeoutScheduled = false;
                        }, _defaults.messageBoxTimeout);
                    }
                }
            }, _defaults.messageBoxInterval);
        })();

        // add class on toggle sidebar widget (CMS individual sidebar widgets)
        $('.collapse').on('show.bs.collapse', function () {
            $(this).prev().find('.fa').removeClass('fa-arrow-down').addClass('fa-arrow-up');
        });

        $('.collapse').on('hide.bs.collapse', function () {
            $(this).prev().find('.fa').removeClass('fa-arrow-up').addClass('fa-arrow-down');
        });

        $('body').on('click', '[data-collapse-show]', function () {
            var href = $(this).data('collapse-show');
            $(href).collapse('show');
        });

        // init search drilldown filter
        if (_defaults.activateDrilldownFilter) {
            this.initDrillDownFilters();
            this.jsfAjax.success.subscribe(e => {
            	let collapseLink = $(e.source).attr("data-collapse-link");
            	if(collapseLink) {
            		this.initDrillDownFilters();
            	}
        	});
        }

        // disable submit button on feedback
        if (currentPage === 'feedback') {
            $('#submitFeedbackBtn').attr('disabled', true);
        }

        // set sidebar position for NER-Widget
        if ($('#widgetNerFacetting').length > 0) {
            nerFacettingConfig.sidebarRight = _defaults.widgetNerSidebarRight;
            this.nerFacetting.init(nerFacettingConfig);
        }

        // fire search query in autocomplete on enter
        $('body').on('keyup', '#pfAutocomplete_input, [id*=":pfAutocomplete_input"]', function (event) {
            if (event.keyCode == 13) {
                $('#submitSearch, [id*=":submitSearch"]').click();
            }
        });

        // make sure only integer values may be entered in input fields of class
        // 'input-integer'
        $('body').on('keypress', '.input-integer', function (event) {
            if (event.which < 48 || event.which > 57) {
                return false;
            }
        });

        // make sure only integer values may be entered in input fields of class
        // 'input-float'
        $('body').on('keypress', '.input-float', function (event) {
            switch (event.which) {
                case 8: // delete
                case 9: // tab
                case 13: // enter
                case 46: // dot
                case 44: // comma
                case 43: // plus
                case 45: // minus
                    return true;
                case 118:
                    return event.ctrlKey; // copy
                default:
                    switch (event.keyCode) {
                        case 8: // delete
                        case 9: // tab
                        case 13: // enter
                            return true;
                        default:
                            if (event.which < 48 || event.which > 57) {
                                return false;
                            }
                            else {
                                return true;
                            }
                    }
            }
        });

        // set tinymce language

        // init tinymce if it exists
        viewer.initTinyMCE();

        // handle browser bugs
        switch (_defaults.browser) {
            case 'Chrome':
                break;
            case 'Firefox':
                break;
            case 'IE':
                /* SET IE CLASS TO HTML */
                $('html').addClass('is-IE');
                break;
            case 'Edge':
                break;
            case 'Safari':
                break;
        } 
        
        // hide second scrollbar if modal is too long, bootstrap adaption
		$(document.body).on("show.bs.modal", function () {
			$(window.document).find("html").addClass("modal-open");
		});
		$(document.body).on("hide.bs.modal", function () {
			$(window.document).find("html").removeClass("modal-open");
		});

		// Method to select widget chronology input data and hide tooltip on click on input
		$('.widget-chronology-slider__item-input').click(function() {
			$('[data-toggle="tooltip"]').tooltip('hide');
			$(this).select();
		});	
		$('.widget-chronology-slider__item-input').focus(function(){
			$('.widget-chronology-slider__item-input[data-toggle="tooltip"]').tooltip('disable');
		}).blur(function(){
			$('.widget-chronology-slider__item-input[data-toggle="tooltip"]').tooltip('enable');
		});
		
		//init empty translator instance
	    var restApiURL = restURL.replace("/rest", "/api/v1");
	    viewer.translator = new viewerJS.Translator(restApiURL, currentLang);
     
		viewer.initialized.next();
		viewer.initialized.complete();
		viewer.setCheckedStatus();
		viewer.slider.init();
	// EOL viewerJS function
    };
    
    viewer.showLoader = function() {
        viewer.jsfAjax.complete.pipe(rxjs.operators.first()).subscribe(() => $(".ajax_loader").hide())
        $(".ajax_loader").show();
    }
    
    // refresh HC sticky method (use case: after ajax calls/DOM changes)
    viewer.refreshHCsticky = function () {
    	jQuery(document).ready(function($) {

        	$(".-refreshHCsticky").hcSticky('refresh', {});

    		});

    	// console.log('refresh hc sticky done');
    }
    
    viewer.initTinyMCE  = function(event) {
        //trigger initializazion if either no event was given or if it is a jsf event in status 'success'
        if(!event || event.status == "success") {            
            if ($('.tinyMCE').length > 0) {
                viewer.tinyConfig.language = currentLang;
                viewer.tinyConfig.setup = function (ed) {
                    // listen to changes on tinymce input fields
                    ed.on('init', function (e) {
                        if(_debug)console.log("init ", e);
                        viewer.refreshHCsticky();
                    });
                    
                    ed.on('change input paste', function (e) {
                       tinymce.triggerSave();
                       //trigger a change event on the underlying textArea
                       $(ed.targetElm).change();
                        if (currentPage === 'adminCmsCreatePage') {
                            createPageConfig.prevBtn.attr('disabled', true);
                            createPageConfig.prevDescription.show();
                        }
                    });
                };
                viewerJS.tinyMce.init(viewer.tinyConfig);
            }
        }
    }

    viewer.initFragmentNavigation = function () {
        if (window.location.hash) {
            $(document).ready(function () {
                var hash = window.location.hash;
                if(!hash.includes("=")) {                    
                    var $hashedElement = $(hash);
                    if ($hashedElement.length > 0) {
                        $hashedElement.get(0).scrollIntoView();
                        
                        var $hashedCollapsible = $hashedElement.find(".collapse");
                        if ($hashedCollapsible.length > 0) {
                            $hashedCollapsible.collapse("show");
                        }
                    }
                }
            })
        }
    }

    viewer.initStoreScrollPosition = function () {
        $(document).ready(function () {
            viewer.checkScrollPosition()
        })
        $('body').on('click', 'a.remember-scroll-position', function () {
            viewer.handleScrollPositionClick(this);
        });
    }

    viewer.checkScrollPosition = function () {
        var scrollPositionString = sessionStorage.getItem('scrollPositions');

        if (scrollPositionString) {
            var scrollPositions = JSON.parse(scrollPositionString);
            var scrollPosition = scrollPositions[currentPage];
            if (scrollPosition) {
                var linkId = scrollPosition.linkId;
                var $element = $('a[data-linkid="' + linkId + '"]');
                if ($element.length > 0) {
                    var scrollTop = scrollPosition.scrollTop;
                    if (_debug) {
                        console.log('scroll to position ', scrollTop);
                    }
                    $('html').scrollTop(scrollTop);
                    //after scrolling to the position, remove the entry
                    scrollPositions[currentPage] = undefined;
                    sessionStorage.setItem('scrollPositions', JSON.stringify(scrollPositions));
                }
            }
        }
    }

    viewer.handleScrollPositionClick = function (element) {
        var scrollPositions = {};
        var scrollPositionString = sessionStorage.getItem('scrollPositions');
        if (scrollPositionString) {
            scrollPositions = JSON.parse(scrollPositionString);
        }

        scrollPositions[currentPage] = {
            scrollTop: $('html').scrollTop(),
            linkId: $(element).data('linkid')
        }
        if (_debug) {
            console.log('saving scrollPositions ', scrollPositions);
        }
        sessionStorage.setItem('scrollPositions', JSON.stringify(scrollPositions));
    }

    viewer.initDrillDownFilters = function () {
        var $drilldowns = $('.widget-search-drilldown__collection');

        $drilldowns.each(function () {
            var filterConfig = {
                wrapper: $(this).find('.widget-search-drilldown__filter'),
                input: $(this).find('.widget-search-drilldown__filter-input'),
                elements: $(this).find('li a')
            }

            var filter = new viewerJS.listFilter(filterConfig);
        });
    }
    
    //for all html elements with attribute "data-checked" add the "checked" attribute - or remove it if the value of "data-checked" is "false"
    viewer.setCheckedStatus = function() {
    	$("[data-checked]").each( (index, element) => {
    		let checked = element.getAttribute("data-checked");
    		if(checked == "false") {
    			element.removeAttribute("checked");
    		} else {
    			element.setAttribute("checked", null);
    		}
    		
    	});
    }
    
    viewer.getRestApiUrl = function () {
        return restURL.replace("/rest", "/api/v1");
    }


    // init bootstrap 4 popovers
	$(document).ready(function(){
	    try {	        
	        $('[data-toggle="popover"]').popover({
	            trigger : 'hover'
	        });
	    } catch(error) {
	        //no bootstrap defined
	    }
	});


    // global object for tinymce config
    viewer.tinyConfig = {};

    return viewer;
    
})(jQuery);
  
	//reset global bootstrap boundary of tooltips to window
    if($.fn.tooltip.Constructor) {        
        $.fn.tooltip.Constructor.Default.boundary = "window";
        $.fn.dropdown.Constructor.Default.boundary = "window";
    }
