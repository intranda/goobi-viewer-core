<canvasPaginator>

<nav class="numeric-paginator" aria-label="{Crowdsourcing.translate(aria_label__nav_pagination)}">

	<ul>
		<li if="{getCurrentIndex() > 0}" class="numeric-paginator__navigate navigate_prev">
			<span  onclick="{this.loadPrevious}">
				<svg class="numeric-paginator__navigate-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
					<use riot-href="{getIconHref('chevron-left')}"></use>
				</svg>
			</span>
		</li>
		<li each="{canvas in this.firstCanvases()}" class="group_left {this.getIndex(canvas) == this.getCurrentIndex() ? 'numeric-paginator__active' : ''}">
			<span  index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span>
		</li>
		<li class="numeric-paginator__separator" if="{this.useMiddleButtons()}">...</li>
		<li each="{canvas in this.middleCanvases()}" class="group_middle {this.getIndex(canvas) == this.getCurrentIndex() ? 'numeric-paginator__active' : ''}">
			<span index="{this.getIndex(canvas)}" onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span>
		</li>
		<li class="numeric-paginator__separator" if="{this.useLastButtons()}">...</li>
		<li each="{canvas in this.lastCanvases()}" class="group_right {this.getIndex(canvas) == this.getCurrentIndex() ? 'numeric-paginator__active' : ''}">
			<span index="{this.getIndex(canvas)}"  onclick="{this.loadFromEvent}">{this.getOrder(canvas)}</span>
		</li>
		<li if="{getCurrentIndex() < getTotalImageCount()-1}" class="numeric-paginator__navigate navigate_next">
			<span  onclick="{this.loadNext}">
				<svg class="numeric-paginator__navigate-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
					<use riot-href="{getIconHref('chevron-right')}"></use>
				</svg>
			</span>
		</li>
	</ul>

</nav>

<script>
const ensureTrailingSlash = value => value.endsWith('/') ? value : `${value}/`;
const viewerConfig = window.viewerConfig || {};
this.iconBasePath = ensureTrailingSlash(viewerConfig.iconBasePath || viewerConfig.contextPath || '/');
this.getIconHref = iconName => `${this.iconBasePath}resources/icons/outline/${iconName}.svg#icon`;

this.on( "mount", function() {
	
    var paginatorConfig = {
	        previous: () => this.load(this.getCurrentIndex()-1),
	        next: () => this.load(this.getCurrentIndex()+1),
	        first: () => this.load(0),
	        last: () => this.load(this.getTotalImageCount()-1),
	}
	viewerJS.paginator.init(paginatorConfig);
    
});



loadFromEvent(e) {
    let index = parseInt(e.target.attributes["index"].value);
	this.load(index);
}
    
load(index) {
    if(index != this.getCurrentIndex() && index >= 0 && index < this.getTotalImageCount()) {        
		if(this.opts.actionlistener) {
			this.opts.actionlistener.next({
				action: "setImageIndex",
				value: index
			})
		}
    }
}

loadPrevious() {
    let index = this.getCurrentIndex()-1;
	this.load(index);
}

loadNext() {
    let index = this.getCurrentIndex()+1;
	this.load(index);
}


getCurrentIndex() {
    return this.opts.index
}

getIndex(canvas) {
    return this.opts.items.indexOf(canvas);
}

getOrder(canvas) {
    return this.getIndex(canvas) + 1;
}

getTotalImageCount() {
    return this.opts.items.length;
}

useMiddleButtons() {
    return this.getTotalImageCount() > 9 && this.getCurrentIndex() > 4 && this.getCurrentIndex() < this.getTotalImageCount()-5;
}

useLastButtons() {
    return this.getTotalImageCount() > 9;
}

firstCanvases() {
    if(this.getTotalImageCount() < 10) {
        return this.opts.items;
    } else if(this.getCurrentIndex() < 5) {
        return this.opts.items.slice(0, this.getCurrentIndex()+3);
    } else {
        return this.opts.items.slice(0, 2);
    }
}

middleCanvases() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5 && this.getCurrentIndex() > 4) {
        return this.opts.items.slice(this.getCurrentIndex()-2, this.getCurrentIndex()+3);
    } else {
        return [];
    }
}

lastCanvases() {
    if(this.getTotalImageCount() < 10) {
        return [];
    } else if(this.getCurrentIndex() < this.getTotalImageCount()-5) {
        return this.opts.items.slice(this.getTotalImageCount()-2);
    } else {
        return this.opts.items.slice(this.getCurrentIndex()-2);
    }
}

toPageNumber(e) {
    let page = parseInt(e.target.value);
    if(page > 0 && page <= this.getTotalImageCount()) {        
    	this.load(page-1);
    } else{
        alert(page + " is not a valid page number")
    }
}


</script>

</canvasPaginator>
