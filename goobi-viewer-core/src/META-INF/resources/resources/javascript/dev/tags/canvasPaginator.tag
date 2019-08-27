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
		this.opts.item.loadImage(index);
		this.update();
    }
}

getCurrentIndex() {
    return this.opts.item.currentCanvasIndex;
}

getIndex(canvas) {
    return this.opts.item.canvases.indexOf(canvas);
}

getOrder(canvas) {
    return this.getIndex(canvas) + 1;
}

getTotalImageCount() {
    return this.opts.item.canvases.length;
}

useMiddleButtons() {
    return this.getTotalImageCount() > 9 && this.getCurrentIndex() > 4 && this.getCurrentIndex() < this.getTotalImageCount()-5;
}

useLastButtons() {
    return this.getTotalImageCount() > 9;
}

firstCanvases() {
    if(this.getTotalImageCount() < 10) {
        return this.opts.item.canvases;
    } else if(this.getCurrentIndex() < 5) {
        return this.opts.item.canvases.slice(0, this.getCurrentIndex()+3);
    } else {
        return this.opts.item.canvases.slice(0, 2);
    }
}

middleCanvases() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5 && this.getCurrentIndex() > 4) {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2, this.getCurrentIndex()+3);
    } else {
        return [];
    }
}

lastCanvases() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5) {
        return this.opts.item.canvases.slice(this.getTotalImageCount()-2);
    } else {
        return this.opts.item.canvases.slice(this.getCurrentIndex()-2);
    }
}


</script>

</canvasPaginator>