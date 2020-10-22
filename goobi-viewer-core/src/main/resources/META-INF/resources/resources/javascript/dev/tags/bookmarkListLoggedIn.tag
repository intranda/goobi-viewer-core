<bookmarklistLoggedIn>

<ul if="{opts.bookmarks.config.userLoggedIn}" class="{mainClass}-small-list list">
	<li class="{mainClass}-entry" each="{bookmarkList in getBookmarkLists()}">
		<div class="login-navigation__bookmarks-name">
			<a href="{opts.bookmarks.getBookmarkListUrl(bookmarkList.id)}">{bookmarkList.name}</a>
		</div>	
		
		<div class="login-navigation__bookmarks-icon-list icon-list">
			<a href="{searchListUrl(bookmarkList)}"
				data-toggle="tooltip" data-placement="top" data-original-title=""
				title="{msg('action__search_in_bookmarks')}"> 
				<i class="fa fa-search" aria-hidden="true"></i>
			</a>
			<a href="{miradorUrl(bookmarkList)}" target="_blank" title="{msg('viewMiradorComparison')}"> 
				<i class="fa fa-th" aria-hidden="true"></i>
			</a>
			<span title="{msg('admin__crowdsourcing_campaign_statistics_numRecords')}" class="{mainClass}-counter">{bookmarkList.numItems}</span>
		</div>
	</li>
	<li class="{mainClass}-entry">
		<a class="login-navigation__bookmarks-overview-link" href="{navigationHelper.applicationUrl}user/bookmarks/"
			data-toggle="tooltip" data-placement="top" data-original-title=""
			title="{msg('action__search_in_bookmarks')}">{msg('bookmarkList_myBookmarkLists')}
		</a>
	</li>
</ul>

<script> 

this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.loader; 
this.button = this.opts.button;
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookmark-popup__body-list";


this.on( 'mount', function() {    	
    this.updateLists();
    this.opts.bookmarks.listsUpdated.subscribe( () => this.onListUpdate());
});

getBookmarkLists() {
    let lists =  this.opts.bookmarks.getBookmarkLists().slice(0,3);
    return lists;
}

updateLists() {
    this.opts.bookmarks.listsNeedUpdate.next();
}

onListUpdate() {
	this.update();
    this.hideLoader();
}

hideLoader() {
    $(this.loader).hide();
}

showLoader() {
    $(this.loader).show();
}

add(event) {
    let list = event.item.bookmarkList
    this.opts.bookmarks.addToBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
    .then( () => this.updateLists());
}

remove(event) {
    if(this.opts.bookmarks.config.userLoggedIn) {        
	    let list = event.item.bookmarkList
	    this.opts.bookmarks.removeFromBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
	    .then( () => this.updateLists())
    } else {
        let bookmark = event.item.bookmark;
        this.opts.bookmarks.removeFromBookmarkList(undefined, bookmark.pi, undefined, undefined, false)
	    .then( () => this.updateLists())
    }
}

inList(list, pi, page, logid) {
    return this.opts.bookmarks.inList(list, pi, page, logid);
}
 
mayEmptyList(list) { 
    return list.items.length > 0;
}

deleteList(event) {
    let list = event.item.bookmarkList
    this.opts.bookmarks.removeBookmarkList(list.id)
    .then( () => this.updateLists());
}

maySendList(list) {
    return !opts.bookmarks.config.userLoggedIn && list.items.length > 0; 
}

sendListUrl(list) {
	return this.opts.bookmarks.config.root + "/bookmarks/send/";
}

maySearchList(list) {
    return list.items.length > 0;
}

searchListUrl(list) {
    let url;
    if(this.opts.bookmarks.config.userLoggedIn) {
	    url = this.opts.bookmarks.config.root + "/user/bookmarks/search/" + list.name + "/";
    } else {
	    url = this.opts.bookmarks.config.root + "/bookmarks/search/" + list.name + "/";
    }
    return url; 
}

mayCompareList(list) {
    return list.items.length > 0;
}

miradorUrl(list) {
    if(list.id != null) {
    	return this.opts.bookmarks.config.root + "/mirador/id/" + list.id + "/";
    } else {        
    	return this.opts.bookmarks.config.root + "/mirador/";
    }
}

msg(key) {
    return this.opts.bookmarks.translator.translate(key);
}

</script> 

</bookmarkListLoggedIn>

