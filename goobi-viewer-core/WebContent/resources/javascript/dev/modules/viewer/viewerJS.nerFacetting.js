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
 * Module which facetts the NER-Tags in a sidebar widget and Pageview.
 * 
 * @version 3.2.0
 * @module viewerJS.nerFacetting
 * @requires jQuery
 * @requires Bootstrap
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _json = null;
    var _apiCall = '';
    var _html = '';
    var _pageCount = 0;
    var _scaleCount = 0;
    var _scaleHeight = 0;
    var _sliderHandlePosition = {};
    var _movedSliderHandlePosition = 0;
    var _recurrenceCount = 0;
    var _scaleValue = 0;
    var _sliderScaleHeight = 0;
    var _start = 0;
    var _end = 0;
    var _currentNerPageRangeSelected = '';
    var _currentNerPageRange = '';
    var _currentNerType = '';
    var _promise = null;
    var _defaults = {
        currentPage: '',
        baseUrl: '',
        apiUrl: '/rest/ner/tags/',
        workId: '',
        overviewTrigger: '',
        overviewContent: '',
        sectionTrigger: '',
        sectionContent: '',
        facettingTrigger: '',
        setTagRange: '',
        slider: '',
        sliderScale: '',
        sectionTags: '',
        currentTags: '',
        sliderHandle: '',
        sliderSectionStripe: '',
        recurrenceNumber: 0,
        recurrenceSectionNumber: 0,
        sidebarRight: false,
        loader: '',
        msg: {
            noJSON: 'Es konnten keine Daten abgerufen werden.',
            emptyTag: 'Keine Tags vorhanden',
            page: 'Seite',
            tags: 'Tags',
        }
    };
    
    viewer.nerFacetting = {
        /**
         * Method to initialize the NER-Widget or NER-View.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.currentPage The name of the current page.
         * @param {String} config.baseUrl The root URL.
         * @param {String} config.apiUrl The base URL for the API-Calls.
         * @param {String} config.workId The ID of the current work.
         * @param {String} config.overviewTrigger The ID/Class of the overview trigger.
         * @param {String} config.overviewContent The ID/Class of the content section from
         * overview.
         * @param {String} config.sectionTrigger The ID/Class of the section trigger.
         * @param {String} config.sectionContent The ID/Class of the content section from
         * section.
         * @param {String} config.facettingTrigger The ID/Class of the facetting trigger.
         * @param {String} config.setTagRange The ID/Class of the select menu for the
         * range.
         * @param {String} config.slider The ID/Class of the tag range slider.
         * @param {String} config.sliderScale The ID/Class of the slider scale.
         * @param {String} config.sectionTags The ID/Class of the tag section.
         * @param {String} config.currentTags The ID/Class of the tag container.
         * @param {String} config.sliderHandle The ID/Class of the slider handle.
         * @param {String} config.sliderSectionStripe The ID/Class of the range stripe on
         * the slider.
         * @param {Number} config.recurrenceNumber The number of displayed tags in a row.
         * @param {Number} config.recurrenceSectionNumber The number of displayed tags in
         * a section.
         * @param {Boolean} config.sidebarRight If true, the current tag row will show up
         * to the left of the sidebar widget.
         * @param {String} config.loader The ID/Class of the AJAX-Loader.
         * @param {Object} config.msg An object of strings for multi language use.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.nerFacetting.init' );
                console.log( '##############################' );
                console.log( 'viewer.nerFacetting.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            if ( viewer.localStoragePossible ) {
                // show loader
                $( _defaults.loader ).show();
                
                // clean local storage
                _cleanUpLocalStorage();
                
                // reset select menu
                $( _defaults.setTagRange ).find( 'option' ).attr( 'selected', false );
                
                if ( _defaults.currentPage === 'nerfacetting' ) {
                    $( _defaults.setTagRangeOverview ).find( 'option[value="1"]' ).prop( 'selected', true );
                }
                else {
                    $( _defaults.setTagRangeOverview ).find( 'option[value="10"]' ).prop( 'selected', true );
                }
                
                // reset facetting icons
                _resetFacettingIcons();
                
                // get data for current work
                if ( _defaults.currentPage === 'nerfacetting' ) {
                    _apiCall = _getAllTagsOfARange( 1, '-' );
                }
                else {
                    _apiCall = _getAllTagsOfARange( 10, '-' );
                }
                
                _promise = viewer.helper.getRemoteData( _apiCall );
                
                _promise.then( function( json ) {
                    _json = json;
                    
                    // check if data is not empty
                    if ( _json !== null || _json !== 'undefinded' ) {
                        // check if overview is already loaded
                        if ( $( _defaults.overviewContent ).html() === '' ) {
                            _renderOverview( _json );
                        }
                    }
                    else {
                        _html = viewer.helper.renderAlert( 'alert-danger', _defaults.msg.noJSON + '<br /><br />URL: ' + _apiCall, true );
                        $( _defaults.overviewContent ).html( _html );
                    }
                } ).then( null, function( error ) {
                    $( '.facetting-content' ).empty().append( viewer.helper
                            .renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                    console.error( 'ERROR: viewer.nerFacetting.init - ', error );
                } );
                
                /**
                 * Event if overview tab is clicked.
                 */
                $( _defaults.overviewTrigger ).on( 'click', function() {
                    // show loader
                    $( _defaults.loader ).show();
                    
                    // resets
                    $( _defaults.setTagRange ).find( 'option' ).attr( 'selected', false );
                    
                    if ( _defaults.currentPage === 'nerfacetting' ) {
                        $( _defaults.setTagRangeOverview ).find( 'option[value="1"]' ).prop( 'selected', true );
                        localStorage.setItem( 'currentNerPageRange', '1' );
                    }
                    else {
                        $( _defaults.setTagRangeOverview ).find( 'option[value="10"]' ).prop( 'selected', true );
                        localStorage.setItem( 'currentNerPageRange', '10' );
                    }
                    _currentNerPageRange = localStorage.getItem( 'currentNerPageRange' );
                    
                    localStorage.setItem( 'currentNerType', '-' );
                    _currentNerType = localStorage.getItem( 'currentNerType' );
                    
                    _resetFacettingIcons();
                    
                    // check if tab is active
                    if ( $( this ).parent().hasClass( 'active' ) ) {
                        console.info( 'Overview is already active.' );
                    }
                    else {
                        if ( _defaults.currentPage === 'nerfacetting' ) {
                            _apiCall = _getAllTagsOfARange( 1, '-' );
                        }
                        else {
                            _apiCall = _getAllTagsOfARange( 10, '-' );
                        }
                        
                        _promise = viewer.helper.getRemoteData( _apiCall );
                        
                        _promise.then( function( json ) {
                            _json = json;
                            _renderOverview( _json );
                        } ).then( null, function( error ) {
                            $( '.facetting-content' ).empty().append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' '
                                    + error.statusText, false ) );
                            console.error( 'ERROR: viewer.nerFacetting.init - ', error );
                        } );
                        
                    }
                } );
                
                /**
                 * Event if section tab is clicked.
                 */
                $( _defaults.sectionTrigger ).on( 'click', function() {
                    // show loader
                    $( _defaults.loader ).show();
                    
                    // reset select menu
                    $( _defaults.setTagRange ).find( 'option' ).attr( 'selected', false );
                    
                    if ( _defaults.currentPage === 'nerfacetting' ) {
                        $( _defaults.setTagRangeSection ).find( 'option[value="5"]' ).prop( 'selected', true );
                    }
                    else {
                        $( _defaults.setTagRangeSection ).find( 'option[value="10"]' ).prop( 'selected', true );
                    }
                    
                    // reset facetting
                    _resetFacettingIcons()

                    // set local storage value
                    if ( _defaults.currentPage === 'nerfacetting' ) {
                        localStorage.setItem( 'currentNerPageRange', 5 );
                    }
                    else {
                        localStorage.setItem( 'currentNerPageRange', 10 );
                    }
                    _currentNerPageRange = localStorage.getItem( 'currentNerPageRange' );
                    localStorage.setItem( 'currentNerType', '-' );
                    _currentNerType = localStorage.getItem( 'currentNerType' );
                    
                    // check if tab is active
                    if ( $( this ).parent().hasClass( 'active' ) ) {
                        console.info( 'Section is already active.' );
                    }
                    else {
                        _renderSection();
                        
                        // reset section stripe
                        $( _defaults.sliderSectionStripe ).css( 'top', '0px' );
                    }
                } );
                
                /**
                 * Event if select menu changes.
                 */
                $( _defaults.setTagRange ).on( 'change', function() {
                    var currVal = $( this ).val();
                    _currentNerType = localStorage.getItem( 'currentNerType' );
                    
                    // show loader
                    $( _defaults.loader ).show();
                    
                    // save current value in local storage
                    localStorage.setItem( 'currentNerPageRange', currVal );
                    _currentNerPageRange = localStorage.getItem( 'currentNerPageRange' );
                    
                    // render overview
                    if ( $( this ).hasClass( 'overview' ) ) {
                        if ( _currentNerType === null || _currentNerType === '' ) {
                            _currentNerType = '-';
                        }
                        _apiCall = _getAllTagsOfARange( currVal, _currentNerType );
                        
                        _promise = viewer.helper.getRemoteData( _apiCall );
                        
                        _promise.then( function( json ) {
                            _json = json;
                            
                            // check if data is not empty
                            if ( _json !== null || _json !== 'undefinded' ) {
                                _renderOverview( _json );
                            }
                            else {
                                _html = viewer.helper.renderAlert( 'alert-danger', _defaults.msg.noJSON + '<br /><br />URL: ' + _apiCall, true );
                                $( _defaults.overviewContent ).html( _html );
                            }
                        } ).then( null, function( error ) {
                            $( '.facetting-content' ).empty().append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' '
                                    + error.statusText, false ) );
                            console.error( 'ERROR: viewer.nerFacetting.init - ', error );
                        } );
                    }
                    // render section
                    else {
                        // setup values
                        localStorage.setItem( 'currentNerPageRange', currVal );
                        _currentNerPageRange = localStorage.getItem( 'currentNerPageRange' );
                        
                        _renderSection();
                        
                        // reset section stripe
                        if ( _currentNerPageRange > _pageCount ) {
                            $( _defaults.sliderSectionStripe ).css( {
                                'top': '0px',
                                'height': '600px'
                            } );
                        }
                        else {
                            $( _defaults.sliderSectionStripe ).css( {
                                'top': '0px',
                                'height': '100px'
                            } );
                        }
                    }
                    
                } );
                
                /**
                 * Event if facetting icons are clicked.
                 */
                $( _defaults.facettingTrigger ).on( 'click', function() {
                    var currType = $( this ).attr( 'data-type' );
                    
                    // show loader
                    $( _defaults.loader ).show();
                    
                    // set values
                    localStorage.setItem( 'currentNerType', currType );
                    _currentNerType = localStorage.getItem( 'currentNerType' );
                    
                    if ( _defaults.currentPage === 'nerfacetting' ) {
                        if ( _currentNerPageRange == null || _currentNerPageRange === '' ) {
                            _currentNerPageRange = localStorage.setItem( 'currentNerPageRange', 1 );
                        }
                    }
                    else {
                        if ( _currentNerPageRange == null || _currentNerPageRange === '' ) {
                            _currentNerPageRange = localStorage.setItem( 'currentNerPageRange', 10 );
                        }
                    }
                    _currentNerPageRange = localStorage.getItem( 'currentNerPageRange' );
                    
                    // activate icons
                    $( '.facetting-trigger' ).removeClass( 'active' );
                    $( this ).addClass( 'active' );
                    $( '.reset-filter' ).show();
                    
                    // filter overview
                    if ( $( this ).parent().parent().parent().attr( 'id' ) === 'overview' ) {
                        // setup data
                        _apiCall = _getAllTagsOfARange( _currentNerPageRange, _currentNerType );
                        
                        _promise = viewer.helper.getRemoteData( _apiCall );
                        
                        _promise.then( function( json ) {
                            _json = json;
                            
                            _renderOverview( _json );
                            
                            // hide select all
                            if ( $( this ).parent().hasClass( 'reset-filter' ) ) {
                                $( this ).parent().hide();
                            }
                            // set icons to active if "all" is selected
                            if ( _currentNerType === '-' ) {
                                $( '.facetting-trigger' ).addClass( 'active' );
                            }
                        } ).then( null, function( error ) {
                            $( '.facetting-content' ).empty().append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' '
                                    + error.statusText, false ) );
                            console.error( 'ERROR: viewer.nerFacetting.init - ', error );
                        } );
                    }
                    // filter section
                    else {
                        _renderSection();
                        
                        // hide select all
                        if ( $( this ).parent().hasClass( 'reset-filter' ) ) {
                            $( this ).parent().hide();
                        }
                        // set icons to active if "all" is selected
                        if ( _currentNerType === '-' ) {
                            $( '.facetting-trigger' ).addClass( 'active' );
                        }
                        // reset section stripe
                        $( _defaults.sliderSectionStripe ).css( 'top', '0px' );
                    }
                    
                } );
            }
            else {
                $( '.facetting-content' ).empty().append( viewer.helper
                        .renderAlert( 'alert-danger', '<strong>Deactivated: </strong>Not possible to write in local Storage!', false ) );
            }
        }
    };
    
    /**
     * Method to render the NER overview.
     * 
     * @method _renderOverview
     * @param {Object} data A JSON-Object.
     * @returns {Sting} A HTML-String which renders the overview.
     */
    function _renderOverview( data ) {
        if ( _debug ) {
            console.log( '---------- _renderOverview() ----------' );
            console.log( '_renderOverview: data = ', data );
        }
        
        _html = '';
        _html += '<ul class="overview-scale">';
        
        // render page number
        $.each( data.pages, function( p, page ) {
            _html += '<li>';
            _html += '<div class="page-number">';
            if ( data.rangeSize == 1 ) {
                if ( _defaults.currentPage === 'nerfacetting' ) {
                    _html += '<a href="' + _defaults.baseUrl + '/image/' + _defaults.workId + '/' + page.pageOrder + '/">';
                }
                else {
                    _html += '<a href="' + _defaults.baseUrl + '/' + _defaults.currentPage + '/' + _defaults.workId + '/' + page.pageOrder + '/">';
                }
                _html += page.pageOrder;
                _html += '</a>';
            }
            else {
                if ( _defaults.currentPage === 'nerfacetting' ) {
                    if ( page.firstPage !== undefined || page.lastPage !== undefined ) {
                        _html += '<a href="' + _defaults.baseUrl + '/image/' + _defaults.workId + '/' + page.firstPage + '/">';
                        _html += page.firstPage + '-' + page.lastPage;
                        _html += '</a>';
                    }
                    else {
                        _html += '<a href="' + _defaults.baseUrl + '/image/' + _defaults.workId + '/' + page.pageOrder + '/">';
                        _html += page.pageOrder;
                        _html += '</a>';
                    }
                }
                else {
                    if ( page.firstPage !== undefined || page.lastPage !== undefined ) {
                        _html += '<a href="' + _defaults.baseUrl + '/' + _defaults.currentPage + '/' + _defaults.workId + '/' + page.firstPage + '/">';
                        _html += page.firstPage + '-' + page.lastPage;
                        _html += '</a>';
                    }
                    else {
                        _html += '<a href="' + _defaults.baseUrl + '/' + _defaults.currentPage + '/' + _defaults.workId + '/' + page.pageOrder + '/">';
                        _html += page.pageOrder;
                        _html += '</a>';
                    }
                }
            }
            _html += '</div>';
            _html += '<div class="tag-container">';
            
            // render tags
            if ( page.tags.length === 0 || page.tags.length === 'undefined' ) {
                _html += '<span class="page-tag empty">' + _defaults.msg.emptyTag + '</span>';
            }
            else {
                $.each( page.tags, function( t, tag ) {
                    _html += '<span class="page-tag ' + tag.type + '">' + tag.value + '</span>';
                } );
            }
            _html += '</div>';
            _html += '</li>';
        } );
        _html += '</ul>';
        
        $( _defaults.overviewContent ).hide().html( _html ).find( '.tag-container' ).each( function() {
            $( this ).children( '.page-tag' ).slice( _defaults.recurrenceNumber ).remove();
        } );
        $( _defaults.overviewContent ).show();
        
        // hide loader
        $( _defaults.loader ).hide();
        
        $( '.tag-container' ).on( {
            'mouseover': function() {
                var $this = $( this );
                
                _showCurrentTags( $this );
            },
            'mouseout': function() {
                _hideCurrentTags();
            }
        } );
    }
    
    /**
     * Method which shows the current tag row in a tooltip.
     * 
     * @method _showCurrentTags
     * @param {Object} $obj An jQuery object of the current tag row.
     */
    function _showCurrentTags( $obj ) {
        var content = $obj.html();
        var pos = $obj.position();
        
        if ( _defaults.sidebarRight ) {
            if ( _defaults.currentPage === 'nerfacetting' ) {
                $( _defaults.currentTags ).html( content ).css( {
                    'display': 'block',
                    'top': pos.top + 25 + 'px',
                } );
            }
            else {
                $( _defaults.currentTags ).addClass( 'right' ).html( content ).css( {
                    'display': 'block',
                    'top': pos.top - 2 + 'px',
                    'left': 'auto',
                    'right': '100%'
                } );
            }
        }
        else {
            if ( _defaults.currentPage === 'nerfacetting' ) {
                $( _defaults.currentTags ).html( content ).css( {
                    'display': 'block',
                    'top': pos.top + 25 + 'px'
                } );
            }
            else {
                $( _defaults.currentTags ).html( content ).css( {
                    'display': 'block',
                    'top': pos.top - 2 + 'px'
                } );
            }
        }
    }
    
    /**
     * Method which hides the current tag row tooltip.
     * 
     * @method _hideCurrentTags
     */
    function _hideCurrentTags() {
        $( _defaults.currentTags ).hide();
    }
    
    /**
     * Method to render the NER section.
     * 
     * @method _renderSection
     * @param {Object} data A JSON-Object.
     */
    function _renderSection() {
        if ( _debug ) {
            console.log( '---------- _renderSection() ----------' );
            console.log( '_renderSection: _currentNerPageRange = ', _currentNerPageRange );
            console.log( '_renderSection: _currentNerType = ', _currentNerType );
        }
        
        // set values
        _apiCall = _getAllTags();
        
        _promise = viewer.helper.getRemoteData( _apiCall );
        
        _promise.then( function( workCall ) {
            _pageCount = _getPageCount( workCall );
            
            if ( _currentNerPageRange === null || _currentNerPageRange === '' ) {
                _currentNerPageRange = localStorage.getItem( 'currentNerPageRange' );
            }
            if ( _currentNerType === null || _currentNerType === '' ) {
                _currentNerType = localStorage.getItem( 'currentNerType' )
            }
            
            // render page count to scale
            if ( _defaults.currentPage === 'nerfacetting' ) {
                $( '#sliderScale .scale-page.end' ).html( _pageCount );
            }
            else {
                if ( _pageCount > 1000 ) {
                    $( '#sliderScale .scale-page.end' ).html( '999+' );
                }
                else {
                    $( '#sliderScale .scale-page.end' ).html( _pageCount );
                }
            }
            
            // init slider
            $( _defaults.slider ).slider( {
                orientation: "vertical",
                range: false,
                min: 1,
                max: _pageCount,
                value: _pageCount,
                slide: function( event, ui ) {
                    _sliderHandlePosition = $( _defaults.sliderHandle ).position();
                    _scaleValue = ( _pageCount + 1 ) - ui.value;
                    
                    // show bubble
                    $( '.page-bubble' ).show();
                    _renderPageBubble( _scaleValue );
                },
                start: function() {
                    _sliderHandlePosition = $( _defaults.sliderHandle ).position();
                    _movedSliderHandlePosition = _sliderHandlePosition.top;
                },
                stop: function( event, ui ) {
                    _currentNerType = localStorage.getItem( 'currentNerType' );
                    _sliderScaleHeight = $( _defaults.sliderScale ).height();
                    
                    // set position of section stripe
                    if ( _currentNerPageRange > _pageCount ) {
                        $( _defaults.sliderSectionStripe ).css( {
                            'top': '0px',
                            'height': '600px'
                        } );
                    }
                    else {
                        if ( _sliderHandlePosition.top < 100 ) {
                            $( _defaults.sliderSectionStripe ).animate( {
                                'top': '0px',
                                'height': '100px'
                            } );
                        }
                        else if ( _sliderHandlePosition.top > 100 ) {
                            if ( _sliderHandlePosition.top > 500 ) {
                                $( _defaults.sliderSectionStripe ).animate( {
                                    'top': ( _sliderScaleHeight - 100 ) + 'px',
                                    'height': '100px'
                                } );
                            }
                            else {
                                if ( _movedSliderHandlePosition < _sliderHandlePosition.top ) {
                                    $( _defaults.sliderSectionStripe ).animate( {
                                        'top': _sliderHandlePosition.top - 25 + 'px',
                                        'height': '100px'
                                    } );
                                }
                                else {
                                    $( _defaults.sliderSectionStripe ).animate( {
                                        'top': _sliderHandlePosition.top - 50 + 'px',
                                        'height': '100px'
                                    } );
                                }
                            }
                        }
                    }
                    
                    // render tags
                    switch ( _currentNerPageRange ) {
                        case '5':
                            _start = _scaleValue - 2;
                            _end = _scaleValue + 3;
                            
                            while ( _start < 1 ) {
                                _start++;
                                _end++;
                            }
                            while ( _end > _pageCount ) {
                                _start--;
                                _end--;
                            }
                            
                            _apiCall = _getAllTagsOfPageSection( _start, _end, _currentNerType );
                            break;
                        case '10':
                            _start = _scaleValue - 5;
                            _end = _scaleValue + 5;
                            
                            while ( _start < 1 ) {
                                _start++;
                                _end++;
                            }
                            while ( _end > _pageCount ) {
                                _start--;
                                _end--;
                            }
                            
                            _apiCall = _getAllTagsOfPageSection( _start, _end, _currentNerType );
                            break;
                        case '50':
                            _start = _scaleValue - 25;
                            _end = _scaleValue + 25;
                            
                            while ( _start < 1 ) {
                                _start++;
                                _end++;
                            }
                            while ( _end > _pageCount ) {
                                _start--;
                                _end--;
                            }
                            
                            _apiCall = _getAllTagsOfPageSection( _start, _end, _currentNerType );
                            break;
                        case '100':
                            _start = _scaleValue - 50;
                            _end = _scaleValue + 50;
                            
                            while ( _start < 1 ) {
                                _start++;
                                _end++;
                            }
                            while ( _end > _pageCount ) {
                                _start--;
                                _end--;
                            }
                            
                            _apiCall = _getAllTagsOfPageSection( _start, _end, _currentNerType );
                            break;
                    }
                    
                    _promise = viewer.helper.getRemoteData( _apiCall );
                    
                    _promise.then( function( json ) {
                        _json = json;
                        
                        _html = _renderSectionTags( _json );
                        
                        if ( _html === '' ) {
                            $( _defaults.sectionTags ).hide().html( viewer.helper.renderAlert( 'alert-warning', _defaults.msg.emptyTag, false ) ).show();
                        }
                        else {
                            $( _defaults.sectionTags ).hide().html( _html ).each( function() {
                                $( this ).children( '.page-tag' ).slice( _defaults.recurrenceSectionNumber ).remove();
                            } );
                            $( _defaults.sectionTags ).show();
                        }
                        
                        // hide bubble
                        $( '.page-bubble' ).fadeOut();
                        
                    } ).then( null, function( error ) {
                        $( '.facetting-content' ).empty().append( viewer.helper
                                .renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                        console.error( 'ERROR: viewer.nerFacetting.init - ', error );
                    } );
                }
            } );
            
            // render section tags
            _apiCall = _getAllTagsOfPageSection( 0, _currentNerPageRange, _currentNerType );
            
            _promise = viewer.helper.getRemoteData( _apiCall );
            
            _promise.then( function( json ) {
                _json = json;
                
                _html = _renderSectionTags( _json );
                
                if ( _html === '' ) {
                    $( _defaults.sectionTags ).hide().html( viewer.helper.renderAlert( 'alert-warning', _defaults.msg.emptyTag, false ) ).show();
                }
                else {
                    $( _defaults.sectionTags ).hide().html( _html ).each( function() {
                        $( this ).children( '.page-tag' ).slice( _defaults.recurrenceSectionNumber ).remove();
                    } );
                    $( _defaults.sectionTags ).show();
                }
                
                // hide loader
                $( _defaults.loader ).hide();
                
            } )
                    .then( null, function( error ) {
                        $( '.facetting-content' ).empty().append( viewer.helper
                                .renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                        console.error( 'ERROR: viewer.nerFacetting.init - ', error );
                    } );
            
        } ).then( null, function( error ) {
            $( '.facetting-content' ).empty().append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
            console.error( 'ERROR: viewer.nerFacetting.init - ', error );
        } );
    }
    
    /**
     * Method which renders the tags in the section area.
     * 
     * @method _renderSectionTags
     * @param {Object} data A JSON-Object.
     * @returns {Sting} A HTML-String which renders the tag section.
     */
    function _renderSectionTags( data ) {
        if ( _debug ) {
            console.log( '---------- _renderSectionTags() ----------' );
            console.log( '_renderSectionTags: data - ', data );
        }
        
        _html = '';
        // render tags
        $.each( data.pages, function( p, page ) {
            if ( page.tags.length === 0 || page.tags.length === 'undefined' ) {
                _html += '';
            }
            else {
                $.each( page.tags, function( t, tag ) {
                    if ( _defaults.currentPage === 'nerfacetting' ) {
                        if ( tag.counter < 10 ) {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 1.' + tag.counter + 'rem;">' + tag.value + '</span>';
                        }
                        else {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 2rem;">' + tag.value + '</span>';
                        }
                    }
                    else {
                        if ( tag.counter < 10 ) {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 1' + tag.counter + 'px;">' + tag.value + '</span>';
                        }
                        else {
                            _html += '<span class="page-tag ' + tag.type + '" style="font-size: 19px;">' + tag.value + '</span>';
                        }
                    }
                } );
            }
        } );
        
        return _html;
    }
    
    /**
     * Method which renders a span showing the current page section.
     * 
     * @method _renderPageBubble
     * @param {Number} page The current pagenumber.
     */
    function _renderPageBubble( page ) {
        if ( _debug ) {
            console.log( '---------- _renderPageBubble() ----------' );
            console.log( '_renderPageBubble: page - ', page );
        }
        
        var pageBubble = '';
        
        switch ( _currentNerPageRange ) {
            case '5':
                _start = page - 2;
                _end = page + 3;
                
                while ( _start < 1 ) {
                    _start++;
                    _end++;
                }
                while ( _end > _pageCount ) {
                    _start--;
                    _end--;
                }
                
                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
            case '10':
                _start = page - 5;
                _end = page + 5;
                
                while ( _start < 1 ) {
                    _start++;
                    _end++;
                }
                while ( _end > _pageCount ) {
                    _start--;
                    _end--;
                }
                
                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
            case '50':
                _start = page - 25;
                _end = page + 25;
                
                while ( _start < 1 ) {
                    _start++;
                    _end++;
                }
                while ( _end > _pageCount ) {
                    _start--;
                    _end--;
                }
                
                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
            case '100':
                _start = page - 50;
                _end = page + 50;
                
                while ( _start < 1 ) {
                    _start++;
                    _end++;
                }
                while ( _end > _pageCount ) {
                    _start--;
                    _end--;
                }
                
                pageBubble += '<span class="page-bubble">' + _start + '-' + _end + '</span>';
                break;
        }
        
        $( '#sliderVertical .ui-slider-handle' ).html( pageBubble );
    }
    
    /**
     * Method which returns the page count of the current work.
     * 
     * @method _getPageCount
     * @param {Object} work The current wor object.
     * @returns {Number} The page count of the current work.
     */
    function _getPageCount( work ) {
        if ( _debug ) {
            console.log( '---------- _getPageCount() ----------' );
            console.log( '_getPageCount: work - ', work );
        }
        
        return work.pages.length;
    }
    
    /**
     * Method which resets all facetting icons to default
     * 
     * @method _resetFacettingIcons
     */
    function _resetFacettingIcons() {
        if ( _debug ) {
            console.log( '---------- _resetFacettingIcons() ----------' );
        }
        
        $( '.facetting-trigger' ).addClass( 'active' );
        $( '.reset-filter' ).hide();
    }
    
    /**
     * Method which removes all set local storage values.
     * 
     * @method _cleanUpLocalStorage
     */
    function _cleanUpLocalStorage() {
        if ( _debug ) {
            console.log( '---------- _cleanUpLocalStorage() ----------' );
        }
        
        localStorage.removeItem( 'currentNerPageRange' );
        localStorage.removeItem( 'currentNerType' );
    }
    
    /**
     * API-Calls
     */
    // get all tags from all pages: /rest/ner/tags/{pi}/
    function _getAllTags() {
        return _defaults.baseUrl + _defaults.apiUrl + _defaults.workId;
    }
    
    // get all tags of a range: /viewer/rest/ner/tags/ranges/{range}/{type}/{pi}/
    function _getAllTagsOfARange( range, type ) {
        return _defaults.baseUrl + _defaults.apiUrl + 'ranges/' + range + '/' + type + '/' + _defaults.workId + '/';
    }
    
    // get all tags sorted of type: /rest/ner/tags/{type}/{pi}/
    function _getAllTagsOfAType( type ) {
        return _defaults.baseUrl + _defaults.apiUrl + type + '/' + _defaults.workId + '/';
    }
    
    // get all tags sorted of recurrence (asc/desc):
    // /rest/ner/tags/recurrence/{type}/{order}/{pi}/
    function _getAllTagsOfRecurrence( type, order ) {
        if ( type === '-' ) {
            return _defaults.baseUrl + _defaults.apiUrl + 'recurrence/-/' + order + '/' + _defaults.workId + '/';
        }
        else {
            return _defaults.baseUrl + _defaults.apiUrl + 'recurrence/' + type + '/' + order + '/' + _defaults.workId + '/';
        }
    }
    
    // get all tags sorted of page section: /rest/ner/tags/{start}/{end}/{pi}/
    function _getAllTagsOfPageSection( start, end, type ) {
        return _defaults.baseUrl + _defaults.apiUrl + start + '/' + end + '/' + type + '/' + _defaults.workId + '/';
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
