<!-- 
	Creates a multilanguage input form for an array of metadata objects
	Each metadata objects must have the following form
	{	
		property: "title",
		label: "Titel",
		value: "Mein Titel",
		required: true|false,
		helptext: "Hilfetext zum Titel" (optional),
		listener: Rx.Observable triggered at onChange (optional)
	}
	The array of metadata must be provided in opts.metadata
	The supported languages must be provided in opts.languages
	A Rx.observable returning a metadata array may be added in opts.provider to change the metadata on the fly 
 -->
<metadataEditor> 
	<div if="{this.metadataList && this.metadataList.length > 0}">
		<ul class="nav nav-tabs">
				<li each="{language, index in this.opts.languages}"
					class="{language == this.currentLanguage ? 'active' : ''}">
					<a onclick="{this.setCurrentLanguage}">{language}</a> 
				</li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane active">

				<div class="input_form">
					<div each="{metadata, index in this.metadataList}" class="input_form__option_group">
						<div class="input_form__option_label">
							<label for="input-{metadata.property}">{metadata.label}:</label>
						</div>
						<div class="input_form__option_marker {metadata.required ? 'in' : ''}">
							<label>*</label>
						</div>
						<div class="input_form__option_control">
							<input ref="input" if="{metadata.type != 'longtext'}" type="{metadata.type}" id="input-{metadata.property}"
								class="form-control"
								value="{getValue(metadata)}"
								oninput="{this.updateMetadata}"/>
							 <textarea ref="input" if="{metadata.type == 'longtext'}" id="input-{metadata.property}"
								class="form-control"
								value="{getValue(metadata)}"
								oninput="{this.updateMetadata}"/>
						</div>
						<div if="{metadata.helptext}" class="input_form__option_help">
							<button type="button" class="btn btn--clean"
								data-toggle="helptext" for="help_{metadata.property}">
								<i class="fa fa-question-circle" aria-hidden="true"></i>
							</button>
						</div>
						<div if="{metadata.helptext}" id="help_{metadata.property}"
							class="input_form__option_control_helptext">{metadata.helptext}</div>
					</div>
				</div>
			</div>
		</div>
	</div>
 <script>
    
 	this.on("mount", () => {
 	    console.log("mount metadataEditor ", this.opts);
 	    this.currentLanguage = this.opts.currentLanguage;
 	    this.updateMetadataList(this.opts.metadata);
 	    this.focusInput();
 	    if(this.opts.provider) {
 	        this.opts.provider.subscribe( (metadata) => {
 	            this.updateMetadataList(metadata)
 	            this.update();
 	            this.focusInput();
 	        });
 	    }
 	})
 	
 	focusInput() {
 	    if(Array.isArray(this.refs.input)) {
 	        this.refs.input[0].focus();
 	    } else if(this.refs.input) {
 	        this.refs.input.focus();
 	    }
 	}
 	
 	updateMetadataList(metadataList) {
 	   this.metadataList = metadataList;
       	this.metadataList.forEach(md => {
   	        let valueObject = this.getValueForLanguages(this.opts.languages);
   	        if(typeof md.value == "string") {
   	            valueObject[this.currentLanguage] = [md.value];
   	        } else if(md.value) {
   	            $.extend(valueObject, md.value);
   	        }
   	        md.value = valueObject;
   	    })
 	}
 	
 	getValueForLanguages(languages) {
 	    let ret = {};
 	    languages.forEach(lang => {
 	        ret[lang] = []
 	    })
 	    return ret;
 	}
 	
 	updateMetadata(event) { 
 	    let metadata = event.item.metadata;
 	    metadata.value[this.currentLanguage] = [event.target.value];
 	    if(metadata.listener) {
 	        metadata.listener.next(metadata);
 	    }
 	}
 	
 	getValue(metadata) {
 	    let value = viewerJS.getMetadataValue(metadata.value, this.currentLanguage);
 	    return value;
 	}
 	
 	setCurrentLanguage(event) {
 	    this.currentLanguage = event.item.language;
 	    this.update();
 	}
 
</script> 

</metadataEditor>


