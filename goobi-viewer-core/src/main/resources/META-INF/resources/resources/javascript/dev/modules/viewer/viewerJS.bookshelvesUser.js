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
 * Module to manage bookmark lists if the user is logged in.
 * 
 * @version 3.2.0
 * @module viewerJS.bookmarkListsUser
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = true;
    var _defaults = {
        root: '',
        msg: {
            resetBookmarkLists: '',
            resetBookmarkListsConfirm: '',
            noItemsAvailable: '',
            selectBookmarkList: '',
            addNewBookmarkList: '',
            typeRecord: '',
            typePage: ''
        }
    };
    
    viewer.bookmarkListsUser = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.bookmarkListsUser.init' );
                console.log( '##############################' );
                console.log( 'viewer.bookmarkListsUser.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // render bookmarks navigation list
            _renderBookmarksNavigationList();
            
            // toggle bookmarks dropdown
            $( '[data-bookmark-list-type="dropdown"]' ).off().on( 'click', function( event ) {
                event.stopPropagation();
                
                // hide other dropdowns
                $( '.login-navigation__login-dropdown, .login-navigation__user-dropdown, .navigation__collection-panel' ).hide();
                $( '.bookshelf-popup' ).remove();
                
                $( '.bookshelf-navigation__dropdown' ).slideToggle( 'fast' );
            } );
            
            // check if element is in any bookmark list
            _setAddedStatus();
            
            // render bookmarks popup
            $( '[data-bookmark-list-type="add"]' ).off().on( 'click', function( event ) {
                event.stopPropagation();
                
                // hide other dropdowns
                $( '.bookshelf-navigation__dropdown, .login-navigation__user-dropdown' ).hide();
                
                var currBtn = $( this );
                var currPi = currBtn.attr( 'data-pi' );
                var currLogid = currBtn.attr( 'data-logid' );
                var currPage = currBtn.attr( 'data-page' );
                var currType = currBtn.attr( 'data-type' );
                var currPos = currBtn.offset();
                var currSize = {
                    width: currBtn.outerWidth(),
                    height: currBtn.outerHeight()
                };
                
                // render bookmarks popup
                _renderBookmarkListPopup( currPi, currLogid, currPage, currPos, currSize );
            } );
            
            // hide menus/popups by clicking on body
            $( 'body' ).on( 'click', function( event ) {
                $( '.bookshelf-navigation__dropdown' ).hide();
                
                if ( $( '.bookshelf-popup' ).length > 0 ) {
                    var target = $( event.target );
                    var popup = $( '.bookshelf-popup' );
                    var popupChild = popup.find( '*' );
                    
                    if ( !target.is( popup ) && !target.is( popupChild ) ) {
                        $( '.bookshelf-popup' ).remove();
                    }
                }
            } );
            
            // add new bookmark list in overview
            $( '#addBookmarkListBtn' ).off().on( 'click', function() {
                var bsName = $( '#addBookmarkListInput' ).val();
                
                if ( bsName != '' ) {
                    _addNamedBookmarkList( _defaults.root, bsName ).then( function() {
                        location.reload();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _addNamedBookmarkList: ', error.responseText );
                    } );
                }
                else {
                    _addAutomaticNamedBookmarkList( _defaults.root ).then( function() {
                        location.reload();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _addAutomaticNamedBookmarkList: ', error.responseText );
                    } );
                }
            } );
            
            // add new bookmark list on enter in overview
            $( '#addBookmarkListInput' ).on( 'keyup', function( event ) {
                if ( event.which == 13 ) {
                    $( '#addBookmarkListBtn' ).click();
                }
            } );
            
            // set bookmark list id to session storage for mirador view
            $( ".view-mirador__link" ).on( "click", function() {
        		var currBookmarkListId = $( this ).attr( "data-bookmark-list-id" );
                
        		sessionStorage.setItem( 'bookmarkListId', currBookmarkListId );
        	} );
        }
    };
    /* ######## ADD (CREATE) ######## */
    /**
     * Method to add an item with PI, LOGID and PAGE to the user bookmark list.
     * 
     * @method _addBookmark
     * @param {String} root The application root path.
     * @param {String} id The current bookmark list id.
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addBookmark( root, id, pi, logid, page ) {
        if ( _debug ) {
            console.log( '---------- _addBookmark() ----------' );
            console.log( '_addBookmark: root - ', root );
            console.log( '_addBookmark: id - ', id );
            console.log( '_addBookmark: pi - ', pi );
            console.log( '_addBookmark: logid - ', logid );
            console.log( '_addBookmark: page - ', page );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/get/' + id + '/add/' + pi + '/' + page + '/' + logid + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add a named bookmark list to the user account.
     * 
     * @method _addNamedBookmarkList
     * @param {String} root The application root path.
     * @param {String} name The name of the bookmark list.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addNamedBookmarkList( root, name ) {
        if ( _debug ) {
            console.log( '---------- _addNamedBookmarkList() ----------' );
            console.log( '_addNamedBookmarkList: root - ', root );
            console.log( '_addNamedBookmarkList: name - ', name );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/add/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add a automatic named bookmark list to the user account.
     * 
     * @method _addAutomaticNamedBookmarkList
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addAutomaticNamedBookmarkList( root ) {
        if ( _debug ) {
            console.log( '---------- _addAutomaticNamedBookmarkList() ----------' );
            console.log( '_addAutomaticNamedBookmarkList: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/add/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add the session bookmark list to the user account with an automatic generated
     * name.
     * 
     * @method _addSessionBookmarkList
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addSessionBookmarkList( root ) {
        if ( _debug ) {
            console.log( '---------- _addSessionBookmarkList() ----------' );
            console.log( '_addSessionBookmarkList: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/addSessionBookmarkList/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add the session bookmark list to the user account with a given name.
     * 
     * @method _addSessionBookmarkListNamed
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addSessionBookmarkListNamed( root, name ) {
        if ( _debug ) {
            console.log( '---------- _addSessionBookmarkListNamed() ----------' );
            console.log( '_addSessionBookmarkListNamed: root - ', root );
            console.log( '_addSessionBookmarkListNamed: name - ', name );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/addSessionBookmarkList/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## GET (READ) ######## */
    /**
     * Method to get all user bookmark lists.
     * 
     * @method _getAllBookmarkLists
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getAllBookmarkLists( root ) {
        if ( _debug ) {
            console.log( '---------- _getAllBookmarkLists() ----------' );
            console.log( '_getAllBookmarkLists: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get a bookmark list by id.
     * 
     * @method _getBookmarkListById
     * @param {String} root The application root path.
     * @param {String} id The current bookmark list id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getBookmarkListById( root, id ) {
        if ( _debug ) {
            console.log( '---------- _getBookmarkListById() ----------' );
            console.log( '_getBookmarkListById: root - ', root );
            console.log( '_getBookmarkListById: id - ', id );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/get/' + id + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get the number of items in the selected bookmark list.
     * 
     * @method _getBookmarkListItemCount
     * @param {String} root The application root path.
     * @param {String} id The current bookmark list id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getBookmarkListItemCount( root, id ) {
        if ( _debug ) {
            console.log( '---------- _getBookmarkListItemCount() ----------' );
            console.log( '_getBookmarkListItemCount: root - ', root );
            console.log( '_getBookmarkListItemCount: id - ', id );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/get/' + id + '/count/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get all public bookmark lists.
     * 
     * @method _getPublicBookmarkLists
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getPublicBookmarkLists( root ) {
        if ( _debug ) {
            console.log( '---------- _getPublicBookmarkLists() ----------' );
            console.log( '_getPublicBookmarkLists: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/public/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get all shared bookmark lists.
     * 
     * @method _getSharedBookmarkLists
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getSharedBookmarkLists( root ) {
        if ( _debug ) {
            console.log( '---------- _getSharedBookmarkLists() ----------' );
            console.log( '_getSharedBookmarkLists: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/shared/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }

    /**
     * Method to check if an item by PI, LOGID and PAGE if it's in user bookmark list. It
     * returns the bookmark lists or false if no items are in list.
     * 
     * @method _getContainingBookmarkListItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} pi The pi of the current item.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getContainingBookmarkListItem( root, pi, logid, page ) {
        if ( _debug ) {
            console.log( '---------- _getContainingBookmarkListItem()() ----------' );
            console.log( '_getContainingBookmarkListItem(): root - ', root );
            console.log( '_getContainingBookmarkListItem(): pi - ', pi );
            console.log( '_getContainingBookmarkListItem(): logid - ', logid );
            console.log( '_getContainingBookmarkListItem(): page - ', page );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/contains/' + pi + '/' + page + '/-/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## SET (UPDATE) ######## */
    /**
     * Method to set the name of a bookmark list.
     * 
     * @method _setBookmarkListName
     * @param {String} root The application root path.
     * @param {String} id The current bookmark list id.
     * @param {String} name The name of the bookmark list.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _setBookmarkListName( root, id, name ) {
        if ( _debug ) {
            console.log( '---------- _setBookmarkListName() ----------' );
            console.log( '_setBookmarkListName: root - ', root );
            console.log( '_setBookmarkListName: id - ', id );
            console.log( '_setBookmarkListName: name - ', name );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/get/' + id + '/set/name/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## DELETE ######## */
    /**
     * Method to delete an item with PI, LOGID and PAGE from the user bookmark list.
     * 
     * @method _deleteBookmarkListItem
     * @param {String} root The application root path.
     * @param {String} id The current bookmark list id.
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookmarkListItem( root, id, pi, logid, page ) {
        if ( _debug ) {
            console.log( '---------- _deleteBookmarkListItem() ----------' );
            console.log( '_deleteBookmarkListItem: root - ', root );
            console.log( '_deleteBookmarkListItem: id - ', id );
            console.log( '_deleteBookmarkListItem: pi - ', pi );
            console.log( '_deleteBookmarkListItem: logid - ', logid );
            console.log( '_deleteBookmarkListItem: page - ', page );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/get/' + id + '/delete/' + pi + '/' + page + '/-/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to delete a bookmark list by ID.
     * 
     * @method _deleteBookmarkListById
     * @param {String} root The application root path.
     * @param {String} id The current bookmark list id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookmarkListById( root, id ) {
        if ( _debug ) {
            console.log( '---------- _deleteBookmarkListById() ----------' );
            console.log( '_deleteBookmarkListById: root - ', root );
            console.log( '_deleteBookmarkListById: id - ', id );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookmarks/user/delete/' + id + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## BUILD ######## */
    /**
     * Method to render a popup which contains bookmark list actions.
     * 
     * @method _renderBookmarkListPopup
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @param {Object} pos The position of the clicked button.
     * @param {Object} size The width and height of the clicked button.
     */
    function _renderBookmarkListPopup( pi, logid, page, pos, size ) {
        if ( _debug ) {
            console.log( '---------- _renderBookmarkListPopup() ----------' );
            console.log( '_renderBookmarkListPopup: pi - ', pi );
            console.log( '_renderBookmarkListPopup: logid - ', logid );
            console.log( '_renderBookmarkListPopup: page - ', page );
            console.log( '_renderBookmarkListPopup: pos - ', pos );
            console.log( '_renderBookmarkListPopup: size - ', size );
        }
        
        var pi = pi;
        var logid = logid;
        var page = page;
        var posTop = pos.top;
        var posLeft = pos.left;
        
        // remove all popups
        $( '.bookshelf-popup' ).remove();
        
        // DOM-Elements
        var bookmarkListPopup = $( '<div />' ).addClass( 'bookshelf-popup bottom' ).css( {
            'top': ( posTop + size.height ) + 10 + 'px',
            'left': ( posLeft - 142 ) + ( size.width / 2 ) + 'px'
        } );
        var bookmarkListPopupLoader = $( '<div />' ).addClass( 'bookshelf-popup__body-loader' );
        bookmarkListPopup.append( bookmarkListPopupLoader );
        
        // build popup header
        var bookmarkListPopupHeader = $( '<div />' ).addClass( 'bookshelf-popup__header' ).text( _defaults.msg.selectBookmarkList );
        
        // type radio buttons
        var bookmarkListPopupRadioButtons = $( '<div />' ).addClass( 'bookmarkList-popup__radio-buttons' );
        var bookmarkListPopupRadioLabel = $( '<div />' ).text( _defaults.msg.type_label );
        var bookmarkListPopupRadioDivLeft = $( '<div />' ).addClass( 'col-xs-6 no-padding' );
        var bookmarkListPopupRadioDivRight = $( '<div />' ).addClass( 'col-xs-6 no-padding' );
        var	bookmarkListPopupTypeRadioButton1 = $( '<button />' ).addClass( 'btn btn--clean' ).attr( 'type', 'radio' ).attr( 'name', 'selectedType' ).attr('value', 'record').text( _defaults.msg.typeRecord );
        var bookmarkListPopupTypeRadioButton2 = $( '<button />' ).addClass( 'btn btn--clean' ).attr( 'type', 'radio' ).attr( 'name', 'selectedType' ).attr('value', 'page').text( _defaults.msg.typePage );
        bookmarkListPopupRadioDivLeft.append(bookmarkListPopupTypeRadioButton1);
        bookmarkListPopupRadioDivRight.append(bookmarkListPopupTypeRadioButton2);
        bookmarkListPopupRadioButtons.append(bookmarkListPopupRadioLabel).append(bookmarkListPopupRadioDivLeft).append(bookmarkListPopupRadioDivRight);
        
        // build popup body
        var bookmarkListPopupBody = $( '<div />' ).addClass( 'bookshelf-popup__body' );

        // build popup footer
        var bookmarkListPopupFooter = $( '<div />' ).addClass( 'bookshelf-popup__footer' );
        var bookmarkListPopupFooterRow = $( '<div />' ).addClass( 'row no-margin' );
        var bookmarkListPopupFooterColLeft = $( '<div />' ).addClass( 'col-xs-11 no-padding' );
        var bookmarkListPopupFooterInput = $( '<input />' ).attr( 'type', 'text' ).attr( 'placeholder', _defaults.msg.addNewBookmarkList );
        bookmarkListPopupFooterColLeft.append( bookmarkListPopupFooterInput );
        var bookmarkListPopupFooterColright = $( '<div />' ).addClass( 'col-xs-1 no-padding' );
        var bookmarkListPopupFooterAddBtn = $( '<button />' ).addClass( 'btn btn--clean' ).attr( 'type', 'button' ).attr( 'data-bookmark-list-type', 'add' ).attr( 'data-pi', pi ).attr( 'data-logid', logid ).attr( 'data-page', page );
        bookmarkListPopupFooterColright.append( bookmarkListPopupFooterAddBtn );
        bookmarkListPopupFooterRow.append( bookmarkListPopupFooterColLeft ).append( bookmarkListPopupFooterColright );
        bookmarkListPopupFooter.append( bookmarkListPopupFooterRow );
        
        // build popup
        bookmarkListPopup.append( bookmarkListPopupRadioButtons ).append( bookmarkListPopupHeader ).append( bookmarkListPopupBody ).append( bookmarkListPopupFooter );
        
        // append popup
        $( 'body' ).append( bookmarkListPopup );
        
        var selectedType = $ ( '#selectedType' ).value;
        
        // render bookmark list list
        _renderBookmarkListPopoverList( pi, logid, page, selectedType );
        
        // add new bookmark list in popover
        $( '.bookshelf-popup__footer [data-bookmark-list-type="add"]' ).on( 'click', function() {
            var bsName = $( '.bookshelf-popup__footer input' ).val();
            var currPi = $( this ).attr( 'data-pi' );
            var currLogid = $( this ).attr( 'data-logid' );
            var currPage = $( this ).attr( 'data-page' );
            
            if ( bsName != '' ) {
                _addNamedBookmarkList( _defaults.root, bsName ).then( function() {
                    $( '.bookshelf-popup__footer input' ).val( '' );
                    _renderBookmarksPopoverList( currPi, logid, currPage, selectedType );
                    _renderBookmarksNavigationList();
                } ).fail( function( error ) {
                    console.error( 'ERROR - _addNamedBookmarkList: ', error.responseText );
                } );
            }
            else {
                _addAutomaticNamedBookmarkList( _defaults.root ).then( function() {
                    $( '.bookshelf-popup__footer input' ).val( '' );
                    _renderBookmarksPopoverList( currPi, logid, currPage, selectedType );
                    _renderBookmarksNavigationList();
                } ).fail( function( error ) {
                    console.error( 'ERROR - _addAutomaticNamedBookmarkList: ', error.responseText );
                } );
            }
        } );
        
        // update bookmark list list when type has been changed
        $( '#selectedType' ).on( 'click', function() {
        	 // render bookmark list list
            _renderBookmarkListPopoverList( pi, logid, page, selectedType );
        } );
        
        // add new bookmark list on enter in popover
        $( '.bookshelf-popup__footer input' ).on( 'keyup', function( event ) {
            if ( event.which == 13 ) {
                $( '.bookshelf-popup__footer [data-bookmark-list-type="add"]' ).click();
            }
        } );
    }
    /**
     * Method to render the element list in bookmark list popover.
     * 
     * @method _renderBookmarksPopoverList
     * @param {String} pi The current PI of the selected item.
     */
    function _renderBookmarksPopoverList( pi, logid, page, type ) {
        if ( _debug ) {
            console.log( '---------- _renderBookmarksPopoverList() ----------' );
            console.log( '_renderBookmarksPopoverList: pi - ', pi, ', logid - ', logid, ', page = ', page, ', type = ', type);
        }
        
        _getAllBookmarkLists( _defaults.root ).then( function( elements ) {
            // DOM-Elements
            var dropdownList = $( '<ul />' ).addClass( 'bookshelf-popup__body-list list' );
            var dropdownListItem = null;
            var dropdownListItemText = null;
            var dropdownListItemAdd = null;
            var dropdownListItemIsInBookmarkList = null;
            var dropdownListItemAddCounter = null;
            
            if ( elements.length > 0 ) {
                elements.forEach( function( item ) {
                    dropdownListItem = $( '<li />' );
                    dropdownListItemIsInBookmarkList = '';
                    // check if item is in bookmark list
                    item.items.forEach( function( object ) {
                        if ( object.pi == pi && object.logid == logid && object.page == page ) {
                            dropdownListItemIsInBookmarkList = '<i class="fa fa-check" aria-hidden="true"></i> ';
                        }
                    } );
                    dropdownListItemAddCounter = $( '<span />' ).text( item.items.length );
                    dropdownListItemAdd = $( '<button />' ).addClass( 'btn btn--clean' ).attr( 'type', 'button' )
                    		.attr( 'data-bookmark-list-type', 'add' ).attr( 'data-id', item.id )
                            .attr( 'data-pi', pi ).attr( 'data-logid', logid ).attr( 'data-page', page ).attr( 'data-type', type )
                            .text( item.name ).prepend( dropdownListItemIsInBookmarkList ).append( dropdownListItemAddCounter );
                    
                    // build bookmark list item
                    dropdownListItem.append( dropdownListItemAdd );
                    dropdownList.append( dropdownListItem );
                } );
            }
            else {
                // add empty list item
                dropdownListItem = $( '<li />' );
                dropdownListItemText = $( '<span />' ).addClass( 'empty' ).text( _defaults.msg.noItemsAvailable );
                
                dropdownListItem.append( dropdownListItemText );
                dropdownList.append( dropdownListItem );
            }
            
            // render complete list
            $( '.bookshelf-popup__body' ).empty().append( dropdownList );
            
            // remove loader
            $( '.bookmark list-popup__body-loader' ).remove();
            
            // add item to bookmark list
            $( '.bookshelf-popup__body-list [data-bookmark-list-type="add"]' ).on( 'click', function() {
                var currBtn = $( this );
                var currId = currBtn.attr( 'data-id' );
                var currPi = currBtn.attr( 'data-pi' );
                var currLogid = currBtn.attr( 'data-logid' );
                var currPage = currBtn.attr( 'data-page' );
                var currType = currBtn.attr( 'data-type' );
                var isChecked = currBtn.find( '.fa-check' );
                
                if ( isChecked.length > 0 ) {
                    _deleteBookmark( _defaults.root, currId, currPi, currLogid, currPage ).then( function() {
                        _renderBookmarksPopoverList( currPi, currLogid, currPage, currType );
                        _renderBookmarksNavigationList();
                        _setAddedStatus();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _deleteBookmark: ', error.responseText );
                    } );
                }
                else {
                    _addBookmark( _defaults.root, currId, currPi, currLogid, currPage ).then( function() {
                        _renderBookmarksPopoverList( currPi, currLogid, currPage, currType );
                        _renderBookmarksNavigationList();
                        _setAddedStatus();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _addBookmark: ', error.responseText );
                    } );
                }
                
            } );
            
        } ).fail( function( error ) {
            console.error( 'ERROR - _getAllBookmarkLists: ', error.responseText );
        } );
    }
    /**
     * Method to render the element list in bookmark list navigation.
     * 
     * @method _renderBookmarksNavigationList
     */
    function _renderBookmarksNavigationList() {
        if ( _debug ) {
            console.log( '---------- _renderBookmarksNavigationList() ----------' );
        }
        
        var allBookmarks = 0;
        
        _getAllBookmarkLists( _defaults.root ).then( function( elements ) {
            // DOM-Elements
            var dropdownList = $( '<ul />' ).addClass( 'bookshelf-navigation__dropdown-list list' );
            var dropdownListItem = null;
            var dropdownListItemRow = null;
            var dropdownListItemLeft = null;
            var dropdownListItemRight = null;
            var dropdownListItemText = null;
            var dropdownListItemLink = null;
            var dropdownListItemAddCounter = null;
            
            if ( elements.length > 0 ) {
                elements.forEach( function( item ) {
                    dropdownListItem = $( '<li />' );
                    dropdownListItemRow = $( '<div />' ).addClass( 'row no-margin' );
                    dropdownListItemLeft = $( '<div />' ).addClass( 'col-xs-10 no-padding' );
                    dropdownListItemRight = $( '<div />' ).addClass( 'col-xs-2 no-padding' );
                    dropdownListItemLink = $( '<a />' ).attr( 'href', _defaults.root + '/user/bookmarks/' + item.id + '/' ).text( item.name );
                    dropdownListItemAddCounter = $( '<span />' ).addClass( 'bookshelf-navigation__dropdown-list-counter' ).text( item.items.length );
                    
                    // build bookmark
                    dropdownListItemLeft.append( dropdownListItemLink );
                    dropdownListItemRight.append( dropdownListItemAddCounter );
                    dropdownListItemRow.append( dropdownListItemLeft ).append( dropdownListItemRight )
                    dropdownListItem.append( dropdownListItemRow );
                    dropdownList.append( dropdownListItem );
                    
                    // raise bookmark counter
                    allBookmarks += item.items.length;
                } );
                
                // set bookmark counter
                $( '[data-bookmark-list-type="counter"]' ).empty().text( allBookmarks ).addClass( 'in' );
            }
            else {
                // add empty list item
                dropdownListItem = $( '<li />' );
                dropdownListItemText = $( '<span />' ).addClass( 'empty' ).text( _defaults.msg.noItemsAvailable );
                
                dropdownListItem.append( dropdownListItemText );
                dropdownList.append( dropdownListItem );
                
                // set bookmark counter
                $( '[data-bookmark-list-type="counter"]' ).empty().text( allBookmarks ).addClass( 'in' );
            }
            
            // render complete list
            $( '.bookshelf-navigation__dropdown-list' ).empty().append( dropdownList );
            
        } ).fail( function( error ) {
            console.error( 'ERROR - _getAllBookmarkLists: ', error.responseText );
        } );
    }
    
    /**
     * Method to set an 'added' status to an object.
     * 
     * @method _setAddedStatus
     */
    function _setAddedStatus() {
        if ( _debug ) {
            console.log( '---------- _setAddedStatus() ----------' );
        }
        
        $( '[data-bookmark-list-type="add"]' ).each( function() {
            var currTrigger = $( this );
            var currTriggerPi = currTrigger.attr( 'data-pi' );
            var currTriggerLogid = currTrigger.attr( 'data-logid' );
            var currTriggerPage = currTrigger.attr( 'data-page' );
            var currTriggerType = currTrigger.attr( 'data-type' );
            
            _isItemInBookmarkList( currTrigger, currTriggerPi, currTriggerLogid, currTriggerPage, currTriggerType );
        } );
    }
    /**
     * Method to check if item is in any bookmark list.
     * 
     * @method _isItemInBookmarkList
     * @param {Object} object An jQuery-Object of the current item.
     * @param {String} pi The current PI of the selected item.
     * @param {String} logid
     * @param {String} page
     * @param {String} type
     */
    function _isItemInBookmarkList( object, pi, logid, page, type ) {
        if ( _debug ) {
            console.log( '---------- _isItemInBookmarkList() ----------' );
            console.log( '_isItemInBookmarkList: pi - ', pi );
            console.log( '_isItemInBookmarkList: logid - ', logid );
            console.log( '_isItemInBookmarkList: page - ', page );
            console.log( '_isItemInBookmarkList: object - ', object );
        }
        
        if ( type === 'page' ) {
        _getContainingBookmark( _defaults.root, pi, logid, page ).then( function( items ) {
            if ( items.length == 0 ) {
                object.removeClass( 'added' );
                
                return false;
            }
            else {
                object.addClass( 'added' );
            }
        } ).fail( function( error ) {
            console.error( 'ERROR - _getContainingBookmarkByPi: ', error.responseText );
        } );
        } else {
            _getContainingBookmark( _defaults.root, pi, null, null ).then( function( items ) {
                if ( items.length == 0 ) {
                    object.removeClass( 'added' );
                    
                    return false;
                }
                else {
                    object.addClass( 'added' );
                }
            } ).fail( function( error ) {
                console.error( 'ERROR - _getContainingBookmarkByPi: ', error.responseText );
            } );
        }
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
