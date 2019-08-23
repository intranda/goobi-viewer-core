<canvasPaginator>

<div class="canvas_paginator">

	<div class="canvas_paginator__list">
		<div each="{canvas in this.firstCanvases()}" class="canvas_paginator__button group_left {this.getIndex(canvas) == this.getCurrentIndex() ? 'current' : ''}" index="{this.getIndex(canvas)}" onclick="{this.load}">{this.getOrder(canvas)}</div>
		<div class="canvas_paginator__separator" if="{this.useMiddleButtons()}">...</div>
		<div each="{canvas in this.middleCanvases()}" class="canvas_paginator__button group_middle {this.getIndex(canvas) == this.getCurrentIndex() ? 'current' : ''}" index="{this.getIndex(canvas)}" onclick="{this.load}">{this.getOrder(canvas)}</div>
		<div class="canvas_paginator__separator" if="{this.useLastButtons()}">...</div>
		<div each="{canvas in this.lastCanvases()}" class="canvas_paginator__button group_right {this.getIndex(canvas) == this.getCurrentIndex() ? 'current' : ''}" index="{this.getIndex(canvas)}" onclick="{this.load}">{this.getOrder(canvas)}</div>
	</div>
	
</div>

<script>

load(e) {
    let index = parseInt(e.target.attributes["index"].value);
    if(index != this.getCurrentIndex()) {        
	    console.log("paginator loading image " + index);
		this.opts.loader.loadImage(index);
		this.update();
    }
}

getCurrentIndex() {
    return this.opts.loader.imageIndex;
}

getIndex(canvas) {
    return this.opts.loader.canvasList.indexOf(canvas);
}

getOrder(canvas) {
    return this.getIndex(canvas) + 1;
}

useMiddleButtons() {
    return this.opts.loader.canvasList.length > 9 && this.opts.loader.imageIndex > 4 && this.opts.loader.imageIndex < this.opts.loader.canvasList.length-5;
}

useLastButtons() {
    return this.opts.loader.canvasList.length > 9;
}

firstCanvases() {
    if(this.opts.loader.canvasList.length < 10) {
        return this.opts.loader.canvasList;
    } else if(this.opts.loader.imageIndex < 5) {
        return this.opts.loader.canvasList.slice(0, this.opts.loader.imageIndex+3);
    } else {
        return this.opts.loader.canvasList.slice(0, 2);
    }
}

middleCanvases() {
    if(this.opts.loader.canvasList.length < 10) {
        return [];
    } else if(this.opts.loader.imageIndex < this.opts.loader.canvasList.length-5 && this.opts.loader.imageIndex > 4) {
        return this.opts.loader.canvasList.slice(this.opts.loader.imageIndex-2, this.opts.loader.imageIndex+3);
    } else {
        return [];
    }
}

lastCanvases() {
    if(this.opts.loader.canvasList.length < 10) {
        return [];
    } else if(this.opts.loader.imageIndex < this.opts.loader.canvasList.length-5) {
        return this.opts.loader.canvasList.slice(this.opts.loader.canvasList.length-2);
    } else {
        return this.opts.loader.canvasList.slice(this.opts.loader.imageIndex-2);
    }
}


</script>

</canvasPaginator>