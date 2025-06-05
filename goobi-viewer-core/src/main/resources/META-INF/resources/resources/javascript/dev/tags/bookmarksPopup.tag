<bookmarksPopup class="bookmark-popup bottom" role="region" aria-label="{msg('bookmarks')}">

	<div class="bookmark-popup__body-loader"></div>

	<div if="{opts.data.page !== undefined}" class="bookmark-popup__radio-buttons">
		<div>
			<label><input type="radio" checked="{opts.bookmarks.isTypeRecord()}" name="bookmarkType" value="{msg('bookmarkList_typeRecord')}" onclick="{setBookmarkTypeRecord}"/>{msg('bookmarkList_typeRecord')}</label>
		</div>
		<div>
			<label><input type="radio" checked="{opts.bookmarks.isTypePage()}" name="bookmarkType" value="{msg('bookmarkList_typePage')}" onclick="{setBookmarkTypePage}"/>{msg('bookmarkList_typePage')}</label>
		</div>
	</div>
	
   <div class="bookmark-popup__header">
		{msg('bookmarkList_selectBookmarkList')}
	</div>

	<div class="bookmark-popup__body">
		<bookmarkList data="{this.opts.data}" loader="{this.opts.loader}" button="{this.opts.button}" bookmarks="{this.opts.bookmarks}"></bookmarkList>
	</div>
	
	<div class="bookmark-popup__footer">
		<div class="row no-margin">
			<div class="col-11 no-padding">
				<input ref="inputValue" type="text" placeholder="{msg('bookmarkList_addNewBookmarkList')}" aria-label="{msg('bookmarkList_addNewBookmarkList')}"/>
			</div>
			<div class="col-1 no-padding">
				<button class="btn btn-clean" type="button"  aria-label="{msg('bookmarkList_addNewBookmarkList')}" onclick="{add}">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 41.57 41.57">
					  <g id="icon-bs-add" transform="translate(-27.5 -243.5)">
					    <line id="Linie_12" data-name="Linie 12" class="cls-1" x2="41.57" transform="translate(27.5 264.285)"/>
					    <line id="Linie_13" data-name="Linie 13" class="cls-1" x2="41.57" transform="translate(48.285 243.5) rotate(90)"/>
					  </g>
					</svg>
				</button>
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
    var $popup = $(this.root);
    var popupOffset = 10; // small offset distance from button

    var anchor = {
        x: $button.offset().left + $button.outerWidth() / 2,
        y: $button.offset().top + $button.outerHeight(),
    };

    var popupWidth = $popup.outerWidth();
    var popupHeight = $popup.outerHeight();

    var position = {
        left: anchor.x - popupWidth / 2,
        top: anchor.y + popupOffset
    };
 
    // get viewport dimensions
    var viewportWidth = $(window).width();
    var viewportHeight = $(window).height();
    var scrollTop = $(window).scrollTop();

    // horizontal correction
    if (position.left < 0) {
        position.left = 0;
    } else if (position.left + popupWidth > viewportWidth) {
        position.left = viewportWidth - popupWidth;
    }

    // vertical correction
    if (position.top + popupHeight > scrollTop + viewportHeight) {
        position.top = $button.offset().top - popupHeight - popupOffset;
        if (position.top < scrollTop) {
            position.top = scrollTop + 10;
        }
    }

    $popup.offset(position);
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
        this.opts.bookmarks.listsNeedUpdate.next();
        this.update();
    })
}

setBookmarkTypeRecord() {
    this.opts.bookmarks.setTypeRecord();
    this.opts.bookmarks.listsNeedUpdate.next();
}

setBookmarkTypePage() {
    this.opts.bookmarks.setTypePage();
    this.opts.bookmarks.listsNeedUpdate.next();
}

hideLoader() {
    $(this.opts.data.loader).hide();
}

showLoader() {
    $(this.opts.data.loader).show();
}

msg(key) {
    return this.opts.bookmarks.translator.translate(key);
}

</script>

</bookmarksPopup>