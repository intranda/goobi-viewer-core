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
        restApiPath: '/api/v1/search/hit/',
        hitsPerCall: 20,
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
            
            // get child hits            
            $( '[data-toggle="hit-content"]' ).each( function() {
                var currBtn = $( this );
                var currIdDoc = $( this ).attr( 'data-iddoc' );
                var currUrl = _getApiUrl( currIdDoc, _defaults.hitsPerCall );
                
                if ( _debug ) {
                    console.log( 'Current API Call URL: ', currUrl );
                }
                
                _promise = viewer.helper.getRemoteData( currUrl );
                
                currBtn.find( _defaults.hitContentLoaderSelector ).css( 'display', 'inline-block' );
                
                // get data and render hits if data is valid
                _promise.then( function( data ) {
                    if(data.hitsDisplayed == 0) {
                        //any hits are hidden. Hide whole subhits section
                        currBtn.hide();
                    } else if ( data.hitsDisplayed < _defaults.hitsPerCall ) {
                        // render child hits into the DOM
                        _renderChildHits( data, currBtn );
                        // set current button active, remove loader and show content
                        currBtn.toggleClass( 'in' ).find( _defaults.hitContentLoaderSelector ).hide();
                        currBtn.next().show();
                        // set event to toggle current hits
                        currBtn.off().on( 'click', function() {
                            $( this ).toggleClass( 'in' ).next().slideToggle();
                        } );
                    }
                    else {
                        // remove loader
                        currBtn.find( _defaults.hitContentLoaderSelector ).hide();
                        // set event to toggle current hits
                        currBtn.off().on( 'click', function() {
                            // render child hits into the DOM
                            _renderChildHits( data, currBtn );
                            // check if more children exist and render link
                            _renderGetMoreChildren( data, currIdDoc, currBtn );
                            $( this ).toggleClass( 'in' ).next().slideToggle();
                        } );
                    }
                } ).then( null, function() {
                    currBtn.next().append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                    console.error( 'ERROR: viewer.searchList.init - ', error );
                } );
            } );
            
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
    };
    
    /**
     * Method to get the full REST-API URL.
     * 
     * @method _getApiUrl
     * @param {String} id The current IDDoc of the hit set.
     * @returns {String} The full REST-API URL.
     */
    function _getApiUrl( id, hits ) {
        if ( _debug ) {
            console.log( '---------- _getApiUrl() ----------' );
            console.log( '_getApiUrl: id = ', id );
        }
        
        return _defaults.contextPath + _defaults.restApiPath + id + '/' + hits + '/';
    }
    
    /**
     * Method which renders the child hits into the DOM.
     * 
     * @method _renderChildHits
     * @param {Object} data The data object which contains the child hits.
     * @param {Object} $this The current child hits trigger.
     * @returns {Object} An jquery object which contains the child hits.
     */
    function _renderChildHits( data, $this ) {
        if ( _debug ) {
            console.log( '---------- _renderChildHits() ----------' );
            console.log( '_renderChildHits: data = ', data );
            console.log( '_renderChildHits: $this = ', $this );
        }
        
        var hitSet = null;
        
        // clean hit sets
        $this.next().empty();
        
        // build hits
        $.each( data.children, function( children, child ) {
            hitSet = $( '<div class="search-list__hit-content-set" />' );
            
            let hitSetText = $("<div class='search-list__hit-text-area'></div>");
            hitSet.append(hitSetText);
            
            // build title
            hitSetText.append( _renderHitSetTitle( child.browseElement ) );
            
            // append metadata if exist
            hitSetText.append( _renderMetdataInfo( child.foundMetadata, child.url ) );
            
            // append thumbnail image
            hitSet.append( _renderThumbnail( child.browseElement, child.url ) );
            
            // build child hits
            if ( child.hasChildren ) {
                $.each( child.children, function( subChildren, subChild ) {
                    hitSetText.append( _renderSubChildHits( subChild.browseElement, subChild.type, subChild.translatedType ) );
                } );
            } else {
                hitSetText.append( _renderSubChildHits( child.browseElement, child.type, child.translatedType ) );
            }
            
            // append complete set
            $this.next().append( hitSet );
        } );
        
    }
    
    function _renderThumbnail( browseElement, url ) {
        let $thumb = $("<a class='search-list__subhit-thumbnail'><img></img></a>");
        $thumb.find("img").attr("src", browseElement.thumbnailUrl);
        $thumb.attr("href", _defaults.contextPath + "/" + url);
        if(!_searchListShowThumbs) {
            $thumb.css("display", "none");
        }
        return $thumb;
    }
    
    /**
     * Method which renders the hit set title.
     * 
     * @method _renderHitSetTitle
     * @param {Object} data The data object which contains the hit set title values.
     * @returns {Object} A jquery object which contains the hit set title.
     */
    function _renderHitSetTitle( data ) {
        if ( _debug ) {
            console.log( '---------- _renderHitSetTitle() ----------' );
            console.log( '_renderHitSetTitle: data = ', data );
        }
        
        var hitSetTitle = null;
        var hitSetTitleH5 = null;
        var hitSetTitleDl = null;
        var hitSetTitleDt = null;
        var hitSetTitleDd = null;
        var hitSetTitleLink = null;
        
        hitSetTitle = $( '<div class="search-list__struct-title" />' );
        hitSetTitleH5 = $( '<h4 />' );
        if ( data.labelShort === 'TEI' ) {
        	hitSetTitleLink = $( '<span />' ).html( data.labelShort );
        }
        else {
        	hitSetTitleLink = $( '<a />' ).attr( 'href', _defaults.contextPath + '/' + data.url ).html( data.labelShort );        	
        }
        hitSetTitleH5.append( hitSetTitleLink );
        hitSetTitle.append( hitSetTitleH5 );
        
        return hitSetTitle;
    }
    
    /**
     * Method which renders metadata info.
     * 
     * @method _renderMetdataInfo
     * @param {Object} data The data object which contains the sub hit values.
     * @param {String} url The URL for the current work.
     * @returns {Object} A jquery object which contains the metadata info.
     */
    function _renderMetdataInfo( data, url ) {
        if ( _debug ) {
            console.log( '---------- _renderMetdataInfo() ----------' );
            console.log( '_renderMetdataInfo: data = ', data );
            console.log( '_renderMetdataInfo: url = ', url );
        }
        
        var metadataWrapper = null;
        var metadataTable = null;
        var metadataTableBody = null;
        var metadataTableRow = null;
        var metadataTableCellLeft = null;
        var metadataTableCellRight = null;
        var metadataKeyIcon = null;
        var metadataKeyLink = null;
        var metadataValueLink = null;
        
        if ( !$.isEmptyObject( data ) ) {
            metadataWrapper = $( '<div class="search-list__metadata-info" />' );
            metadataTable = $( '<table />' );
            metadataTableBody = $( '<tbody />' );
            
            data.forEach( function( metadata ) {
                // left cell
                metadataTableCellLeft = $( '<td />' );
                metadataKeyIcon = $( '<i />' ).attr( 'aria-hidden', 'true' ).addClass( 'fa fa-bookmark-o' );
                metadataKeyLink = $( '<span />' ).html( metadata.one + ':' );
                metadataTableCellLeft.append( metadataKeyIcon ).append( metadataKeyLink );
                
                // right cell
                metadataTableCellRight = $( '<td />' );
                metadataValueLink = $( '<a />' ).attr( 'href', _defaults.contextPath + '/' + url ).html( metadata.two );
                metadataTableCellRight.append( metadataValueLink );
                
                // row
                metadataTableRow = $( '<tr />' );
                metadataTableRow.append( metadataTableCellLeft ).append( metadataTableCellRight );
                
                // body
                metadataTableBody.append( metadataTableRow );
            } );
            
            metadataTable.append( metadataTableBody );
            metadataWrapper.append( metadataTable );
            
            return metadataWrapper;
        }
    }
    
    /**
     * Method which renders sub child hits.
     * 
     * @method _renderSubChildHits
     * @param {Object} data The data object which contains the sub hit values.
     * @param {String} type The type of hit to render.
     * @returns {Object} A jquery object which contains the sub child hits.
     */
    function _renderSubChildHits( data, type, title ) {
        if ( _debug ) {
            console.log( '---------- _renderSubChildHits() ----------' );
            console.log( '_renderSubChildHits: data = ', data );
            console.log( '_renderSubChildHits: type = ', type );
            console.log( '_renderSubChildHits: title = ', title ); 
        }
        
        var hitSetChildren = null;
        var hitSetChildrenDl = null;
        var hitSetChildrenDt = null;
        var hitSetChildrenDd = null;
        var hitSetChildrenLink = null;        
        var iconTitle;
        
        if ( title === '' || title === null ) {
        	iconTitle = '';
        }
        else {
        	iconTitle = title;
        }
        
        hitSetChildren = $( '<div class="search-list__struct-child-hits" />' );
        hitSetChildrenDl = $( '<dl class="dl-horizontal" />' );
        hitSetChildrenDt = $( '<dt />' );
        // check hit type
        switch ( type ) {
            case 'PAGE':
                hitSetChildrenDt.append( '<i class="fa fa-file-text" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'PERSON':
                hitSetChildrenDt.append( '<i class="fa fa-user" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'CORPORATION':
                hitSetChildrenDt.append( '<i class="fa fa-university" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'LOCATION':
                hitSetChildrenDt.append( '<i class="fa fa-location-arrow" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'ADDRESS':
                hitSetChildrenDt.append( '<i class="fa fa-envelope" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'SUBJECT':
                hitSetChildrenDt.append( '<i class="fa fa-question-circle-o" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'PUBLISHER':
                hitSetChildrenDt.append( '<i class="fa fa-copyright" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'COMMENT':
                hitSetChildrenDt.append( '<i class="fa fa-comment-o" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'CMS':
                hitSetChildrenDt.append( '<i class="fa fa-file-text-o" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'EVENT':
                hitSetChildrenDt.append( '<i class="fa fa-calendar" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
            case 'ACCESSDENIED':
                hitSetChildrenDt.append( '<i class="fa fa-lock" title="' + iconTitle + '" aria-hidden="true"></i>' );
                break;
        }
        hitSetChildrenDd = $( '<dd />' );
        hitSetChildrenLink = $( '<a />' ).attr( 'href', _defaults.contextPath + '/' + data.url );
        switch ( type ) {
            case 'CMS':
            case 'PAGE':
            case 'ACCESSDENIED':
                hitSetChildrenLink.append( data.fulltextForHtml );
                break;
            default:
                hitSetChildrenLink.append( data.labelShort );
                break;
        }
        hitSetChildrenDd.append( hitSetChildrenLink );
        hitSetChildrenDl.append( hitSetChildrenDt ).append( hitSetChildrenDd );
        if ( type !== null ) {        	
        	hitSetChildren.append( hitSetChildrenDl );        	
        }
        
        return hitSetChildren;
    }
    
    /**
     * Method to render a get more children link.
     * 
     * @method _renderGetMoreChildren
     */
    function _renderGetMoreChildren( data, iddoc, $this ) {
        if ( _debug ) {
            console.log( '---------- _renderGetMoreChildren() ----------' );
            console.log( '_renderGetMoreChildren: data = ', data );
            console.log( '_renderGetMoreChildren: iddoc = ', iddoc );
            console.log( '_renderGetMoreChildren: $this = ', $this );
        }
        
        var apiUrl = _getApiUrl( iddoc, _defaults.hitsPerCall + data.hitsDisplayed );
        var hitContentMore = $( '<div />' );
        var getMoreChildrenLink = $( '<button type="button" />' );
        
        if ( data.hasMoreChildren ) {
            // build get more link
            hitContentMore.addClass( 'search-list__hit-content-more' );
            getMoreChildrenLink.addClass( 'btn btn--clean' );
            getMoreChildrenLink.attr( 'data-api', apiUrl );
            getMoreChildrenLink.attr( 'data-iddoc', iddoc );
            getMoreChildrenLink.append( _defaults.msg.getMoreChildren );
            hitContentMore.append( getMoreChildrenLink );
            // append links
            $this.next().append( hitContentMore );
            // render new hit set
            getMoreChildrenLink.off().on( 'click', function( event ) {
                var currApiUrl = $( this ).attr( 'data-api' );
                var parentOffset = $this.parent().offset().top;
                
                // get data and render hits if data is valid
                _promise = viewer.helper.getRemoteData( currApiUrl );
                _promise.then( function( data ) {
                    // render child hits into the DOM
                    _renderChildHits( data, $this );
                    // check if more children exist and render link
                    _renderGetMoreChildren( data, iddoc, $this );
                } ).then( null, function() {
                    $this.next().append( viewer.helper.renderAlert( 'alert-danger', '<strong>Status: </strong>' + error.status + ' ' + error.statusText, false ) );
                    console.error( 'ERROR: _renderGetMoreChildren - ', error );
                } );
            } );
        }
        else {
            // clear and hide current get more link
            $this.next().find( _defaults.hitContentMoreSelector ).empty().hide();
            console.info( '_renderGetMoreChildren: No more child hits available' );
            return false;
        }
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
