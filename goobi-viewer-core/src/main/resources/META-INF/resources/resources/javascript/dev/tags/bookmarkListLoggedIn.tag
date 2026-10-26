<bookmarklistLoggedIn>

<ul if="{opts.bookmarks.config.userLoggedIn}" id="userBookmarkListsMenu" class="{mainClass}-small-list list">
	<li class="{mainClass}-entry" each="{bookmarkList in getBookmarkLists()}">
		<a class="{mainClass}-link" href="{opts.bookmarks.getBookmarkListUrl(bookmarkList.id)}">
			<span class="{mainClass}-name">{bookmarkList.name}</span>
		</a>
		<a class="{mainClass}-action" href="{searchListUrl(bookmarkList)}"
			title="{msg('action__search_in_bookmarks')}"
			aria-label="{msg('action__search_in_bookmarks')}: {bookmarkList.name}">
			<svg class="icon-svg {mainClass}-action-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
				<use riot-href="{getIconHref('search')}"></use>
			</svg>
		</a>
		<a class="{mainClass}-action" href="{miradorUrl(bookmarkList)}" target="_blank"
			title="{msg('viewMiradorComparison')}"
			aria-label="{msg('viewMiradorComparison')}: {bookmarkList.name}">
			<svg class="icon-svg {mainClass}-action-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
				<use riot-href="{getIconHref('grid-dots')}"></use>
			</svg>
		</a>
		<span class="{mainClass}-counter" title="{msg('admin__crowdsourcing_campaign_statistics_numRecords')}">
			<span class="sr-only">{msg('admin__crowdsourcing_campaign_statistics_numRecords')}: </span>{bookmarkList.numItems}
		</span>
	</li>
	<li class="{mainClass}-entry">
		<a class="{mainClass}-overview-link" href="{allBookmarksUrl()}">
			<span class="{mainClass}-name">{msg('bookmarkList_overview_all')} ({opts.bookmarks.getBookmarkListsCount()})</span>
			<svg class="icon-svg {mainClass}-overview-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
				<use riot-href="{getIconHref('arrow-right')}"></use>
			</svg>
		</a>
	</li>
</ul>

<script> 

const ensureTrailingSlash = value => value.endsWith('/') ? value : `${value}/`;
const viewerConfig = window.viewerConfig || {};
this.iconBasePath = ensureTrailingSlash(viewerConfig.iconBasePath || viewerConfig.contextPath || '/');
this.getIconHref = iconName => `${this.iconBasePath}resources/icons/outline/${iconName}.svg#icon`;

this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.loader; 
this.button = this.opts.button;
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookmark-popup__body-list";

this.on( 'mount', function() {    	
    this.opts.bookmarks.listsUpdated.subscribe( () => this.onListUpdate());
});

getBookmarkLists() {
    let lists =  this.opts.bookmarks.getBookmarkLists().slice(0,5);
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
	    let list = event.item.bookmarkList
	    this.opts.bookmarks.removeFromBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
	    .then( () => this.updateLists())
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

allBookmarksUrl() {
    	return this.opts.bookmarks.config.root + "/user/bookmarks/";
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
