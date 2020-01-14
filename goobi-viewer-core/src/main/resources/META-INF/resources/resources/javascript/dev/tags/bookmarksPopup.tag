<bookmarksPopup class="bookmark-popup bottom">

	<div class="bookmark-popup__body-loader"></div>

	<div if="{opts.data.page !== undefined}" class="bookmark-popup__radio-buttons">
		<div>
			<label><input type="radio" checked="{opts.bookmarks.isTypeRecord()}" name="bookmarkType" value="{opts.msg.typeRecord}" onclick="{setBookmarkTypeRecord}"/>{opts.msg.typeRecord}</label>
		</div>
		<div>
			<label><input type="radio" checked="{opts.bookmarks.isTypePage()}" name="bookmarkType" value="{opts.msg.typePage}" onclick="{setBookmarkTypePage}"/>{opts.msg.typePage}</label>
		</div>
	</div>
	
   <div class="bookmark-popup__header">
		{this.opts.msg.selectBookmarkList}
	</div>

	<div class="bookmark-popup__body">
		<bookmarkList data="{this.opts.data}" loader="{this.opts.loader}" msg="{this.opts.msg}" button="{this.opts.button}" bookmarks="{this.opts.bookmarks}"></bookmarkList>
	</div>
	
	<div class="bookmark-popup__footer">
		<div class="row no-margin">
			<div class="col-xs-11 no-padding">
				<input ref="inputValue" type="text" placeholder="{this.opts.msg.addNewBookmarkList}"/>
			</div>
			<div class="col-xs-1 no-padding">
				<button class="btn btn-clean" type="button" onclick="{add}"></button>
			</div>
		</div>
	</div>

<script>

const popupOffset = 6;

this.opts.loader = ".bookmark-popup__body-loader";

this.on( 'mount', function() {    	
	
	this.setPosition();
	this.addCloseHandler();
	
});

setPosition() {
    var $button = $(this.opts.button);
    var anchor = {
            x : $button.offset().left + $button.outerWidth()/2,
            y : $button.offset().top + $button.outerHeight(),
    }
    var position = {
            left: anchor.x - this.root.getBoundingClientRect().width/2,
            top: anchor.y + popupOffset
    }
    $(this.root).offset(position);
}

addCloseHandler() {
    
    $(this.root).on("click", function(event){
        event.stopPropagation();
    });
    
    $('body').one("click", function(event) {
        this.unmount(true);
        $(this.root).off();
        this.root.remove();
    }.bind(this));
}

add() {
    let name = this.refs.inputValue.value;
    this.refs.inputValue.value = "";
    this.opts.bookmarks.addBookmarkList(name)
    .then( () => {
        this.opts.bookmarks.listsNeedUpdate.onNext();
        this.update();
    })
}

setBookmarkTypeRecord() {
    this.opts.bookmarks.setTypeRecord();
    this.opts.bookmarks.listsNeedUpdate.onNext();
}

setBookmarkTypePage() {
    this.opts.bookmarks.setTypePage();
    this.opts.bookmarks.listsNeedUpdate.onNext();
}

hideLoader() {
    $(this.opts.data.loader).hide();
}

showLoader() {
    $(this.opts.data.loader).show();
}

</script>

</bookmarksPopup>