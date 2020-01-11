<progressBar>
	<div class="goobi-progress-bar-wrapper">
		<div class="goobi-progress-bar">
			<div each="{value, index in this.values}" 
			class="goobi-progress-bar__bar {styleClasses[index]}" style="width: {getRelativeWidth(value)};">
			</div>
		</div>
	</div>

<script>
	this.values = JSON.parse(this.opts.values);
	this.styleClasses = JSON.parse(this.opts.styleclasses);
	console.log("init progressbar ", this.values, this.styleClasses);
	
	this.on("mount", function() {
	    let bar = this.root.querySelector(".goobi-progress-bar");
	    this.totalBarWidth = bar.getBoundingClientRect().width;
		this.update();
	})

	getRelativeWidth(value) {
		    let barWidth = value/this.opts.total*this.totalBarWidth;
		    return barWidth + "px"; 
	}
	
	loaded() {
	    console.log("on load");
	}

</script>

</progressBar>