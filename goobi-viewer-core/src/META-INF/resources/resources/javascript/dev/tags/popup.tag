<popup>

	<yield></yield>

<script>

this.on( 'mount', function() {    	
	console.log("mount popup");
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
    console.log("add popup close handler");
    $(this.root).on("click", function(event){
        console.log("click root");
        event.stopPropagation();
    });
    
    $('body').one("click", function(event) {
        console.log("click body");
        this.unmount(true);
        $(this.root).off();
        this.root.remove();
    }.bind(this));
}


</script>

</popup>

