<!--
 This tag displays an array of thumbnail images taken from the opts.source property. 
For ambigious sources, the additional opts.type property determines how the source is to be interpreted to create the thumbnails

 The source may be one of the following:
 * a iiif 3.0 manifest 
 * more to come...
 The source may either be a json object or an url string pointing one

 Valid values for opts.type are
 * items: create thumbnails for all elements in the items property of the source (canvases for manifests)
 * structures: create thumbnails for all elements in the structures property of the source (ranges/structElements). One thumbnail is created from the first canvas of each top level range or the canvas references in the start property of the range if it exists

-->

<thumbnails>
		<div ref="thumb" class="thumbnails-image-wrapper {this.opts.index == index ? 'selected' : ''} {getPageStatus(index)}" each="{canvas, index in thumbnails}">
			<a class="thumbnails-image-link" href="{getLink(canvas)}" aria-label="{getAriaLabel(canvas, index)}" aria-current="{this.opts.index == index ? 'page' : undefined}" tabindex="{needsKeyboardFocus(canvas) ? '0' : undefined}" role="{needsKeyboardFocus(canvas) ? 'link' : undefined}" onclick="{handleClickOnImage}" onkeydown="{handleKeydownOnImage}">
				<!-- the link carries the accessible name via aria-label; a speaking alt would be read twice by screen readers -->
				<img class="thumbnails-image" alt="" src="{getImage(canvas)}" loading="lazy" data-viewer-thumbnail="thumbnail"/>
			<div class="thumbnails-image-overlay">
				<div class="thumbnails-label">{getValue(canvas.label)}</div>
			</div>
			</a>
		</div>

<script>

this.thumbnails = [];
this._debug = false;

this.on("mount", () => {
	if(this._debug)console.log("mount ", this.opts);
	this.type = opts.type ? opts.type : "items";
	this.language = opts.language ? opts.language : "en";
	this.imageSize = opts.imagesize;
	if(this.opts.index === undefined) {
		this.opts.index = 0;
	}
	
	let source = opts.source;
	if(viewerJS.isString(source)) {
		fetch(source)
		.then(response => response.json())
		.then(json => this.loadThumbnails(json, this.type))
		.catch(e => {
			console.error("Error reading manifest from ", source);
		})
	} else {
		this.loadThumbnails(source, this.type);
	} 
});

this.on("updated", () => {
	if(this._debug)console.log("updated", this.opts);
	let activeThumb = this.refs.thumb[this.opts.index];
	if(activeThumb) {		
		activeThumb.scrollIntoView({block: "end", behavior: "smooth"});
	}
	//the thumbnail loader runs before this tag creates is html, so the img elements are not registered in the thumbnailLoader. Trigger it manually
	window.viewerJS?.thumbnailLoader?.loadAll();
	if(this.opts.onload) {
	    this.opts.onload();
	}
});

loadThumbnails(source, type) {
    if(this._debug)console.log("Loading thumbnails from ", source);
	if(source) {
		switch(type) {
			case "structures":
				if(this._debug)console.log("structures", source.structures);
				rxjs.from(source.structures)
				.pipe(
						rxjs.operators.map(range => this.getFirstCanvas(range, true)),
						rxjs.operators.filter(canvas => canvas != undefined),
						rxjs.operators.concatMap(canvas => this.loadCanvas(canvas))
						)
				.subscribe(item => this.addThumbnail(item));
				break;
			case "sequence":
				this.createThumbnails(source.sequences[0].canvases);
				break;
			case "items":
			case "default":
				this.createThumbnails(source.items)
		}
	} else {
		throw "source manifest not defined";
	}
	
}

addThumbnail(item) {
    if(this._debug)console.log("add thumbnail from ", item);
    
	this.thumbnails.push(item);
	this.update();
}

createThumbnails(items) {
    if(this._debug)console.log("creating thumbnails from ", items);

	this.thumbnails = items;
	this.update();
}

getFirstCanvas(range, overwriteLabel) {

	let canvas = undefined;
	if(range.start) {
		canvas = range.start;
	} else if(range.items) {
		canvas = range.items.find( item => item.type == "Canvas");
	}
	if(canvas && overwriteLabel) {
		if(this.opts.label) {
			let md = range.metadata.find(md => viewerJS.iiif.getValue(md.label, "none") == this.opts.label);
			if(md) {
				canvas.label = this.getValue(md.value);
			} else {
				canvas.label = range.label;
			}
		} else {			
			canvas.label = range.label;
		}

	}
	return canvas;
}

loadCanvas(source) {
	return fetch(viewerJS.iiif.getId(source))
	.then(response => response.json())
	.then(canvas => {
		//use label of source if present to allow previously given custom labels
		if(source.label) {
			canvas.label = source.label;
		}
		return canvas;
	})
}

getValue(value) {
	return viewerJS.iiif.getValue(value, this.language, this.language == "en" ? "de" : "en");
}

/**
 * Returns the accessible name for a thumbnail link: the canvas label, or the
 * 1-based page number as fallback, prefixed with the translated opts.msg.page
 * text if the mounting code provides one.
 * @param {Object} canvas - IIIF canvas the thumbnail is created from
 * @param {Number} index - zero-based thumbnail index
 * @returns {String} the accessible name for the link
 */
getAriaLabel(canvas, index) {
	let label = this.getValue(canvas.label);
	label = typeof label === "string" ? label.trim() : "";
	if(label) {
		return label;
	}
	let pageNumber = String(index + 1);
	return this.opts.msg && this.opts.msg.page ? this.opts.msg.page + " " + pageNumber : pageNumber;
}

/**
 * Determines whether a thumbnail link must be made focusable and
 * keyboard-activatable manually: only when it has no real href and clicks are
 * handled by the actionlistener instead.
 * @param {Object} canvas - IIIF canvas the thumbnail is created from
 * @returns {Boolean} true if tabindex, role and Enter activation are needed
 */
needsKeyboardFocus(canvas) {
	return !this.getLink(canvas) && this.opts.actionlistener != undefined;
}

getImage(canvas) {
// 	console.log("get image from ", canvas);
	if(canvas.items) {
		return canvas.items
		.filter(page => page.items != undefined)
		.flatMap(page => page.items)
		.filter(anno => anno.body != undefined)
		.map(anno => anno.body)
		.map(res => this.getImageUrl(res, this.imageSize))
		.find(url => url != undefined)
	} else if(canvas.images && canvas.images.length > 0) {
		return this.getImageUrl(canvas.images[0].resource, this.imageSize);                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
	} else {
		return undefined;
	}
}

getImageUrl(resource, size) {
// 	console.log("get image url ", resource, size);
	if(size && resource.service && (!Array.isArray(resource.service) || resource.service.length > 0)) {
		let url = viewerJS.iiif.getId(viewerJS.iiif.getId(resource.id) ? resource.service[0] : resource.service);
		return url + "/full/" + size + "/0/default." + this.getExtension(resource.format);
	} else {
		return viewerJS.iiif.getId(resource);
	}
}

getExtension(format) {
	if(format && format == "image/png") {
		return "png";
	} else {
		return "jpg";
	}
}

getLink(canvas) {
	if(this.opts.link) {
		return this.opts.link(canvas);
	} else {
		return this.getHomepage(canvas);
	}
}

getHomepage(canvas) {
	if(canvas.homepage && canvas.homepage.length > 0) {
		return canvas.homepage[0].id;
	} else {
		return undefined;
	}
}

/**
 * Notifies the actionlistener about an activated thumbnail. Modified clicks
 * (Ctrl/Cmd/Shift) keep the browser default so "open in new tab" still works.
 * @param {Event} event - click event, or a keydown event delegated by handleKeydownOnImage
 */
handleClickOnImage(event) {
	if(this.opts.actionlistener && !(event.ctrlKey || event.metaKey || event.shiftKey)) {
		this.opts.actionlistener.next({
			action: "clickImage",
			value: event.item.index
		})
		//navigation is handled by the actionlistener -> don't follow the thumbnail link
		event.preventDefault();
	}
	//updating is handled in actionlistener. set this to prevent double update
	event.preventUpdate = true;
}

/**
 * Activates a thumbnail link that has no real href (see needsKeyboardFocus)
 * with the Enter key, so it is keyboard-operable like a native link.
 * Enter only: the element announces itself as a link, not as a button.
 * @param {Event} event - keydown event from the thumbnail link
 */
handleKeydownOnImage(event) {
	event.preventUpdate = true;
	if(event.key === "Enter" && this.needsKeyboardFocus(event.item.canvas)) {
		event.preventDefault();
		this.handleClickOnImage(event);
	}
}

getPageStatus(index) {
	if(this.opts.statusmap) {
		return this.opts.statusmap.get(index);
	}
}


</script>

</thumbnails>