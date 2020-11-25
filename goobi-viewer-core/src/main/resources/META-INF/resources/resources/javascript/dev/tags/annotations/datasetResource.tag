<datasetResource>
	<div class="annotation__body__dataset">		
		<dl class="annotation__body__dataset__data_list" each="{field in dataFields}">
				<dt class="data_list__label">{getName(field)}: </dt>
				<dd class="data_list__value">{getValue(field)}</dd>
		</dl>
	</div>
<script>
    this.dataSet = {};
    this.dataFields = [];

	this.on("mount", () => {
		this.dataSet = this.opts.resource.data;
		this.dataFields = Object.keys(this.dataSet);
		viewerJS.initialized.subscribe(() => {
	        viewerJS.translator.addTranslations(this.dataFields)
			.then(() => this.update());
		});
	})
	
	getValue(field) {
	    let value = this.dataSet[field];
	    if(!value) {
	        return "";
	    } else if(Array.isArray(value)) {
	        return value.join("; ")
	    } else {
	        return value;
	    }
	}
	
	getName(field) {
	    return viewerJS.translator.translate(field);
	}


</script>

</datasetResource>
