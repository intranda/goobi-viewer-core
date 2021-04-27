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

	<div class="archives__object-thumbnails-image-wrapper" each="{canvas, index in thumbnails}">
		<a class="archives__object-thumbnails-image-link" href="{getLink(canvas)}">
			<img class="archives__object-thumbnails-image" alt="{getValue(canvas.label)}" src="{getImage(canvas)}" />
		<div class="archives__object-thumbnails-image-overlay">
			<div class="archives__object-thumbnails-label">{getValue(canvas.label)}</div>
		</div>
		</a>
	</div>



<script>

this.thumbnails = [];

this.on("mount", () => {
	console.log("mount ", this.opts);
	this.type = opts.type ? opts.type : "items";
	this.language = opts.language ? opts.language : "en";
	this.imageSize = opts.imageSize;
	
	let source = opts.source;
	if(viewerJS.isString(source)) {
		fetch(source)
		.then(response => response.json())
		.then(json => this.loadThumbnails(json, this.type));
	} else {
		this.loadThumbnails(source, this.type);
	}
});

loadThumbnails(source, type) {
	console.log("Loading thumbnails from ", source);

	switch(type) {
		case "structures":
			rxjs.from(source.structures)
			.pipe(
					rxjs.operators.map(range => this.getFirstCanvas(range, true)),
					rxjs.operators.concatMap(canvas => this.loadCanvas(canvas))
					)
			.subscribe(item => this.addThumbnail(item));
			break;
		case "items":
		case "default":
			this.createThumbnails(source.items)
	}
	
}

addThumbnail(item) {
	console.log("add thumbnail from ", item);
	this.thumbnails.push(item);
	this.update();
}

createThumbnails(items) {
	console.log("creating thumbnails from ", items)
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

getImage(canvas) {
	console.log("get image from ", canvas);
	if(canvas.items) {
		return canvas.items
		.filter(page => page.items != undefined)
		.flatMap(page => page.items)
		.filter(anno => anno.body != undefined)
		.map(anno => anno.body)
		.map(res => this.getImageUrl(res, this.imageSize))
		.find(url => url != undefined)
	} else {
		return undefined;
	}
}

getImageUrl(resource, size) {
	console.log("get image url ", resource, size);
	if(size && resource.service && resource.service.length > 0) {
		let url = resource.service[0].id;
		return url + "/full/" + size + "/0/default." + this.getExtension(resource.format);
	} else {
		return resource.id;
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

</script>

</thumbnails>