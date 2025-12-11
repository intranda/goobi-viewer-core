<bookmarkListSession>

<ul each="{bookmarkList in getBookmarkLists()}" class="{mainClass} list">

	
	<li each="{bookmark in bookmarkList.items}">
		<div class="row no-margin {mainClass}-single-entry">
			<div class="col-11 no-padding {mainClass}-title">
				<a href="{opts.bookmarks.config.root}{bookmark.url}">
					<div class="row no-gutters">
						<div class="col-4 no-padding">
							<div class="{mainClass}-image"
								style="background-image: url({bookmark.representativeImageUrl});">
							</div>
						</div>
						<div class="col-7 no-padding">
							<h3>{bookmark.name}</h3>
						</div>
					</div>
				</a>
			</div>
			<div class="col-1 no-padding {mainClass}-remove">
				<button class="btn btn--clean" type="button"
					data-bookmark-list-type="delete" onclick="{remove}"
					aria-label="{msg('bookmarkList_removeFromBookmarkList')}">
					<svg class="admin-cms-media__upload-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                    	<use riot-href="{getIconHref('ban')}"></use>
                	</svg>
				</button>
			</div>
		</div>
	</li>
</ul>

<div each="{bookmarkList in getBookmarkLists()}" class="{mainClass}-actions">

	<div if="{mayEmptyList(bookmarkList)}" class="{mainClass}-reset">
		<button class="btn btn--clean" type="button"
			data-bookmark-list-type="reset" onclick="{deleteList}">
			<span>{msg('bookmarkList_reset')}</span>
				<svg class="admin-cms-media__upload-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                    <use riot-href="{getIconHref('trash')}"></use>
                </svg>
		</button>
	</div>


	<div if="{maySendList(bookmarkList)}" class="{mainClass}-send">
		<a href="{sendListUrl(bookmarkList)}"> 
			<span>{msg('bookmarkList_session_mail_sendList')}</span> 
				<svg class="admin-cms-media__upload-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                    <use riot-href="{getIconHref('send')}"></use>
                </svg>
		</a>
	</div>


	<div if="{maySearchList(bookmarkList)}" class="{mainClass}-search">
		<a href="{searchListUrl(bookmarkList)}"
			data-toggle="tooltip" data-placement="top" data-original-title=""
			title=""> 
			<span>{msg('action__search_in_bookmarks')}</span> 
				<svg class="admin-cms-media__upload-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                    <use riot-href="{getIconHref('search')}"></use>
                </svg>
		</a>
	</div>

	<div if="{mayCompareList(bookmarkList)}" class="{mainClass}-mirador">
		<a href="{miradorUrl(bookmarkList)}" target="_blank"> 
			<span>{msg('viewMiradorComparison')}</span> 
				<svg class="admin-cms-media__upload-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                    <use riot-href="{getIconHref('grid-dots')}"></use>
                </svg>
		</a>
	</div>
</div>

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

 
mayEmptyList(list) { 
    return list.items.length > 0;
}

remove(event) {
        let bookmark = event.item.bookmark;
        this.opts.bookmarks.removeFromBookmarkList(0, bookmark.pi, undefined, undefined, false)
	    .then( () => this.updateLists())
}

deleteList(event) {
    this.opts.bookmarks.removeBookmarkList(0)
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
    	return this.opts.bookmarks.config.root + "/mirador/id/0/";
}

msg(key) {
    return this.opts.bookmarks.translator.translate(key);
}

</script> 

</bookmarkListSession>
