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
 * Module which initializes the tags for the CMS media files.
 * 
 * @version 3.2.0
 * @module cmsJS.tagList
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    var _debug = false;
    var _defaults = {
        inputFieldId: 'tagInput',
        tagListId: 'tagList',
        autoSuggestUrl: '',
        msg: {
            addTagLabel: 'Tag hinzufügen'
        }
    };
    
    cms.tagList = {
        tags: [],
        $tagListElement: null,
        autoSuggestUrl: null,
        /**
         * Method which initializes the tag list.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.inputFieldId The Selector for the tag input field.
         * @param {String} config.tagList The Selector for the tag list.
         * @param {String} config.autoSuggestUrl The URL for the tag auto suggest.
         * @param {Object} config.msg An object with message keys.
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'cmsJS.tagList.init' );
                console.log( '##############################' );
                console.log( 'cmsJS.tagList.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            this.$inputField = $( '#' + _defaults.inputFieldId );
            
            if ( this.$inputField.length === 0 ) {
                console.log( 'Data input field not found' );
            }
            
            this.$tagListElement = $( '#' + _defaults.tagListId );
            
            if ( this.$tagListElement.length === 0 ) {
                throw 'Tag list element not found';
            }
            
            this.autoSuggestUrl = _defaults.autoSuggestUrl;
            this.tags = _readTags( this.$inputField );
            _renderList( this.tags, this.$tagListElement );
            
        },
        /**
         * Method which adds a tag to the list.
         * 
         * @method addTag
         * @param {String} tagName The current tag name.
         */
        addTag: function( tagName ) {
            if ( _debug ) {
                console.log( '---------- cms.tagList.addTag() ----------' );
                console.log( 'cmsJS.tagList.addTag: tagName = ', tagName );
            }
            
            var containedTag = this.getTag( tagName );
            if ( containedTag ) {
                var duration = 200;
                $( containedTag ).fadeOut( duration / 2, function() {
                    $( containedTag ).fadeIn( duration / 2 );
                } );
            }
            else {
                if ( this.$tagListElement ) {
                    _createListElement( tagName, this.$tagListElement );
                }
                this.saveTags();
            }
        },
        /**
         * Method which deletes a tag from the list.
         * 
         * @method deleteTag
         * @param {Object} tag The current tag to delete.
         */
        deleteTag: function( tag ) {
            if ( _debug ) {
                console.log( '---------- cms.tagList.deleteTag() ----------' );
                console.log( 'cmsJS.tagList.deleteTag: tag = ', tag );
            }
            
            tag.remove();
            this.saveTags();
        },
        /**
         * Method to save the tag list.
         * 
         * @method saveTags
         */
        saveTags: function() {
            if ( _debug ) {
                console.log( '---------- cms.tagList.saveTags() ----------' );
            }
            
            _writeTags( cms.tagList.$tagListElement.children( '[id$=_item]' ), cms.tagList.$inputField );
        },
        /**
         * Method to ...
         * 
         * @method contains
         */
        contains: function( tagName ) {
            if ( _debug ) {
                console.log( '---------- cms.tagList.contains() ----------' );
                console.log( 'cmsJS.tagList.contains: tagName = ', tagName );
            }
            
            var found = false;
            var $tags = cms.tagList.$tagListElement.children( '[id$=_item]' );
            
            $tags.each( function() {
                var text = $( this ).text();
                if ( tagName.trim().toUpperCase() === text.trim().toUpperCase() ) {
                    found = true;
                    
                    return false;
                }
            } );
            
            return found;
        },
        /**
         * Method to ...
         * 
         * @method close
         */
        close: function() {
            if ( _debug ) {
                console.log( '---------- cms.tagList.close() ----------' );
            }
            
            this.$inputField.off();
            this.$tagListElement.off();
            this.$tagListElement.find( '.tag-terminator' ).off();
            this.$tagListElement.find( 'input' ).off();
        },
        /**
         * Method to get all tags.
         * 
         * @method getTags
         */
        getTags: function() {
            if ( _debug ) {
                console.log( '---------- cms.tagList.getTags() ----------' );
            }
            
            return cms.tagList.$tagListElement.children( '[id$=_item]' );
        },
        /**
         * Method to get all tag values.
         * 
         * @method getTagValues
         */
        getTagValues: function() {
            if ( _debug ) {
                console.log( '---------- cms.tagList.getTagValues() ----------' );
            }
            
            var values = cmsJS.tagList.getTags().map( function( index, tag ) {
                return $( tag ).text();
            } );
            
            return values;
        },
        /**
         * Method to get a single tag.
         * 
         * @method getTag
         */
        getTag: function( value ) {
            if ( _debug ) {
                console.log( '---------- cms.tagList.getTag() ----------' );
                console.log( 'cmsJS.tagList.getTag: value = ', value );
            }
            
            var selectedTag = undefined;
            
            cmsJS.tagList.getTags().each( function( index, tag ) {
                if ( cmsJS.tagList.getValue( tag ).trim().toUpperCase() === value.trim().toUpperCase() ) {
                    selectedTag = tag;
                    
                    return false;
                }
            } );
            
            return selectedTag;
        },
        /**
         * Method to get a single Value.
         * 
         * @method getValue
         */
        getValue: function( tag ) {
            if ( _debug ) {
                console.log( '---------- cms.tagList.getValue() ----------' );
                console.log( 'cmsJS.tagList.getValue: tag = ', tag );
            }
            
            return $( tag ).text();
        }
    };
    
    /**
     * Method to ...
     * 
     * @method _readTags
     * @param {Object} $input ...
     */
    function _readTags( $input ) {
        if ( _debug ) {
            console.log( '---------- _readTags() ----------' );
            console.log( '_readTags: $input = ', $input );
        }
        
        var tagString = $input.val();
        
        if ( !tagString ) {
            tagString = '[]';
        }
        
        if ( tagString.length > 0 ) {
            if ( _debug ) {
                console.log( '---------- _readTags() ----------' );
                console.log( 'tagString: ', tagString );
            }
            
            try {
                var tagList = JSON.parse( tagString );
                return tagList;
            }
            catch ( error ) {
                console.log( 'Error reading tags from ' + tagString );
                
                return [];
            }
        }
        else {
            return [];
        }
    }
    
    /**
     * Method to ...
     * 
     * @method _writeTags
     * @param {Object} $tags ...
     * @param {Object} $input ...
     */
    function _writeTags( $tags, $input ) {
        if ( _debug ) {
            console.log( '---------- _writeTags() ----------' );
            console.log( '_writeTags: $tags = ', $tags );
            console.log( '_writeTags: $input = ', $input );
        }
        
        var tags = [];
        
        $tags.each( function() {
            if ( $( this ).text().length > 0 ) {
                tags.push( $( this ).text() );
            }
        } )

        var tagString = JSON.stringify( tags );
        
        $input.val( tagString );
    }
    
    /**
     * Method to ...
     * 
     * @method _renderList
     * @param {Array} tags ...
     * @param {Object} $ul ...
     */
    function _renderList( tags, $ul ) {
        if ( _debug ) {
            console.log( '---------- _renderList() ----------' );
            console.log( '_renderList: tags = ', tags );
            console.log( '_renderList: $ul = ', $ul );
        }
        
        var count = 0;
        var ulId = $ul.attr( 'id' );
        for ( var index = 0; index < tags.length; index++ ) {
            var tag = tags[ index ];
            _createListElement( tag, $ul, count );
            count++;
        }
        
        _createTagInputElement( $ul.parent() );
    }
    
    /**
     * Method to ...
     * 
     * @method _createListElement
     * @param {String} value ...
     * @param {Object} $parent ...
     * @param {Number} count
     */
    function _createListElement( value, $parent, count ) {
        if ( _debug ) {
            console.log( '---------- _createListElement() ----------' );
            console.log( '_createListElement: value = ', value );
            console.log( '_createListElement: $parent = ', $parent );
            console.log( '_createListElement: count = ', count );
        }
        
        if ( !count ) {
            count = $parent.children( '[id$=_item]' ).length;
        }
        
        var ulId = $parent.attr( 'id' );
        var $tagInput = $( '.tag-input' );
        var $li = $( '<li/>' );
        $li.attr( 'id', ulId + '_' + count + '_item' );
        var $liText = $( '<span class="tag label"/>' );
        $liText.text( value );
        var $terminator = $( '<span />' );
        $terminator.addClass( 'tag-terminator' );
        $terminator.on( 'click', _handleClickTerminator );
        $li.append( $liText );
        $liText.append( $terminator );
        
        if ( $tagInput.length > 0 ) {
            $li.insertBefore( $tagInput );
        }
        else {
            $parent.append( $li );
        }
    }
    
    /**
     * Method to ...
     * 
     * @method _createTagInputElement
     * @param {Object} $parent ...
     */
    function _createTagInputElement( $parent ) {
        if ( _debug ) {
            console.log( '---------- _createTagInputElement() ----------' );
            console.log( '_createTagInputElement: $parent = ', $parent );
        }
        
        var $container = $( '.media-modal__tags' );
        var sizeCount = 1;
        var ulId = $parent.attr( 'id' );
        var $inputListElement = $( '<li />' );
        $inputListElement.addClass( 'tag-input' );
        var $input = $( '<input type="text" />' );
        $input.attr( 'id', ulId + '_inputField' );
        $input.attr( 'size', sizeCount );
        $inputListElement.append( $input );
        $parent.find( 'ul' ).append( $inputListElement );
        
        // handler
        $container.on( 'click', function() {
            $input.focus();
        } );
        $input.on( 'change', _handleInputChange );
        $input.on( 'keypress', function( event ) {
            // change size of input
            sizeCount++;
            $input.attr( 'size', sizeCount );
            
            // press enter
            if ( event.keyCode == 13 ) {
                sizeCount = 1;
                $input.attr( 'size', sizeCount );
                
                return _handleInputChange( event );
            }
        } );
        
        // autocomplete
        $input.autocomplete( {
            source: function( request, response ) {
                Q( $.ajax( {
                    url: cms.tagList.autoSuggestUrl + request.term + '/',
                    type: 'GET',
                    datatype: 'json',
                } ) ).then( function( data ) {
                    response( data );
                } )
            },
            appendTo: $input.parent(),
            select: function( event, ui ) {
                _handleInputChange( event, ui.item.value );
            }
        } );
    }
    
    /**
     * Method to ...
     * 
     * @method _handleInputChange
     * @param {Object} event ...
     * @param {String} text ...
     */
    function _handleInputChange( event, text ) {
        if ( _debug ) {
            console.log( '---------- _handleInputChange() ----------' );
            console.log( '_handleInputChange: event = ', event );
            console.log( '_handleInputChange: text = ', text );
            console.log( 'on change occured in: ', event.target );
        }
        if ( !text || text.length === 0 ) {
            text = $( event.target ).val();
        }
        if ( text.trim().length > 0 ) {
            cms.tagList.addTag( text );
            $( event.target ).val( '' );
        }
        event.preventDefault();
        event.stopPropagation();
        
        return false;
    }
    
    /**
     * Method to ...
     * 
     * @method _handleClickTerminator
     * @param {Object} event ...
     */
    function _handleClickTerminator( event ) {
        if ( _debug ) {
            console.log( '---------- _handleClickTerminator() ----------' );
            console.log( '_handleClickTerminator: event = ', event );
            console.log( 'Click on: ', event.currentTarget );
        }
        
        var $li = $( event.target ).parent().parent();
        cms.tagList.deleteTag( $li );
    }
    
    return cms;
    
} )( cmsJS || {}, jQuery );
