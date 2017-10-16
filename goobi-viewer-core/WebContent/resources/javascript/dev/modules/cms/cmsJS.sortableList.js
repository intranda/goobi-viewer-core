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
 * Module which enables a sortable List based on jQuery UI. This list is used for the
 * mainmenu items and dynamic sidebar elements.
 * 
 * @version 3.2.0
 * @module cmsJS.sortableList
 * @requires jQuery, jQuery UI
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    var _debug = false;
    var _levelIndent = 0;
    var _allowMultipleOccurances = false;
    var _inputField = null;
    var _sortingAttribute = 'sortposition';
    
    cms.sortableList = {
        /**
         * Method which initializes the CMS sortable list items and sets events.
         * 
         * @method init
         * @param {String} indent
         * @param {Boolean} allowMultiple
         * @param {Object} config
         */
        init: function( indent, allowMultiple, config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.sortableList.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.sortableList.init: indent - ', indent );
                console.log( 'cmsJS.sortableList.init: allowMultiple - ', allowMultiple );
                console.log( 'cmsJS.sortableList.init: inputTextField - ', config.sortablesConfig.componentListInput );
            }
            
            _levelIndent = indent;
            _allowMultipleOccurances = allowMultiple;
            _inputField = config.sortablesConfig.componentListInput;
            
            // validation
            if ( _inputField === null ) {
                console.warn( "Input field for item order not found: No order information can be saved!" );
            }
            if ( config.sortablesConfig.visibleItemList.length === 0 ) {
                console.error( "No container for active items found. Cannot initialize sortable" );
            }
            if ( config.sortablesConfig.availableItemList.length === 0 ) {
                console.error( "No container for available items found. Cannot initialize sortable" );
            }
            
            _updateAllSortIcons();
            
            if ( _debug )
                console.log( "Make sortable ", config.sortablesConfig.visibleItemList );
            config.sortablesConfig.visibleItemList.sortable( {
                update: _serializeVisibleItems,
                connectWith: "#availableItemList"
            } );
            
            if ( _debug )
                console.log( "Make sortable ", config.sortablesConfig.availableItemList );
            config.sortablesConfig.availableItemList.sortable( {
                update: _serializeVisibleItems,
                connectWith: "#visibleItemList",
                helper: "clone"
            } );
            
            _serializeVisibleItems();
            $( "#availableItemList" ).on( "sortbeforestop", _handleBeforeDropFromAvailable );
            $( "#visibleItemList" ).on( "sortbeforestop", _handleBeforeDropFromVisible );
            $( "#availableItemList" ).on( "sortstop", _handleDrop );
            $( "#visibleItemList" ).on( "sortstop", _handleDrop );
        },
        
        /**
         * Method which
         * 
         * @method decreaseLevel
         * @param {String} element
         * @param {String} applyToNext
         */
        decreaseLevel: function( element, applyToNext ) {
            if ( _debug ) {
                console.log( 'cmsJS.sortableList.decreaseLevel: element - ', element );
                console.log( 'cmsJS.sortableList.decreaseLevel: applyToNext - ', applyToNext );
            }
            
            var item = _getJQueryItem( element );
            var level;
            
            if ( _getLevel( item ) > 0 ) {
                level = _changePos( item, -1 );
                if ( applyToNext ) {
                    var nextItem = item.next();
                    while ( nextItem !== null && _getLevel( nextItem ) > _getLevel( item ) + 1 ) {
                        cms.sortableList.decreaseLevel( nextItem, false );
                        nextItem = nextItem.next();
                    }
                }
            }
            
            _serializeVisibleItems();
            _updateAllSortIcons();
        },
        
        /**
         * Method which
         * 
         * @method increaseLevel
         * @param {String} element
         * @param {String} applyToNext
         */
        increaseLevel: function( element, applyToNext ) {
            if ( _debug ) {
                console.log( 'cmsJS.sortableList.increaseLevel: element - ', element );
                console.log( 'cmsJS.sortableList.increaseLevel: applyToNext - ', applyToNext );
            }
            var item = _getJQueryItem( element );
            var prevItem = item.prev();
            
            if ( _getLevel( item ) <= _getLevel( prevItem ) ) {
                _changePos( item, 1 );
                if ( applyToNext ) {
                    var nextItem = item.next();
                    while ( nextItem !== null && _getLevel( nextItem ) >= _getLevel( item ) ) {
                        cms.sortableList.increaseLevel( nextItem, false );
                        nextItem = nextItem.next();
                    }
                }
            }
            
            _serializeVisibleItems();
            _updateAllSortIcons();
        },
        
        save: function( ajaxData ) {
            if ( typeof ajaxData === "undefined" || ajaxData.status === "begin" ) {
                _serializeVisibleItems();
            }
        }
    };
    
    /**
     * (Privat) Method which
     * 
     * @method _handleBeforeDropFromAvailable
     * @param {String} event
     * @param {String} ui
     */
    function _handleBeforeDropFromAvailable( event, ui ) {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _handleBeforeDropFromAvailable: event - ', event );
            console.log( 'cmsJS.sortableList _handleBeforeDropFromAvailable: ui - ', ui );
        }
        
        var $item = $( ui.item );

        var $radioMenues = $item.find("table");
        $radioMenues.each(function(index, element) {
        	var $checkboxes = $(element).find("input");
        	if($checkboxes.length > 0) {
        		var anychecked = false;
        		$checkboxes.each(function(index, element) {
        			if($(element).prop('checked')) {
        				anychecked = true;
        				return false;
        			}
        		})
        		if(!anychecked) {
        			$checkboxes.first().prop('checked', true);
        		}
        	}
        })
        if ( _allowMultipleOccurances && $item.parent().attr( "id" ) === "visibleItemList" ) {
            $item.clone().appendTo( $( "#availableItemList" ) );
        }
        
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _handleBeforeDropFromAvailable
     * @param {String} event
     * @param {String} ui
     */
    function _handleBeforeDropFromVisible( event, ui ) {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _handleBeforeDropFromVisible: event - ', event );
            console.log( 'cmsJS.sortableList _handleBeforeDropFromVisible: ui - ', ui );
        }
        
        var item = $( ui.item );
        if ( _allowMultipleOccurances && item.parent().attr( "id" ) === "availableItemList" ) {
            ui.item.remove();
        }
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _handleDrop
     */
    function _handleDrop() {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _handleDrop' );
        }
        _updateAllSortIcons();
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _updateAllSortIcons
     */
    function _updateAllSortIcons() {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _updateAllSortIcons' );
        }
        
        var childrenVL = $( "#visibleItemList" ).children( "li" );
        childrenVL.each( function() {
            if ( $( this ).attr( _sortingAttribute ) != null ) {
                _updateSortIcons( $( this ) );
            }
        } );
        
        var childrenAL = $( "#availableItemList" ).children( "li" );
        childrenAL.each( function() {
            if ( $( this ).attr( _sortingAttribute ) != null ) {
                _updateSortIcons( $( this ) );
            }
        } );
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _updateSortIcons
     * @param {Object} item
     */
    function _updateSortIcons( item ) {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _updateSortIcons: item - ', item );
        }
        
        var parent = item.parent();
        
        if ( parent.attr( "id" ) === "visibleItemList" ) {
            item.children( '.menu-item__level' ).show();
            if ( _getLevel( item.prev() ) === -1 ) {
                while ( _getLevel( item ) > 0 ) {
                    cms.sortableList.decreaseLevel( item );
                }
            }
            
            if ( _getLevel( item ) === 0 ) {
                item.find( '.left' ).css( "visibility", "hidden" );
                item.find( '.right' ).css( "visibility", "hidden" );
                item.css( "margin-left", "0px" );
            }
            else {
                item.find( '.left' ).css( "visibility", "visible" );
                item.find( '.right' ).css( "visibility", "visible" );
                item.css( "margin-left", _getLevel( item ) * _levelIndent + "px" );
            }
            
            if ( _getLevel( item ) > _getLevel( item.prev() ) ) {
                item.find( '.left' ).css( "visibility", "visible" );
                item.find( '.right' ).css( "visibility", "hidden" );
            }
            else {
                item.find( '.left' ).css( "visibility", "visible" );
                item.find( '.right' ).css( "visibility", "visible" );
            }
        }
        else {
            item.children( '.menu-item__level' ).hide();
        }
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _getJQueryItem
     * @param {Object} element
     */
    function _getJQueryItem( element ) {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _getJQueryItem: element - ', element );
        }
        
        var item = $( element );
        
        while ( item !== null && item.attr( _sortingAttribute ) === undefined ) {
            item = item.parent();
        }
        
        return item;
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _getLevel
     * @param {Object} item
     */
    function _getLevel( item ) {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _getLevel: item - ', item );
        }
        
        var pos = item.attr( _sortingAttribute );
        
        if ( pos === null || pos === undefined ) {
            return -1;
        }
        
        var curLevel = pos.substr( pos.indexOf( '?' ) + 1 );
        var level = parseInt( curLevel );
        
        return level;
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _changePos
     * @param {Object} item
     * @param {String} diff
     */
    function _changePos( item, diff ) {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _changePos: item - ', item );
            console.log( 'cmsJS.sortableList _changePos: diff - ', diff );
        }
        
        var pos = item.attr( _sortingAttribute );
        
        if ( pos === null || pos === undefined ) {
            return -1;
        }
        
        var curLevel = pos.substr( pos.indexOf( '?' ) + 1 );
        var curItem = pos.substr( pos.indexOf( '_' ) + 1 );
        var curItemInt = parseInt( curItem );
        var level = parseInt( curLevel ) + diff;
        item.attr( _sortingAttribute, "item_" + curItemInt + '?' + level );
        
        return level;
    }
    
    /**
     * (Privat) Method which
     * 
     * @method _serializeVisibleItems
     */
    function _serializeVisibleItems() {
        if ( _debug ) {
            console.log( 'cmsJS.sortableList _serializeVisibleItems' );
        }
        
        var postData = $( "#visibleItemList" ).sortable( "serialize", {
            key: "item",
            attribute: _sortingAttribute
        } );
        
        if ( _inputField !== null ) {
            _inputField.value = postData;
            // postData = $("#itemOrderInput").val();
        }
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
