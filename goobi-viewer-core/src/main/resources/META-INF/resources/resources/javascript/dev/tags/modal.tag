<modal>

	<div class="modal fade {modalClass}" id="{modalId}" tabindex="-1" ref="modal"
		role="dialog"
		aria-labelledby="{modalTitle}"
		aria-hidden="true">
		<div class="modal-dialog modal-dialog-centered" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<h1 class="h2 modal-title">{modalTitle}</h1>
					<button class="fancy-close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">x</span>
					</button> 
				</div>
				<div class="modal-body">
					<yield from="body"></yield>
				</div>
				<div class="modal-right">
					<yield from="right"></yield>
				</div>
				<div class="modal-footer">
					<yield from="footer"></yield>
				</div>
			</div>
		</div>
		<div class="alt-backdrop"></div>
	</div>
	
	<script>
	
    this.modalClass = this.opts.styleclass ? this.opts.styleclass : "";
    this.modalId = this.opts.modalid;
    this.modalTitle = this.opts.title;
	
	this.on("mount", () => {
	   
	    if(this.opts.onClose) {
	        $(this.refs.modal).on('hide.bs.modal', () => this.opts.onClose());
	    }
	});
	
	</script>

</modal>