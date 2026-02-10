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
 * Module which creates new CMS pages and sets their status.
 * 
 * @version 3.2.0
 * @module cmsJS.createPage
 * @requires jQuery
 * 
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    var _debug = false;
    var _previewStatus = '';
    var _defaults = {
        selectedPageID: null,
        inputFields: null,
        prevBtn: null,
        prevDescription: null,
        saveBtn: null,
        sortablesConfig: {}
    };
    
    cms.createPage = {
        /**
         * Method which initializes the CMS create page module.
         * 
         * @method init
         * @param {Object} settings
         */
        init: function( settings ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.createPage.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.createPage.init: settings - ', settings );
            }
            
            $.extend( true, _defaults, settings );
            
            if ( _defaults.selectedPageID === null ) {
                cmsJS.createPage.disablePreview();
                _defaults.prevDescription.show();
            }
            
            // listen to changes on input fields
            _defaults.inputFields.on( 'change input paste', function() {
                cmsJS.createPage.disablePreview();
                _defaults.prevDescription.show();
            } );
            
            cmsJS.createPage.initSortables( _defaults );
            cmsJS.createSlider.initStyleOptions();
            
            // check preview status and open new tab
            if ( sessionStorage.getItem( 'previewStatus' ) === 'false' ) {
                _defaults.prevBtn.attr( 'disabled', true );
                _defaults.prevDescription.show();
            }
            else {
                _defaults.prevBtn.attr( 'disabled', false );
                _defaults.prevDescription.hide();
                
                _defaults.prevBtn.on( 'click', function( event ) {
                	var url = $( this ).attr( 'data-previewUrl' );
                    window.open( url, url );
                } );
            }
        },
        
        /**
         * Method which
         * 
         * @method initSortables
         * @param {Object} config
         */
        initSortables: function( config ) {
            if ( _debug ) {
                console.log( 'cmsJS.createPage.initSortables: config - ', config );
            }
            
            config.sortablesConfig.visibleItemList = $( config.sortablesConfig.visibleItemList );
            config.sortablesConfig.availableItemList = $( config.sortablesConfig.availableItemList );
            if ( config.sortablesConfig.availableItemList.length > 0 ) {
                cmsJS.sortableList.init( 0, false, config );
                config.sortablesConfig.editButton.on( 'click', function() {
                    if ( $( this ).hasClass( 'fa-pencil-square-o' ) ) {
                        $( this ).removeClass( 'fa-pencil-square-o' ).addClass( 'fa-times' );
                    }
                    else {
                        $( this ).removeClass( 'fa-times' ).addClass( 'fa-pencil-square-o' );
                    }
                    $( this ).parent( '.sidebar-editor-widget-item-header' ).next( '.sidebar-editor-widget-item-body' ).slideToggle();
                } );
                
                config.sortablesConfig.availableItemList.on( 'sortbeforestop', function( event, ui ) {
                    if ( $( ui.item ).parent().attr( 'id' ) === 'visibleItemList' ) {
                        cmsJS.createPage.disablePreview();
                    }
                } );
                
                config.sortablesConfig.visibleItemList.on( 'sortbeforestop', function() {
                    cmsJS.createPage.disablePreview();
                } );
            }
            else {
                if ( _debug )
                    console.log( "No sortable list elements available" );
                return false;
            }
        },

        /**
         * Method which disables the preview button by setting a local storage value.
         * 
         * @method disablePreview
         */
        disablePreview: function() {
            if ( _debug ) {
                console.log( '---------- cmsJS.createPage.disablePreview() ----------' );
            }
            
            sessionStorage.setItem( 'previewStatus', 'false' );
        },
        
        /**
         * Method which enables the preview button by setting a local storage value.
         * 
         * @method enablePreview
         */
        enablePreview: function() {
            if ( _debug ) {
                console.log( '---------- cmsJS.createPage.enablePreview() ----------' );
            }
            
            sessionStorage.setItem( 'previewStatus', 'true' );
        },
    };


    return cms;
    
} )( cmsJS || {}, jQuery );
