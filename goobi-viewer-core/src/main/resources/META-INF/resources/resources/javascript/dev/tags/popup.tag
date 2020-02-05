<popup>

	<yield></yield>

<script>

this.on( 'mount', function() {    	
	this.addCloseHandler();
	$(this.root).offset(this.opts.offset);
    $("body").append($(this.root));
    $(this.root).css("position", "absolute");
    $(this.root).show();
});


addCloseHandler() {
    $(this.root).on("click", function(event){
        event.stopPropagation();
    });
    
    $('body').one("click", function(event) {
        this.unmount(true);
        $(this.root).off();
        if(this.opts.myparent) {
             $(this.root).hide();
            $(this.opts.myparent).append($(this.root));
            $(this.root).offset({left:0, top:0});
        } else {
            this.root.remove();
        }
    }.bind(this));
    

}


</script>

</popup>

