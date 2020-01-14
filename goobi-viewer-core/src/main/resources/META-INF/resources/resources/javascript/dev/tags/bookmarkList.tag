<bookmarkList>


<ul if="{opts.bookmarks.config.userLoggedIn}" class="{mainClass} list">

	<li each="{bookmarkList in getBookmarkLists()}">

		<button if="{pi}" class="btn btn--clean" type="button"
			onclick="{inList(bookmarkList, this.pi, this.page, this.logid) ? remove : add}">
			<i if="{inList(bookmarkList, this.pi, this.page, this.logid)}"
				class="fa fa-check" aria-hidden="true"></i> {bookmarkList.name} <span>{bookmarkList.numItems}</span>
		</button>

		<div if="{!pi}" class="row no-margin">
			<div class="col-xs-9 no-padding">
				<a
					href="{opts.bookmarks.config.root}/bookmarks/show/{bookmarkList.id}">{bookmarkList.name}</a>
			</div>
			<div class="col-xs-2 no-padding icon-list">
				<a if="{maySendList(bookmarkList)}" href="{sendListUrl(bookmarkList)}" title="{opts.msg.sendBookmarkList}"> 
					<i class="fa fa-paper-plane-o" aria-hidden="true"></i>
				</a>
				<a href="{searchListUrl(bookmarkList)}"
					data-toggle="tooltip" data-placement="top" data-original-title=""
					title="{opts.msg.searchInBookmarkList}"> 
					<i class="fa fa-search" aria-hidden="true"></i>
				</a>
				<a href="{miradorUrl(bookmarkList)}" target="_blank" title="{opts.msg.openInMirador}"> 
					<i class="fa fa-th" aria-hidden="true"></i>
				</a>
			</div>
			<div class="col-xs-1 no-padding">
				<span class="{mainClass}-counter">{bookmarkList.numItems}</span>
			</div>
		</div>

	</li>

</ul>

<ul if="{!opts.bookmarks.config.userLoggedIn}" each="{bookmarkList in getBookmarkLists()}" class="{mainClass} list">

	
	<li each="{bookmark in bookmarkList.items}">
		<div class="row no-margin">
			<div class="col-xs-4 no-padding">
				<div class="{mainClass}-image"
					style="background-image: url({bookmark.representativeImageUrl});">
				</div>
			</div>
			<div class="col-xs-7 no-padding">
				<h4>
					<a href="{opts.bookmarks.root}{bookmark.url}">{bookmark.name}</a>
				</h4>
			</div>
			<div class="col-xs-1 no-padding {mainClass}-remove">
				<button class="btn btn--clean" type="button"
					data-bookshelf-type="delete" onclick="{remove}">
					<i class="fa fa-ban" aria-hidden="true"></i>
				</button>
			</div>
		</div>
	</li>
</ul>

<div if="{!opts.bookmarks.config.userLoggedIn}" each="{bookmarkList in getBookmarkLists()}" class="{mainClass}-actions test">


	<div if="{mayEmptyList(bookmarkList)}" class="{mainClass}-reset">
		<button class="btn btn--clean" type="button"
			data-bookshelf-type="reset" onclick="{deleteList}">
			<span>{opts.msg.resetBookmarkLists}</span>
			<i class="fa fa-trash-o" aria-hidden="true"></i>
		</button>
	</div>


	<div if="{maySendList(bookmarkList)}" class="{mainClass}-send">
		<a href="{sendListUrl(bookmarkList)}"> 
			<span>{opts.msg.sendBookmarkList}</span> 
			<i class="fa fa-paper-plane-o" aria-hidden="true"></i>
		</a>
	</div>


	<div if="{maySearchList(bookmarkList)}" class="{mainClass}-search">
		<a href="{searchListUrl(bookmarkList)}"
			data-toggle="tooltip" data-placement="top" data-original-title=""
			title=""> 
			<span>{opts.msg.searchInBookmarkList}</span> 
			<i class="fa fa-search" aria-hidden="true"></i>
		</a>
	</div>

	<div if="{mayCompareList(bookmarkList)}" class="{mainClass}-mirador">
		<a href="{miradorUrl(bookmarkList)}" target="_blank"> 
			<span>{opts.msg.openInMirador}</span> 
			<i class="fa fa-th" aria-hidden="true"></i>
		</a>
	</div>
</div>



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
    let lists =  this.opts.bookmarks.getBookmarkLists();
    return lists;
}

updateLists() {
    this.opts.bookmarks.listsNeedUpdate.onNext();
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
    let url = this.opts.bookmarks.config.root + "/bookmarks/search/" + list.name + "/";
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

</script> 

</bookmarkList>

