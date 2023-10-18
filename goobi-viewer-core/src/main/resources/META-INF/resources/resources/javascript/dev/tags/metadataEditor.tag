<!-- 
	Creates a multilanguage input form for an array of metadata objects
	Each metadata objects must have the following form
	{	
		property: "title",
		label: "Titel",
		value: "Mein Titel",
		required: true|false,
		helptext: "Hilfetext zum Titel" (optional),
		editable: true|false (optional)
	}
	The array of metadata must be provided in opts.metadata
	The supported languages must be provided in opts.languages
	A rxjs.observable returning a metadata array may be added in opts.provider to change the metadata on the fly 
	deleteListener: An observable notified by a delete command (optional)
	updateListener: rxjs.Observable triggered at onChange (optional)
	deleteLabel: a label for the delete button
 -->
<metadataEditor> 
	<div if="{this.metadataList}">
	<h2>Pin content</h2>
	<div class="admin__language-tabs">
		<ul class="nav nav-tabs">
				<li each="{language, index in this.opts.languages}"
					class="admin__language-tab {language == this.currentLanguage ? 'active' : ''}">
					<a onclick="{this.setCurrentLanguage}">{language}</a> 
				</li>
		</ul>
		</div>
		<div class="cms__geomap__featureset_panel ">
			<div class="active">

				<div class="input_form">
					<div each="{metadata, index in this.metadataList}" class="input_form__option_group">
						<div class="input_form__option_label">
							<label for="input-{metadata.property}">{metadata.label}:</label>
						</div>
						<div class="input_form__option_marker {metadata.required ? 'in' : ''}">
							<label>*</label>
						</div>
						<div class="input_form__option_control">
							<input tabindex="{index+1}" disabled="{this.isEditable(metadata) ? '' : 'disabled' }" ref="input" if="{metadata.type != 'longtext'}" type="{metadata.type}" id="input-{metadata.property}"
								class="form-control"
								value="{getValue(metadata)}"
								oninput="{this.updateMetadata}"/>
							 <textarea tabindex="{index+1}" disabled="{this.isEditable(metadata) ? '' : 'disabled' }" ref="input" if="{metadata.type == 'longtext'}" id="input-{metadata.property}"
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
					<div class="admin__geomap-edit-delete-wrapper">
						<a if="{this.opts.deleteListener}" disabled="{this.mayDelete() ? '' : 'disabled' }" class="btn btn--clean -redlink" onclick="{this.notifyDelete}">{this.opts.deleteLabel}</a>
					</div>
				</div>
			</div>
		</div>
	</div>
 <script>
    
 	this.on("mount", () => {
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
 	}
 	
 	updateMetadata(event) { 
 	    let metadata = event.item.metadata;
 	    if(!metadata.value) {
 	        metadata.value = {};
 	    }
 	    let value = event.target.value;
 	    if(value) {
	 	    metadata.value[this.currentLanguage] = [event.target.value]; 	        
 	    } else {
 	       metadata.value[this.currentLanguage] = undefined;
 	    }
 	    if(this.opts.updateListener) {
 	       this.opts.updateListener.next(metadata);
 	    }
 	}
 	
 	getValue(metadata) {
 	    if(metadata.value && metadata.value[this.currentLanguage]) { 	        
	 	    let value = metadata.value[this.currentLanguage][0];
	 	    return value;
 	    } else {
 	        return "";
 	    }
 	}
 	
 	setCurrentLanguage(event) {
 	    this.currentLanguage = event.item.language;
 	    this.update();
 	}
 	
 	notifyDelete() {
 	    this.opts.deleteListener.next();
 	}
 	
 	isEditable(metadata) {
 	    return metadata.editable === undefined || metadata.editable === true;
 	}
 	
 	mayDelete() {
 	    editable = this.metadataList.find( md => this.isEditable(md));
 	    return editable !== undefined;
 	}
 
</script> 

</metadataEditor>


