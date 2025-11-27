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
        activateFacetsFilter: true
    };
    

    var viewer = {};
    viewer.initialized = new rxjs.Subject();
    viewer.toggledCollapsible = new rxjs.Subject();
    
    viewer.init = function (config) {
        if (_debug) {
            console.log('Initializing: viewerJS.init');
            console.log('--> config = ', config);
        }

        $.extend(true, _defaults, config);
        if(_debug)console.log("init ", _defaults);
        // detect current browser
        _defaults.browser = getCurrentBrowser();

        // write theme name to viewer object so submodules can use it
        viewer.theme = _defaults.theme;

        // throw some console infos about the page
        console.info('Current Browser = ', _defaults.browser);
        console.info('Current Theme = ', _defaults.theme);
        console.info('Current Page = ', _defaults.currentPage);
 
 		viewer.disclaimerModal.init(this.disclaimerConfig);

        //init websocket
        if(viewer.useWebSocket) {
	        viewer.webSocket = new viewerJS.WebSocket(window.location.host, currentPath, viewerJS.WebSocket.PATH_SESSION_SOCKET);
        }
        
  
        // init Bootstrap features
        viewerJS.helper.initBsFeatures();
        
        viewerJS.helper.initRestrictedInputFields();

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
	    
	    viewerJS.initWidgetUsage();

        viewerJS.initFragmentActions();
       
        viewerJS.initRequiredInputs();
        
        viewerJS.initDisallowDownload();
               
        // init bookmarks if enabled
        if ( window.bookmarksEnabled ) { 
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
        viewer.initFragmentNavigation();
        viewer.initStoreScrollPosition();
        viewer.initSidebarCollapsible();

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
        $('body').on( 'click', '.title__header h1', function () {
        	$( this ).find( '.fa' ).toggleClass( 'in' );
        	$( '.title__body' ).slideToggle( 'fast' );        	
        } );





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

        // init search facets filter
        if (_defaults.activateFacetsFilter) {
            this.initFacetsFilters();
            this.jsfAjax.success.subscribe(e => {
            	let collapseLink = $(e.source).attr("data-collapse-link");
            	if(collapseLink) {
            		this.initFacetsFilters();
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
        $('body').on('keyup', '#pfAutocomplete_input, [id*="\\:pfAutocomplete_input"]', function (event) {
            if (event.keyCode == 13) {
                $('#submitSearch, [id*="\\:submitSearch"]').click();
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
	    viewer.translator = new viewerJS.translator(restApiURL, currentLang);
		viewer.initialized.next();
		viewer.initialized.complete();
		viewer.setCheckedStatus();
		viewer.slider.init();
    viewer.accessibility.init();
	// EOL viewerJS function
    };

    
    viewer.showLoader = function() {
        viewer.jsfAjax.complete.pipe(rxjs.operators.first()).subscribe(() => $(".ajax_loader").hide())
        $(".ajax_loader").show();
    }
  	
	// Collapsible sidebar widgets function
    viewer.initSidebarCollapsible = function() {
    	$('body').on('click', '.widget__topbar.collapsible', function (e) {
			$(this).toggleClass('in').closest('.widget').find('.widget__body').slideToggle(300, function() {
				viewer.toggledCollapsible.next(e);
		    })
		})
    }
   
    viewer.initTinyMCE  = function(event) {
        //trigger initializazion if either no event was given or if it is a jsf event in status 'success'
        if(!event || event.status == "success") {            
            if ($('.tinyMCE').length > 0) {
                viewer.tinyConfig.language = currentLang;
                viewer.tinyConfig.setup = function (ed) {
                    // listen to changes on tinymce input fields
                    ed.on('init', function (e) {
                        viewerJS.stickyElements.refresh.next();
                    });
                    
                    ed.on('change input paste', function (e) {
                       tinymce.triggerSave();
                       //trigger a change event on the underlying textArea
                       $(ed.targetElm).change();
                        if (currentPage === 'adminCmsNewPage') {
                            createPageConfig.prevBtn.attr('disabled', true);
                            createPageConfig.prevDescription.show();
                        }
                    });
                };
                viewerJS.tinyMce.init(viewer.tinyConfig);
            }
        }
    }

	viewer.initDisallowDownload = function() {
		$("[data-allow-download='false']").each( (index, ele) => {
			ele.addEventListener('contextmenu', function(e) {
			  e.preventDefault();
			});
		})
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

    viewer.initFacetsFilters = function () {
        var $facets = $('[data-facet="searchFacetFilter"]');

        $facets.each(function () {
            var filterConfig = {
				inputToggle: $(this).find('[data-toggle="searchFacetFilter"]'),
                wrapper: $(this).find('[data-wrapper="searchFacetFilter"]'),
                header: $(this).find('[data-heading="searchFacetFilter"]'),
                input: $(this).find('[data-input="searchFacetFilter"]'),
                elements: $(this).find('li')
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

    // Helper fn: returns true if OS is iOS
    viewer.iOS = function() {
      return [
          'iPad Simulator',
          'iPhone Simulator',
          'iPod Simulator',
          'iPad',
          'iPhone',
          'iPod'
        ].includes(navigator.platform)
        // iPad on iOS 13 detection
        || (navigator.userAgent.includes("Mac") && "ontouchend" in document)
    }
    
    viewer.getRestApiUrl = function () {
        return restURL.replace("/rest", "/api/v1");
    }
    
    viewer.initWidgetUsage = function() {
    	let $widgetUsage = $(".widget-usage");
    	if($widgetUsage.length > 0) {
    		let $widgetTitles = $widgetUsage.find(".widget-usage__subtitle");
    		$widgetTitles.each((index,element) => {
    			let $title = $(element);
    			let $options = $title.next();
    			if($options.length == 0) {
    				$title.hide();
    			} else {
					// console.log('not empty!');
				}
    		});
    	}
    }

	viewer.initFragmentActions = function() {
		window.onhashchange = () => viewer.handleFragmentAction();
		viewer.handleFragmentAction();
	}
	
	// IF #feedback HASH IS RECOGNIZED MAKE OPEN MODAL LINK OUT OF IT
	viewer.handleFragmentAction = function() {
		let hash = location.hash;
		if(hash) {
			let hashName = hash.replace(/[=:].*/, "");
			switch(hashName) {
				case "#feedback":
					
					$('#feedbackModal').modal();
					let recipientMatch = hash.match(/recipient=([\w-]+)/);
					if(recipientMatch && recipientMatch.length == 2) {
						let recipient = recipientMatch[1];					
						$('#feedbackModal .feedback-modal__recipient-dropdown').find('[data-recipient-id="'+recipient+'"]').attr('selected', 'selected');
					}
					$('#feedbackModal').on('hidden.bs.modal', function (e) {
						history.replaceState({}, '', window.location.href.replace(/#feedback.*$/, ""));
					})
					break;
				case "#?page":
					// SHOW LOADER BEFORE REDIRECT
					$("[data-loader='fullOverlay']").show();
					$("html").addClass('modal-open');
					// SHOW THE LOADER FOR SOME TIME TO AVOID UNWANTED FLASHING OF HALF TRANSPARENT BACKDROP
					// THEN REDIRECT
					setTimeout(function(){
						let url = location.href.replace("#?page", "&page");
						location.replace(url);
					}, 1400);

			}
		}
	}

	// REQUIRED FIELDS/CHECKBOXES CHECK
	viewer.initRequiredInputs = function() {
	
		let $requireElements = $("[data-require-input]");
		
		$requireElements.each((index, element) => {
			let $ele = $(element);
			$ele.attr("disabled", "disabled");
			let id = $(element).attr("data-require-input").replace(/[!"#$%&'()*+,.\/:;<=>?@[\\\]^`{|}~]/g, "\\\\$&");
			let $texts = $("[data-require-input-text='" + id + "']");
			let $checkboxes = $("input[data-require-input-checkbox='" + id + "']");
			let $uncheckedRadios = $('input[data-require-input-checkbox="uncheckedChecker"]');
			
			var allCheckboxesChecked = false;
			var allTextFieldsFilled = false;
			
			if ($checkboxes.length == 0) {
				// console.log('no checkboxes to check, so allCheckboxesChecked is true');
				var allCheckboxesChecked = true;
			}
			
			
			// CHECK ALL REQUIRED CHECKBOXES AND TEXT FIELDS + CHECK IF RADIO BUTTONS "UNCHECKED"
			$checkboxes.add($texts).add($uncheckedRadios).on("change paste keyup cut", (e) => {
				$checkboxes.each((index, element) => {
					if ( $(element).is(':checked') ) {
						let isChecked = true;
						  // console.log("all boxes checked");
						 // console.log(allCheckboxesChecked);
						 allCheckboxesChecked = true;
					} else {
						let isChecked = false;
						allCheckboxesChecked = false;
						// console.log(allCheckboxesChecked);
						// console.log("not all boxes checked");
						return false;
					}
					});
				
					$texts.each((index, element) => {
						let text = $(element).val();
						if(!text) {
							allTextFieldsFilled = false;
							// console.log(allTextFieldsFilled);
							// console.log($texts);
							// console.log('NO - not all texts filled');
							return false;
						} else {
							allTextFieldsFilled = true;
							// console.log($texts);
							// console.log('YES - all texts filled');
						}
					});
				
				
				// ACTIVATE SUBMIT BUTTON IF ALL REQUIRED CHECKBOXES AND TEXTFIELDS ARE FILLED/CHECKED
				if(allCheckboxesChecked && allTextFieldsFilled == true) {
					$ele.removeAttr("disabled");
					// console.log('button activated, removed attribute');
				} else {
					$ele.attr("disabled", "disabled");
					// console.log('button DEactivated, attribute added');
				}
				
				// console.log(allCheckboxesChecked);
							
			});
			
		});
	
	}

	
	
    // CONTENT ITEMS JS DRAFT
	$(document).ready(function(){
		if(typeof SimpleLightbox != "undefined" ) {			
			new SimpleLightbox({elements: '[data-target="imageLightbox"]'});
		}
	});


    // global object for tinymce config
    viewer.tinyConfig = {};

    return viewer;
    
})(jQuery);
  
	// loading screen while page redirect

	

	// overriding enforceFocus for bootstrao modals in general - important: This might be a problem for accessibility
	// $.fn.modal.Constructor.prototype._enforceFocus = function() {};

	//reset global bootstrap boundary of tooltips to window
    if($.fn.tooltip.Constructor) {        
        $.fn.tooltip.Constructor.Default.boundary = "window";
        $.fn.dropdown.Constructor.Default.boundary = "window";
    }
