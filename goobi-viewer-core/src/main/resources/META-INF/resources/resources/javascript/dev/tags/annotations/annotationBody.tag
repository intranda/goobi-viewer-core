<annotationBody>
	<plaintextResource if="{isPlaintext()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" ></plaintextResource>
	<htmltextResource if="{isHtml()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}"></htmltextResource>
	<geoMapResource if="{isGeoJson()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" mapboxtoken="{this.opts.mapboxtoken}" initialView="{this.opts.geomap.initialView}"></geoMapResource>
	<authorityResource if="{isAuthorityResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></authorityResource>
	<datasetResource if="{isDatasetResource()}" resource="{this.annotationBody}" annotationid="{this.opts.annotationid}" currentlang="{this.opts.currentlang}" resturl="{this.opts.resturl}"></datasetResource>
<script>

this.on("mount", () => {
    if(this.opts.contentid) {    
        let content = document.getElementById(this.opts.contentid).innerText;
        try {
	        this.annotationBody = JSON.parse(content);
	        this.type = this.annotationBody.type;
	        if(!this.type) {
	            this.type = this.anotationBody["@type"];
	        }
	        this.format = this.annotationBody.format;
    	} catch(e) {
    	    this.annotationBody = {value: content};
    	    this.type = "TextualResource";
    	    this.format = "text/plain";
   		}
        this.update();
    }
})

isPlaintext() {
    if(this.type == "TextualBody" || this.type == "TextualResource") {
        return !this.format || this.format == "text/plain";
    }
    return false;
}

isHtml() {
    if(this.type == "TextualBody" || this.type == "TextualResource") {
        return this.format == "text/html";
    }
    return false;
}

isGeoJson() {
    return this.type == "Feature";
}

isAuthorityResource() {
    return this.type == "AuthorityResource";
}

isDatasetResource() {
    return this.type == "Dataset";
}


</script>

</annotationBody>

