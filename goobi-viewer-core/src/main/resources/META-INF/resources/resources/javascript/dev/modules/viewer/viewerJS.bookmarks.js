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
    var _messageKeys = ['bookmarkList_reset', 'bookmarkList_delete', 'bookmarkList_session_mail_sendList', 
        'action__search_in_bookmarks', 'bookmarkList_resetConfirm', 'bookmarkList_noItemsAvailable', 
        'bookmarkList_selectBookmarkList', 'bookmarkList_addNewBookmarkList', 'viewMiradorComparison',
        'bookmarkList_type_label', 'bookmarkList_typeRecord', 'bookmarkList_typePage', 'bookmarkList_overview_all',
        'admin__crowdsourcing_campaign_statistics_numRecords', 'bookmarkList_removeFromBookmarkList', 'bookmarks'];

    var _defaults = {
        root: '',
        rest: '',
        counter: ".bookmark-navigation__counter",
        typePage: false,
        userLoggedIn: false,
        language: 'en'
    };
    var _bookmarks = {
             
            listsNeedUpdate: new rxjs.Subject(),
            listsUpdated: new rxjs.Subject(),
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
                this.translator = new viewerJS.translator(this.config.rest, this.config.language);
                this.translator.init(_messageKeys)
                .then(() => this.updateLists())
                .then(() => {                    
                    this.prepareBookmarksPopup();
                    this.renderBookmarksNavigationList();
                })
                .catch(e => {
					console.error("Error loading bookmark lists: ", e);
				})
            },
            
            updateAddedStatus: function() {
                $('[data-bookmark-list-type="add"]').each( (index, button) => { 
                    let $button = $(button); 
                    var pi = $button.attr( 'data-pi' );
                    var logid = $button.attr( 'data-logid' );
                    var page = $button.attr( 'data-page' );
                    
                    let added = this.contained(pi, page, logid);
                    if(_debug)console.log("set added to " + added + " for ", pi, page);
                    let $span = $button.find("span");
                    if(added) {
                        $button.addClass("added");
                        $span.tooltip('hide').attr("title", $span.attr("data-bookmark-list-title-added")).tooltip("_fixTitle");
                        $button.attr('aria-checked', true);
                    } else {
                        $button.removeClass("added");
                        $span.tooltip('hide').attr("title", $span.attr("data-bookmark-list-title-add")).tooltip("_fixTitle");
                        $button.attr('aria-checked', false);
                    }
                    
                } );
            },
            
            
            
            renderCounter: function() {
                var $counter = $(this.config.counter);
                if($counter.length > 0) {
                    this.listsUpdated.pipe(rxjs.operators.merge(rxjs.of(""))).subscribe((list) => {
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
                
                if(!this.config.userLoggedIn) {                    
                    
                    if(this.getBookmarksCount() > 0) {                        
                        this.renderSessionBookmarksDropdown();
                        this.renderCounter();
                    }
                    this.listsUpdated.subscribe(() => {
                        let sessionListMounted = $("bookmarklistsession").length > 0;
                        if(!sessionListMounted && this.getBookmarksCount() > 0) {
                            this.renderSessionBookmarksDropdown();
                            this.renderCounter();
                        } else if(sessionListMounted && this.getBookmarksCount() == 0){
                            this.hideSessionBookmarksDropdown();
                        }
                    });
                   
                } else {
                    this.renderUserBookmarksDropdown();                    
                }
                
            },
            renderUserBookmarksDropdown: function() {
                
                var $button = $('[data-bookmark-list-type="dropdown"]');
                if($button.length == 0) {
                    return;
                }
                
                var $bookmarkPosition = $('.login-navigation__bookmarks-list');
                var $dropdownUserLogin = $("<bookmarkListLoggedIn></bookmarkListLoggedIn>");
                $dropdownUserLogin.addClass("login-navigation__bookmarks-dropdown");
                
                $bookmarkPosition.after($dropdownUserLogin);
                riot.mount('bookmarkListLoggedIn', {
                    data: {        
                        pi: '',
                        logid: '',
                        page: ''
                    },
                    style: {
                        mainClass : "login-navigation__bookmarks"
                    },
                    bookmarkPosition: '.login-navigation__bookmarks-list',
                    bookmarks: this,
                });

                // handle closing dropdown
                let toggle = function() {
                    $dropdown.slideToggle( 'fast' );
                    $("body").off("click", toggle);
                }
                // bookmark list dropdown toggle 
                $button.on("click", (event) => {
                    if( (this.config.userLoggedIn && this.getBookmarkListsCount() > 0) || this.getBookmarksCount() > 0) {                        
                        $(event.currentTarget).next('.bookmark-navigation__dropdown').fadeToggle('fast');
                    }
                })
                $("body").on("click", (event) => {
                    let $target = $(event.target);
                    if( $target.closest('[data-bookmark-list-type="dropdown"]').length > 0 ) {
                        return; // click on bookmarks button. Don't close
                    }
                    if( $target.closest('bookmarkList').length > 0 ) {
                        return; // click on bookmark list. Don't close
                    }
                    if ($('.bookmark-navigation__dropdown').is(":visible")) {                        
                        $('.bookmark-navigation__dropdown').fadeOut( 'fast');
                    }
                })
                
                // Trigger bookmarks dropdown in login navigation dropdown
                $('body').on('click', '.login-navigation__bookmarks-trigger', function(){
                    event.preventDefault();
                    $('.login-navigation__bookmarks-small-list').slideToggle('fast');
                    $('.login-navigation__bookmarks-list').toggleClass('-opened');
                });
                   
//                $dropdown.on("click", (event) => event.stopPropagation());
            },
            calcDropDownPos: function(dropdown, button) {
              var dropDownRect = dropdown.getBoundingClientRect();

              if (dropDownRect.width === 0) {
                dropdown.style.transform = '';
                // change display prop to get BoundingClientRect values 
                dropdown.style.display = 'block';
                dropDownRect = dropdown.getBoundingClientRect();
                // change it back, so it will not appear visually
                dropdown.style.display = 'none';

                // if negativ value: dropdown overflows left
                if(dropDownRect.x < 0) {
                  return Math.floor(-dropDownRect.x) +'px';
                }
                // if positiv value: dropdown overflows right
                var overflowRight = (dropDownRect.x + dropDownRect.width) - document.documentElement.clientWidth;
                if(overflowRight > 0) {
                  return Math.ceil(-overflowRight) +'px';
                }
                return undefined;
              }
            },
            renderSessionBookmarksDropdown: function() {
                var $button = $('[data-bookmark-list-type="dropdown"]');
                if($button.length == 0) {
                    return;
                }
                $button.css("display", "flex");
                var buttonPosition = $button.offset();
                var buttonSize = {
                        width: $button.width(),
                        height: $button.height()
                }

                var $dropdown = $("<bookmarklistsession></bookmarklistsession>");
                $dropdown.addClass("bookmark-navigation__dropdown");
                $button.on("click", (event) => {

                    // make sure the dropdown does not overflow the viewport
                    var transformVal = this.calcDropDownPos($dropdown[0]);

                    // show/hide dropdown + and set it's width
                    $(event.currentTarget).next('.bookmark-navigation__dropdown')
                      .fadeToggle('fast')
                      .css("transform", "translate(" + transformVal + ")");
                })
                $("body").on("click", (event) => {
                    let $target = $(event.target);
                    if( $target.closest('[data-bookmark-list-type="dropdown"]').length > 0 ) {
                        return;
                    }
                    if( $target.closest('bookmarklistsession').length > 0 ) {
                        return;
                    }
                    if ($('.bookmark-navigation__dropdown').is(":visible")) {
                        $('.bookmark-navigation__dropdown').fadeOut( 'fast');
                    }
                })
                $button.after($dropdown);
                this.sessionBookmarkDropdown = riot.mount('bookmarklistsession', {
                    data: {
                        pi: '',
                        logid: '',
                        page: ''
                    },
                    style: {
                        mainClass : "bookmark-navigation__dropdown-list"
                    },
                    button: '[data-bookmark-list-type="dropdown"]',
                    bookmarks: this,
                });
            },
            hideSessionBookmarksDropdown: function() {
                var $button = $('[data-bookmark-list-type="dropdown"]');
                if($button.length == 0) {
                    return;
                }
                $button.off("click");
                $("body").off("click");
                $button.css("display", "none");
                if(this.sessionBookmarkDropdown) {
                    this.sessionBookmarkDropdown.forEach(component => {
                        component.unmount();
                    })
                }
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
                        this.removeFromBookmarkList(0, currPi, undefined, undefined, false )
                        .then( () => this.listsNeedUpdate.next());
                    } else {
                        this.addToBookmarkList(0, currPi, undefined, undefined, false )
                        .then( () => this.listsNeedUpdate.next());
                    }
                }.bind(this) );  
            },
            renderBookmarksPopup: function(pi, logid, page, button) {
                var $popup = $("<bookmarksPopup></bookmarksPopup>");
                $(button).append($popup);
                riot.mount('bookmarksPopup', {
                    data: {        
                        pi: pi,
                        logid: logid,
                        page: page
                    },
                    button: button,
                    bookmarks: this,
                });
            },

            getUrl: function(id) {
                let url = this.config.rest + "bookmarks/";
                if(id !== undefined) {
                    url += (id + "/");
                };
                return url;
            },
            
            getBookmarkLists: function() {
                return this.bookmarkLists;
            },

            loadBookmarkLists: function() {
                
                let url = this.getUrl();
                
                return fetch(url, {
                    cache: "no-cache",
                    method: "GET"
                 })
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
                return this.loadBookmarkLists()
                .then(lists => this.bookmarkLists = lists)
                .then(() => this.listsUpdated.next(this.bookmarkLists));
            },


            addToBookmarkList: function(listId, pi, page, logid) {
                
                let url = this.getUrl(listId) 
                
                let item = {
                    pi: pi,
                    logId: logid,
                    order: page
                }
                
                return fetch(url, {
                    method:"POST",
                    body: JSON.stringify(item),
                    headers: {'Content-Type': 'application/json'}
                })
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

            removeFromBookmarkList: function(listId, pi, page, logid) {
                
                if(_debug) {
                    console.log("removeFromBookmarkList, listId", listId);
                    console.log("removeFromBookmarkList, pi", pi);
                    console.log("removeFromBookmarkList, page", page);
                    console.log("removeFromBookmarkList, logid", logid);
                }

                let list = this.getList(listId);
                let item = this.getItem(list, pi, page, logid);
                // console.log("got from list ", list, item);
                if(item) {
                    let url = this.getUrl(listId) + "items/" + item.id + "/";
                    // console.log("fetch ", url);
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
                    
                } else {
                    return Promise.reject("Item not found");
                }
            },
            
            addBookmarkList: function(name) {
                
                let url = this.getUrl();
                let listToAdd = {
                        name: name
                }
                
             
                return fetch(url, {
                    method:"POST",
                    body: JSON.stringify(listToAdd),
                    headers: {'Content-Type': 'application/json'}
                 })
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
                
                let url = this.getUrl(id)
                
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
                if(this.config.userLoggedIn) {
                    return this.bookmarkLists.find(list => list.id == id);                    
                } else {
                    return this.bookmarkLists[0];
                }
            },

            getItem: function(list, pi, page, logid) {
                if(list) {    
                    return list.items.find(item => {
                        return item.pi == pi && (page == undefined  || page == item.order) && (logid == undefined || logid == item.logId);
                    })
                    
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
                        if(this.isTypeRecord() && item.pi == pi && item.order == null && item.logId == null) {
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

