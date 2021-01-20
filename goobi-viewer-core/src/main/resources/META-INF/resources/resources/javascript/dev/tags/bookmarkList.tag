<bookmarkList>


<ul class="{mainClass} list">

	<li each="{bookmarkList in getBookmarkLists()}">

		<button if="{pi}" class="btn btn--clean" type="button"
			onclick="{inList(bookmarkList, this.pi, this.page, this.logid) ? remove : add}">
			<i if="{inList(bookmarkList, this.pi, this.page, this.logid)}"
				class="fa fa-check" aria-hidden="true"></i> {bookmarkList.name} <span>{bookmarkList.numItems}</span>
		</button>

		<div if="{!pi}" class="row no-margin">
			<div class="col-9 no-padding">
				<a
					href="{opts.bookmarks.getBookmarkListUrl(bookmarkList.id)}">{bookmarkList.name}</a>
			</div>
			<div class="col-2 no-padding icon-list">
				<a if="{maySendList(bookmarkList)}" href="{sendListUrl(bookmarkList)}" title="{msg('bookmarkList_session_mail_sendList')}"> 
					<i class="fa fa-paper-plane-o" aria-hidden="true"></i>
				</a>
				<a href="{searchListUrl(bookmarkList)}"
					data-toggle="tooltip" data-placement="top" data-original-title=""
					title="{msg('action__search_in_bookmarks')}"> 
					<i class="fa fa-search" aria-hidden="true"></i>
				</a>
				<a href="{miradorUrl(bookmarkList)}" target="_blank" title="{msg('viewMiradorComparison')}"> 
					<i class="fa fa-th" aria-hidden="true"></i>
				</a>
			</div>
			<div class="col-1 no-padding">
				<span class="{mainClass}-counter">{bookmarkList.numItems}</span>
			</div>
		</div>

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
    this.opts.bookmarks.listsUpdated.pipe(rxjs.operators.merge(rxjs.of(""))).subscribe( () => this.onListUpdate());
});

getBookmarkLists() {
    let lists =  this.opts.bookmarks.getBookmarkLists();
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
        this.opts.bookmarks.removeFromBookmarkList(0, bookmark.pi, undefined, undefined, false)
	    .then( () => this.updateLists())
    }
}

inList(list, pi, page, logid) {
    return this.opts.bookmarks.inList(list, pi, page, logid);
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

</bookmarkList>

