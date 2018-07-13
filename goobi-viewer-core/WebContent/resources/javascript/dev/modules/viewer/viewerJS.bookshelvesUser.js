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
 * Module to manage bookshelves if the user is logged in.
 * 
 * @version 3.2.0
 * @module viewerJS.bookshelvesUser
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        root: '',
        msg: {
            resetBookshelves: '',
            resetBookshelvesConfirm: '',
            noItemsAvailable: '',
            selectBookshelf: '',
            addNewBookshelf: ''
        }
    };
    
    viewer.bookshelvesUser = {
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.bookshelvesUser.init' );
                console.log( '##############################' );
                console.log( 'viewer.bookshelvesUser.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // render bookshelf navigation list
            _renderBookshelfNavigationList();
            
            // toggle bookshelf dropdown
            $( '[data-bookshelf-type="dropdown"]' ).off().on( 'click', function( event ) {
                event.stopPropagation();
                
                // hide other dropdowns
                $( '.login-navigation__login-dropdown, .login-navigation__user-dropdown, .navigation__collection-panel' ).hide();
                $( '.bookshelf-popup' ).remove();
                
                $( '.bookshelf-navigation__dropdown' ).slideToggle( 'fast' );
            } );
            
            // check if element is in any bookshelf
            _setAddedStatus();
            
            // render bookshelf popup
            $( '[data-bookshelf-type="add"]' ).off().on( 'click', function( event ) {
                event.stopPropagation();
                
                // hide other dropdowns
                $( '.bookshelf-navigation__dropdown, .login-navigation__user-dropdown' ).hide();
                
                var currBtn = $( this );
                var currPi = currBtn.attr( 'data-pi' );
                var currLogid = currBtn.attr( 'data-logid' );
                var currPage = currBtn.attr( 'data-page' );
                var currPos = currBtn.offset();
                var currSize = {
                    width: currBtn.outerWidth(),
                    height: currBtn.outerHeight()
                };
                
                // render bookshelf popup
                _renderBookshelfPopup( currPi, currLogid, currPage, currPos, currSize );
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
            
            // add new bookshelf in overview
            $( '#addBookshelfBtn' ).off().on( 'click', function() {
                var bsName = $( '#addBookshelfInput' ).val();
                
                if ( bsName != '' ) {
                    _addNamedBookshelf( _defaults.root, bsName ).then( function() {
                        location.reload();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _addNamedBookshelf: ', error.responseText );
                    } );
                }
                else {
                    _addAutomaticNamedBookshelf( _defaults.root ).then( function() {
                        location.reload();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _addAutomaticNamedBookshelf: ', error.responseText );
                    } );
                }
            } );
            
            // add new bookshelf on enter in overview
            $( '#addBookshelfInput' ).on( 'keyup', function( event ) {
                if ( event.which == 13 ) {
                    $( '#addBookshelfBtn' ).click();
                }
            } );
            
            // set bookshelf id to session storage for mirador view
            $( ".view-mirador__link" ).on( "click", function() {
        		var currBookshelfId = $( this ).attr( "data-bookshelf-id" );
                
        		sessionStorage.setItem( 'bookshelfId', currBookshelfId );
        	} );
        }
    };
    /* ######## ADD (CREATE) ######## */
    /**
     * Method to add an item to the user bookshelf by PI.
     * 
     * @method _addBookshelfItemByPi
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addBookshelfItemByPi( root, id, pi ) {
        if ( _debug ) {
            console.log( '---------- _addBookshelfItemByPi() ----------' );
            console.log( '_addBookshelfItemByPi: root - ', root );
            console.log( '_addBookshelfItemByPi: id - ', id );
            console.log( '_addBookshelfItemByPi: pi - ', pi );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/add/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add an item with PI, LOGID and PAGE to the user bookshelf.
     * 
     * @method _addBookshelfItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addBookshelfItemByPiLogidPage( root, id, pi, logid, page ) {
        if ( _debug ) {
            console.log( '---------- _addBookshelfItemByPiLogidPage() ----------' );
            console.log( '_addBookshelfItemByPiLogidPage: root - ', root );
            console.log( '_addBookshelfItemByPiLogidPage: id - ', id );
            console.log( '_addBookshelfItemByPiLogidPage: pi - ', pi );
            console.log( '_addBookshelfItemByPiLogidPage: logid - ', logid );
            console.log( '_addBookshelfItemByPiLogidPage: page - ', page );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/add/' + pi + '/' + logid + '/' + page + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add a named bookshelf to the user account.
     * 
     * @method _addNamedBookshelf
     * @param {String} root The application root path.
     * @param {String} name The name of the bookshelf.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addNamedBookshelf( root, name ) {
        if ( _debug ) {
            console.log( '---------- _addNamedBookshelf() ----------' );
            console.log( '_addNamedBookshelf: root - ', root );
            console.log( '_addNamedBookshelf: name - ', name );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/add/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add a automatic named bookshelf to the user account.
     * 
     * @method _addAutomaticNamedBookshelf
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addAutomaticNamedBookshelf( root ) {
        if ( _debug ) {
            console.log( '---------- _addAutomaticNamedBookshelf() ----------' );
            console.log( '_addAutomaticNamedBookshelf: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/add/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add the session bookshelf to the user account with an automatic generated
     * name.
     * 
     * @method _addSessionBookshelf
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addSessionBookshelf( root ) {
        if ( _debug ) {
            console.log( '---------- _addSessionBookshelf() ----------' );
            console.log( '_addSessionBookshelf: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/addSessionBookshelf/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to add the session bookshelf to the user account with a given name.
     * 
     * @method _addSessionBookshelfNamed
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _addSessionBookshelfNamed( root, name ) {
        if ( _debug ) {
            console.log( '---------- _addSessionBookshelfNamed() ----------' );
            console.log( '_addSessionBookshelfNamed: root - ', root );
            console.log( '_addSessionBookshelfNamed: name - ', name );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/addSessionBookshelf/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## GET (READ) ######## */
    /**
     * Method to get all user bookshelves.
     * 
     * @method _getAllBookshelves
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getAllBookshelves( root ) {
        if ( _debug ) {
            console.log( '---------- _getAllBookshelves() ----------' );
            console.log( '_getAllBookshelves: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get a bookshelf by id.
     * 
     * @method _getBookshelfById
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getBookshelfById( root, id ) {
        if ( _debug ) {
            console.log( '---------- _getBookshelfById() ----------' );
            console.log( '_getBookshelfById: root - ', root );
            console.log( '_getBookshelfById: id - ', id );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get the number of items in the selected bookshelf.
     * 
     * @method _getBookshelfItemCount
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getBookshelfItemCount( root, id ) {
        if ( _debug ) {
            console.log( '---------- _getBookshelfItemCount() ----------' );
            console.log( '_getBookshelfItemCount: root - ', root );
            console.log( '_getBookshelfItemCount: id - ', id );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/count/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get all public bookshelves.
     * 
     * @method _getPublicBookshelves
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getPublicBookshelves( root ) {
        if ( _debug ) {
            console.log( '---------- _getPublicBookshelves() ----------' );
            console.log( '_getPublicBookshelves: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/public/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to get all shared bookshelves.
     * 
     * @method _getSharedBookshelves
     * @param {String} root The application root path.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getSharedBookshelves( root ) {
        if ( _debug ) {
            console.log( '---------- _getSharedBookshelves() ----------' );
            console.log( '_getSharedBookshelves: root - ', root );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/shared/get/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to check if an item by PI if it's in user bookshelf. It returns the
     * bookshelves or false if no items are in list.
     * 
     * @method _getContainingBookshelfItemByPi
     * @param {String} root The application root path.
     * @param {String} pi The pi of the current item.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getContainingBookshelfItemByPi( root, pi ) {
        if ( _debug ) {
            console.log( '---------- _getContainingBookshelfItemByPi() ----------' );
            console.log( '_getContainingBookshelfItemByPi: root - ', root );
            console.log( '_getContainingBookshelfItemByPi: pi - ', pi );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/contains/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to check if an item by PI, LOGID and PAGE if it's in user bookshelf. It
     * returns the bookshelves or false if no items are in list.
     * 
     * @method _getContainingBookshelfItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} pi The pi of the current item.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _getContainingBookshelfItemByPiLogidPage( root, pi, logid, page ) {
        if ( _debug ) {
            console.log( '---------- _getContainingBookshelfItemByPiLogidPage() ----------' );
            console.log( '_getContainingBookshelfItemByPiLogidPage: root - ', root );
            console.log( '_getContainingBookshelfItemByPiLogidPage: pi - ', pi );
            console.log( '_getContainingBookshelfItemByPiLogidPage: logid - ', logid );
            console.log( '_getContainingBookshelfItemByPiLogidPage: page - ', page );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/contains/' + pi + '/' + logid + '/' + page + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## SET (UPDATE) ######## */
    /**
     * Method to set the name of a bookshelf.
     * 
     * @method _setBookshelfName
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} name The name of the bookshelf.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _setBookshelfName( root, id, name ) {
        if ( _debug ) {
            console.log( '---------- _setBookshelfName() ----------' );
            console.log( '_setBookshelfName: root - ', root );
            console.log( '_setBookshelfName: id - ', id );
            console.log( '_setBookshelfName: name - ', name );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/set/name/' + name + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## DELETE ######## */
    /**
     * Method to delete an item from the user bookshelf by PI.
     * 
     * @method _deleteBookshelfItemByPi
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookshelfItemByPi( root, id, pi ) {
        if ( _debug ) {
            console.log( '---------- _deleteBookshelfItemByPi() ----------' );
            console.log( '_deleteBookshelfItemByPi: root - ', root );
            console.log( '_deleteBookshelfItemByPi: id - ', id );
            console.log( '_deleteBookshelfItemByPi: pi - ', pi );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/delete/' + pi + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to delete an item with PI, LOGID and PAGE from the user bookshelf.
     * 
     * @method _deleteBookshelfItemByPiLogidPage
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookshelfItemByPiLogidPage( root, id, pi, logid, page ) {
        if ( _debug ) {
            console.log( '---------- _deleteBookshelfItemByPiLogidPage() ----------' );
            console.log( '_deleteBookshelfItemByPiLogidPage: root - ', root );
            console.log( '_deleteBookshelfItemByPiLogidPage: id - ', id );
            console.log( '_deleteBookshelfItemByPiLogidPage: pi - ', pi );
            console.log( '_deleteBookshelfItemByPiLogidPage: logid - ', logid );
            console.log( '_deleteBookshelfItemByPiLogidPage: page - ', page );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/get/' + id + '/delete/' + pi + '/' + logid + '/' + page + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /**
     * Method to delete a bookshelf by ID.
     * 
     * @method _deleteBookshelfById
     * @param {String} root The application root path.
     * @param {String} id The current bookshelf id.
     * @returns {Promise} A promise that checks the existing items.
     */
    function _deleteBookshelfById( root, id ) {
        if ( _debug ) {
            console.log( '---------- _deleteBookshelfById() ----------' );
            console.log( '_deleteBookshelfById: root - ', root );
            console.log( '_deleteBookshelfById: id - ', id );
        }
        
        var promise = Q( $.ajax( {
            url: root + '/rest/bookshelves/user/delete/' + id + '/',
            type: "GET",
            dataType: "JSON",
            async: true
        } ) );
        
        return promise;
    }
    /* ######## BUILD ######## */
    /**
     * Method to render a popup which contains bookshelf actions.
     * 
     * @method _renderBookshelfPopup
     * @param {String} pi The pi of the item to add.
     * @param {String} logid The logid of the item to add.
     * @param {String} page The page of the item to add.
     * @param {Object} pos The position of the clicked button.
     * @param {Object} size The width and height of the clicked button.
     */
    function _renderBookshelfPopup( pi, logid, page, pos, size ) {
        if ( _debug ) {
            console.log( '---------- _renderBookshelfPopup() ----------' );
            console.log( '_renderBookshelfPopup: pi - ', pi );
            console.log( '_renderBookshelfPopup: logid - ', logid );
            console.log( '_renderBookshelfPopup: page - ', page );
            console.log( '_renderBookshelfPopup: pos - ', pos );
            console.log( '_renderBookshelfPopup: size - ', size );
        }
        
        var pi = pi;
        var posTop = pos.top;
        var posLeft = pos.left;
        
        // remove all popups
        $( '.bookshelf-popup' ).remove();
        
        // DOM-Elements
        var bookshelfPopup = $( '<div />' ).addClass( 'bookshelf-popup bottom' ).css( {
            'top': ( posTop + size.height ) + 10 + 'px',
            'left': ( posLeft - 142 ) + ( size.width / 2 ) + 'px'
        } );
        var bookshelfPopupLoader = $( '<div />' ).addClass( 'bookshelf-popup__body-loader' );
        bookshelfPopup.append( bookshelfPopupLoader );
        
        // build popup header
        var bookshelfPopupHeader = $( '<div />' ).addClass( 'bookshelf-popup__header' ).text( _defaults.msg.selectBookshelf );
        
        // build popup body
        var bookshelfPopupBody = $( '<div />' ).addClass( 'bookshelf-popup__body' );
        
        // build popup footer
        var bookshelfPopupFooter = $( '<div />' ).addClass( 'bookshelf-popup__footer' );
        var bookshelfPopupFooterRow = $( '<div />' ).addClass( 'row no-margin' );
        var bookshelfPopupFooterColLeft = $( '<div />' ).addClass( 'col-xs-11 no-padding' );
        var bookshelfPopupFooterInput = $( '<input />' ).attr( 'type', 'text' ).attr( 'placeholder', _defaults.msg.addNewBookshelf );
        bookshelfPopupFooterColLeft.append( bookshelfPopupFooterInput );
        var bookshelfPopupFooterColright = $( '<div />' ).addClass( 'col-xs-1 no-padding' );
        var bookshelfPopupFooterAddBtn = $( '<button />' ).addClass( 'btn-clean' ).attr( 'type', 'button' ).attr( 'data-bookshelf-type', 'add' ).attr( 'data-pi', pi );
        bookshelfPopupFooterColright.append( bookshelfPopupFooterAddBtn );
        bookshelfPopupFooterRow.append( bookshelfPopupFooterColLeft ).append( bookshelfPopupFooterColright );
        bookshelfPopupFooter.append( bookshelfPopupFooterRow );
        
        // build popup
        bookshelfPopup.append( bookshelfPopupHeader ).append( bookshelfPopupBody ).append( bookshelfPopupFooter );
        
        // append popup
        $( 'body' ).append( bookshelfPopup );
        
        // render bookshelf list
        _renderBookshelfPopoverList( pi );
        
        // add new bookshelf in popover
        $( '.bookshelf-popup__footer [data-bookshelf-type="add"]' ).on( 'click', function() {
            var bsName = $( '.bookshelf-popup__footer input' ).val();
            var currPi = $( this ).attr( 'data-pi' );
            
            if ( bsName != '' ) {
                _addNamedBookshelf( _defaults.root, bsName ).then( function() {
                    $( '.bookshelf-popup__footer input' ).val( '' );
                    _renderBookshelfPopoverList( currPi );
                    _renderBookshelfNavigationList();
                } ).fail( function( error ) {
                    console.error( 'ERROR - _addNamedBookshelf: ', error.responseText );
                } );
            }
            else {
                _addAutomaticNamedBookshelf( _defaults.root ).then( function() {
                    $( '.bookshelf-popup__footer input' ).val( '' );
                    _renderBookshelfPopoverList( currPi );
                    _renderBookshelfNavigationList();
                } ).fail( function( error ) {
                    console.error( 'ERROR - _addAutomaticNamedBookshelf: ', error.responseText );
                } );
            }
        } );
        
        // add new bookshelf on enter in popover
        $( '.bookshelf-popup__footer input' ).on( 'keyup', function( event ) {
            if ( event.which == 13 ) {
                $( '.bookshelf-popup__footer [data-bookshelf-type="add"]' ).click();
            }
        } );
    }
    /**
     * Method to render the element list in bookshelf popover.
     * 
     * @method _renderBookshelfPopoverList
     * @param {String} pi The current PI of the selected item.
     */
    function _renderBookshelfPopoverList( pi ) {
        if ( _debug ) {
            console.log( '---------- _renderBookshelfPopoverList() ----------' );
            console.log( '_renderBookshelfPopoverList: pi - ', pi );
        }
        
        _getAllBookshelves( _defaults.root ).then( function( elements ) {
            // DOM-Elements
            var dropdownList = $( '<ul />' ).addClass( 'bookshelf-popup__body-list list' );
            var dropdownListItem = null;
            var dropdownListItemText = null;
            var dropdownListItemAdd = null;
            var dropdownListItemIsInBookshelf = null;
            var dropdownListItemAddCounter = null;
            
            if ( elements.length > 0 ) {
                elements.forEach( function( item ) {
                    dropdownListItem = $( '<li />' );
                    dropdownListItemIsInBookshelf = '';
                    // check if item is in bookshelf
                    item.items.forEach( function( object ) {
                        if ( object.pi == pi ) {
                            dropdownListItemIsInBookshelf = '<i class="fa fa-check" aria-hidden="true"></i> ';
                        }
                    } );
                    dropdownListItemAddCounter = $( '<span />' ).text( item.items.length );
                    dropdownListItemAdd = $( '<button />' ).addClass( 'btn-clean' ).attr( 'type', 'button' ).attr( 'data-bookshelf-type', 'add' ).attr( 'data-id', item.id )
                            .attr( 'data-pi', pi ).text( item.name ).prepend( dropdownListItemIsInBookshelf ).append( dropdownListItemAddCounter );
                    
                    // build bookshelf item
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
            $( '.bookshelf-popup__body-loader' ).remove();
            
            // add item to bookshelf
            $( '.bookshelf-popup__body-list [data-bookshelf-type="add"]' ).on( 'click', function() {
                var currBtn = $( this );
                var currId = currBtn.attr( 'data-id' );
                var currPi = currBtn.attr( 'data-pi' );
                var isChecked = currBtn.find( '.fa-check' );
                
                if ( isChecked.length > 0 ) {
                    _deleteBookshelfItemByPi( _defaults.root, currId, currPi ).then( function() {
                        _renderBookshelfPopoverList( currPi );
                        _renderBookshelfNavigationList();
                        _setAddedStatus();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _deleteBookshelfItemByPi: ', error.responseText );
                    } );
                }
                else {
                    _addBookshelfItemByPi( _defaults.root, currId, currPi ).then( function() {
                        _renderBookshelfPopoverList( currPi );
                        _renderBookshelfNavigationList();
                        _setAddedStatus();
                    } ).fail( function( error ) {
                        console.error( 'ERROR - _addBookshelfItemByPi: ', error.responseText );
                    } );
                }
                
            } );
            
        } ).fail( function( error ) {
            console.error( 'ERROR - _getAllBookshelves: ', error.responseText );
        } );
    }
    /**
     * Method to render the element list in bookshelf navigation.
     * 
     * @method _renderBookshelfNavigationList
     */
    function _renderBookshelfNavigationList() {
        if ( _debug ) {
            console.log( '---------- _renderBookshelfNavigationList() ----------' );
        }
        
        var allBookshelfItems = 0;
        
        _getAllBookshelves( _defaults.root ).then( function( elements ) {
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
                    dropdownListItemLink = $( '<a />' ).attr( 'href', _defaults.root + '/bookshelf/' + item.id + '/' ).text( item.name );
                    dropdownListItemAddCounter = $( '<span />' ).addClass( 'bookshelf-navigation__dropdown-list-counter' ).text( item.items.length );
                    
                    // build bookshelf item
                    dropdownListItemLeft.append( dropdownListItemLink );
                    dropdownListItemRight.append( dropdownListItemAddCounter );
                    dropdownListItemRow.append( dropdownListItemLeft ).append( dropdownListItemRight )
                    dropdownListItem.append( dropdownListItemRow );
                    dropdownList.append( dropdownListItem );
                    
                    // raise bookshelf item counter
                    allBookshelfItems += item.items.length;
                } );
                
                // set bookshelf item counter
                $( '[data-bookshelf-type="counter"]' ).empty().text( allBookshelfItems );
            }
            else {
                // add empty list item
                dropdownListItem = $( '<li />' );
                dropdownListItemText = $( '<span />' ).addClass( 'empty' ).text( _defaults.msg.noItemsAvailable );
                
                dropdownListItem.append( dropdownListItemText );
                dropdownList.append( dropdownListItem );
                
                // set bookshelf item counter
                $( '[data-bookshelf-type="counter"]' ).empty().text( allBookshelfItems );
            }
            
            // render complete list
            $( '.bookshelf-navigation__dropdown-list' ).empty().append( dropdownList );
            
        } ).fail( function( error ) {
            console.error( 'ERROR - _getAllBookshelves: ', error.responseText );
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
        
        $( '[data-bookshelf-type="add"]' ).each( function() {
            var currTrigger = $( this );
            var currTriggerPi = currTrigger.attr( 'data-pi' );
            
            _isItemInBookshelf( currTrigger, currTriggerPi );
        } );
    }
    /**
     * Method to check if item is in any bookshelf.
     * 
     * @method _isItemInBookshelf
     * @param {Object} object An jQuery-Object of the current item.
     * @param {String} pi The current PI of the selected item.
     */
    function _isItemInBookshelf( object, pi ) {
        if ( _debug ) {
            console.log( '---------- _isItemInBookshelf() ----------' );
            console.log( '_isItemInBookshelf: pi - ', pi );
            console.log( '_isItemInBookshelf: object - ', object );
        }
        
        _getContainingBookshelfItemByPi( _defaults.root, pi ).then( function( items ) {
            if ( items.length == 0 ) {
                object.removeClass( 'added' );
                
                return false;
            }
            else {
                object.addClass( 'added' );
            }
        } ).fail( function( error ) {
            console.error( 'ERROR - _getContainingBookshelfItemByPi: ', error.responseText );
        } );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
