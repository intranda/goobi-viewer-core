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
 * Module to manage bookmarks if the user is logged in.
 * 
 * @version 4.3.0
 * @module viewerJS.bookmarks
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) { 
    'use strict';
    
    var _debug = false;
    var _riotTags = [];
    var _defaults = {
        root: '',
        counter: ".bookmark-navigation__counter",
        typePage: false,
        userLoggedIn: false,
        msg: {
            deleteBookmarkList: "",
            resetBookmarkLists: "",
            resetBookmarkListsConfirm: "",
            noItemsAvailable: "",
            selectBookmarkList: "",
            addNewBookmarkList: "",
            sendBookmarkList: "",
            searchInBookmarkList: "",
            openInMirador: "",
            type_label: "",
            typeRecord: "",
            typePage: ""
        }
    };
    var _bookmarks = {
            
            listsNeedUpdate: new Rx.Subject(),
            listsUpdated: new Rx.Subject(),
            bookmarkLists: [],
            init: function( config ) {
                if ( _debug ) {
                    console.log( '##############################' );
                    console.log( 'viewer.bookmarks.init' );
                    console.log( '##############################' );
                    console.log( 'viewer.bookmarks.init: config - ', config );
                }
                
                this.config = $.extend( true, {}, _defaults, config );
                this.typePage = this.config.typePage;
                this.listsNeedUpdate.subscribe( () => this.updateLists());
                this.listsUpdated.subscribe( (list) => {
                    this.updateAddedStatus();
                })
                this.renderBookmarksNavigationList();
                this.renderCounter();
                this.prepareBookmarksPopup();

            },
            
            updateAddedStatus: function() {
                $('[data-bookmark-list-type="add"]').each( (index, button) => { 
                    let $button = $(button);
                    var pi = $button.attr( 'data-pi' );
                    var logid = $button.attr( 'data-logid' );
                    var page = $button.attr( 'data-page' );
                    
                    let added = this.contained(pi, page, logid);
                    if(added) {
                        $button.addClass("added");
                    } else {
                        $button.removeClass("added");
                    }
                    
                } );
            },
            
            
            
            renderCounter: function() {
                var $counter = $(this.config.counter);
                if($counter.length > 0) {
                    $counter.addClass("in");
                    this.listsUpdated.subscribe((list) => {
                        let count = 0;
                        if(this.bookmarkLists) {
                            count = this.getBookmarksCount();
                        }
                        this.updateCounter(count);
                    })
                }
            },
            getBookmarksCount: function() {
                if(this.bookmarkLists) {                    
                    return this.bookmarkLists.flatMap(list => list.items).length;
                } else {
                    return 0;
                }
            },
            getBookmarkListsCount: function() {
                if(this.bookmarkLists) {                    
                    return this.bookmarkLists.length;
                } else {
                    return 0;
                }
            },
            updateCounter: function(count) {
                $(this.config.counter).html(count);
            },
            renderBookmarksNavigationList: function() {
                var $button = $('[data-bookmark-list-type="dropdown"]');
                if($button.length == 0) {
                    return;
                }
                var dropdownWidth = 275;
                var buttonPosition = $button.offset();
                var buttonSize = {
                        width: $button.width(),
                        height: $button.height()
                }
                var dropdownPosition = {
                    left: buttonPosition.left + buttonSize.width - dropdownWidth,
                    top: buttonPosition.top + buttonSize.height
                }
  
                var $dropdown = $("<bookmarkList></bookmarkList>");
                $dropdown.addClass("bookmark-navigation__dropdown");
                
                $button.after($dropdown);
                riot.mount('bookmarkList', {
                    data: {        
                        pi: '',
                        logid: '',
                        page: ''
                    },
                    style: {
                        mainClass : "bookmark-navigation__dropdown-list"
                    },
                    button: '[data-bookmark-list-type="dropdown"]',
                    msg: this.config.msg,
                    bookmarks: this,
                });
                
                //handle closing dropdown
                let toggle = function() {
                    $dropdown.slideToggle( 'fast' );
                    $("body").off("click", toggle);
                }
                $button.on("click", (event) => {
                    if( (this.config.userLoggedIn && this.getBookmarkListsCount() > 0) || this.getBookmarksCount() > 0) {                        
                        $dropdown.slideToggle( 'fast');
                    }
                    event.stopPropagation();
                })
                $("body").on("click", (event) => {
                    if($dropdown.is(":visible")) {                        
                        $dropdown.slideToggle( 'fast');
                    }
                })
                $dropdown.on("click", (event) => event.stopPropagation());
                
            },
            prepareBookmarksPopup: function() {
                
                // render bookmarks popup
                $( '[data-bookmark-list-type="add"]' ).off().on( 'click', function( event ) {
                                        
                    var currBtn = $( event.target ).closest('[data-bookmark-list-type="add"]');
                    var currPi = currBtn.attr( 'data-pi' );
                    var currLogid = currBtn.attr( 'data-logid' );
                    var currPage = currBtn.attr( 'data-page' );
                    var currType = currBtn.attr( 'data-type' );
                    
                    if(this.config.userLoggedIn) {                        
                        // render bookmarks popup with timeout to finish handling click event first.
                        setTimeout(() => this.renderBookmarksPopup( currPi, currLogid, currPage, currBtn ), 0);
                    } else if(this.contained(currPi, currPage, currLogid)){
                        this.removeFromBookmarkList(undefined, currPi, undefined, undefined, false )
                        .then( () => this.listsNeedUpdate.onNext());
                    } else {
                        this.addToBookmarkList(undefined, currPi, undefined, undefined, false )
                        .then( () => this.listsNeedUpdate.onNext());
                    }
                }.bind(this) );  
            },
            renderBookmarksPopup: function(pi, logid, page, button) {
                var $popup = $("<bookmarksPopup></bookmarksPopup>");
                $('body').append($popup);
                riot.mount('bookmarksPopup', {
                    data: {        
                        pi: pi,
                        logid: logid,
                        page: page
                    },
                    button: button,
                    msg: this.config.msg,
                    bookmarks: this,
                });
            },

            action: function(verb, id) {
                let url = this.config.root + (this.config.userLoggedIn ? "/rest/bookmarks/user" : "/rest/bookmarks/session");
                if(id !== undefined) {
                    url += "/get/" + id;
                };
                url += "/" + verb + "/";
                url = url.replace(/\/+/g, "/");

                return url;
            },
            
            getBookmarkLists: function() {
                return this.bookmarkLists;
            },

            loadBookmarkLists: function() {
                
                let url = this.action("get");
                
                return fetch(url, {cache:"no-cache"})
                .then( data => data.json())
                .then(json => {
                    if(!Array.isArray(json)) {
                        return [json];
                    } else {
                        return json
                    }
                })
                
            },
            
            updateLists: function() {
                this.loadBookmarkLists()
                .then(lists => this.bookmarkLists = lists)
                .then(() => this.listsUpdated.onNext(this.bookmarkLists));
            },


            addToBookmarkList: function(listId, pi, page, logid, bookmarkPage) {
                
                let url = this.action("add", listId) + pi + "/"; 
                if(bookmarkPage) { 
                    url += (page ? page : "-") + "/" + (logid ? logid : "-") + "/";
                }
                
             
                return fetch(url, {method:"POST"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to add bookmark: " + data.message);
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },

            removeFromBookmarkList: function(listId, pi, page, logid, bookmarkPage) {
                
                let url = this.action("delete", listId) + pi + "/";
                if(bookmarkPage) {
                    url += (page ? page : "-") + "/" + (logid ? logid : "-") + "/";
                }
                
             
                return fetch(url, {method:"DELETE"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to remove bookmark: " + data.message);
                    } else {
                        
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },
            
            addBookmarkList: function(name) {
                
                let url = this.action("add")
                if(name) {
                    url +=  name + "/"
                }
                
             
                return fetch(url, {method:"POST"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to create bookmarklist: " + data.message);
                    } else {
                        
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },
            
            removeBookmarkList: function(id) {
                
                let url = this.action("delete")
                if(id) {                    
                    url += id + "/";
                }
                
             
                return fetch(url, {method:"DELETE"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to delete bookmarklist: " + data.message);
                    } else {
                        
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },
            
            getList: function(id) {
                this.bookmarkLists.find(list => list.id == id);
            },

            getItem: function(list, pi, page, logid) {
                for(item of list.items) {
                    if(item.pi == pi && (page == undefined  || page == item.order) && (logid == undefined || logid == item.logId)) {
                        return item;
                    }
                }
                return undefined;
            },

            contained: function(pi, page, logid) {
                for(var list of this.bookmarkLists) {
                    if(this.inList(list, pi, page, logid)) {
                        return true;
                    }
                }
                return false;
            },
                    
            inList: function(list, pi, page, logid) {
                    for(var item of list.items) {
                        if(this.isTypeRecord() && item.pi == pi && item.order === null && item.logId === null) {
                            return true;
                        } else if(this.isTypePage() && item.pi == pi && page == item.order ) {
                            return true;
                        }
                    }
                return false;
            },
            
            getBookmarkListUrl(listId) {
                return this.config.root + "/user/bookmarks/show/" + listId;
            },

            isTypePage: function() {
                return this.typePage;
            },
            isTypeRecord: function() {
                return !this.typePage;
            },
            setTypePage: function() {
                this.typePage = true;
            },
            setTypeRecord: function() {
                this.typePage = false;
            }
            
    }
    viewer.bookmarks = _bookmarks;
    return viewer;
    
} )( viewerJS || {}, jQuery );

