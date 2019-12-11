<bookmarkList>


<ul class="bookshelf-popup__body-list list">

	<li each="{bookmarkList in bookmarkLists}">
	
		<button class="btn btn--clean" type="button"  onclick="{inList(bookmarkList, this.pi, this.page, this.logid) ? remove : add}">
		<i if="{inList(bookmarkList, this.pi, this.page, this.logid)}" class="fa fa-check" aria-hidden="true"></i>
		
		{bookmarkList.name}
		<span>{bookmarkList.numItems}</span>
		</button>
	
	</li>

</ul>


<script> 

this.bookmarkLists = [];
this.pi = this.opts.data.pi;
this.logid = this.opts.data.logid;
this.page = this.opts.data.page;
this.loader = this.opts.data.loader; 
this.button = this.opts.button;

this.opts.bookmarks.listsNeedUpdate.subscribe( () => this.updateLists());

this.on( 'mount', function() {    	
    console.log("opts ", this.opts);
    this.updateLists()
    .then( () => this.hideLoader())
});

this.on( 'update', function() {  
    let $button = $(this.button);
    if($button.length > 0) {        
	    let contained = this.contained(this.pi, this.page, this.logid);
	    if(contained) {
	        $button.addClass("active");
	    } else {
	        $button.removeClass("active");

	    }
    }
})

updateLists() {
	console.log("update list");
    return this.opts.bookmarks.getBookmarkLists()
    .then(lists => {
        this.bookmarkLists = lists;
        this.update();
    })
}

hideLoader() {
    $(this.loader).hide();
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




getList(id) {
    this.bookmarkLists.find(list => list.id == id);
}

getItem(list, pi, page, logid) {
    for(item of list.items) {
        if(item.pi == pi && (page == undefined  || page == item.order) && (logid == undefined || logid == item.logId)) {
            return item;
        }
    }
    return undefined;
}


contained(pi, page, logid) {
    for(list of this.bookmarkLists) {
        if(this.inList(list, pi, page, logid)) {
            return true;
        }
    }
    return false;
}
        
inList(list, pi, page, logid) {
	console.log("check containment", list, pi, page, logid);
        for(item of list.items) {
        	if(this.opts.bookmarks.isTypeRecord() && item.pi == pi && item.order === null && item.logId === null) {
        		return true;
        	} else if(this.opts.bookmarks.isTypePage() && item.pi == pi && page == item.order ) {
                return true;
            }
        }
    return false;
}


</script>

</bookmarkList>