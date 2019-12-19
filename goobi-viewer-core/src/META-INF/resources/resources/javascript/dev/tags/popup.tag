<popup>

	<yield></yield>

<script>

this.on( 'mount', function() {    	
	console.log("mount popup", this.opts);
	this.addCloseHandler();
	$(this.root).offset(this.opts.offset);
    $("body").append($(this.root));
    $(this.root).css("position", "absolute");
    $(this.root).show();
});


addCloseHandler() {
    console.log("add popup close handler");
    $(this.root).on("click", function(event){
        console.log("click root");
        event.stopPropagation();
    });
    
    $('body').one("click", function(event) {
        console.log("click body ", this);
        this.unmount(true);
        $(this.root).off();
        if(this.opts.myparent) {
            console.log("reattach to parent ")
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

