<htmltextResource>
	<div ref="container" class="annotation__body__htmltext">
<!-- 		<iframe srcdoc="{this.opts.resource.value}" sandbox/> -->
<!-- 		<iframe sandbox src="data:text/html,{this.opts.resource.value}"/> -->
	</div>

<script>

	this.on("mount", () => {
	    this.refs.container.innerHTML = this.opts.resource.value;
	})

</script>

</htmltextResource>