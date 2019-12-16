<bookmarkList>


<ul class="{mainClass} list">

	<li each="{bookmarkList in getBookmarkLists()}">
	
		<button if="{pi}" class="btn btn--clean" type="button"  onclick="{inList(bookmarkList, this.pi, this.page, this.logid) ? remove : add}">
		<i if="{inList(bookmarkList, this.pi, this.page, this.logid)}" class="fa fa-check" aria-hidden="true"></i>
		
		{bookmarkList.name}
		<span>{bookmarkList.numItems}</span>
		</button>
		
		<div if="{!pi}" class="row no-margin">			
			<div class="col-xs-10 no-padding">
				<a href="{opts.bookmarks.config.root}/bookmarks/show/{bookmarkList.id}" >{bookmarkList.name}</a>
			</div>
			<div class="col-xs-2 no-padding">
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
this.mainClass = (this.opts.style && this.opts.style.mainClass) ? this.opts.style.mainClass : "bookshelf-popup__body-list";


this.on( 'mount', function() {    	
    this.updateLists();
    this.opts.bookmarks.listsUpdated.subscribe( () => this.onListUpdate());
});

getBookmarkLists() {
    return this.opts.bookmarks.getBookmarkLists();
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
    let list = event.item.bookmarkList
    this.opts.bookmarks.removeFromBookmarkList(list.id, this.pi, this.page, this.logid, this.opts.bookmarks.isTypePage())
    .then( () => this.updateLists())
}

inList(list, pi, page, logid) {
    return this.opts.bookmarks.inList(list, pi, page, logid);
}


</script>

</bookmarkList>