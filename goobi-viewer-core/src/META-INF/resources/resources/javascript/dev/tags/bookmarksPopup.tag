<bookmarksPopup class="bookshelf-popup bottom">

	<div class="bookshelf-popup__body-loader"></div>

	<div class="bookshelf-popup__header">
		{this.opts.msg.selectBookmarkList}
	</div>

	<div class="bookshelf-popup__body">
		<bookmarkList data="{this.opts.data}" msg="{this.opts.msg}" button="{this.opts.button}" bookmarks="{this.opts.bookmarks}"></bookmarkList>
	</div>
	
	<div class="bookshelf-popup__footer">
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

console.log("opts ", this.opts);
this.opts.data.loader = ".bookshelf-popup__body-loader";

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
    console.log("add bookshelf ", name);
}

</script>

</bookmarksPopup>