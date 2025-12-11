

const _config = {
	currentLogId: "",
	areas: [{
		logId: "",
		coords: "",

	}]
}

const _debug = false;

export default class PageAreas {

	constructor(config, image) {
		if(_debug)console.log("init page areas ", config);
		this.areas = config.areas;
		let styles = viewerJS.helper.getCss("page-area", ['borderTopColor', 'borderTopWidth', 'background-color']);
		let style = {
				borderWidth: styles["borderTopWidth"],
				borderColor: styles["borderTopColor"],
				fillColor: styles["background-color"]
		}
		let activeStyles = viewerJS.helper.getCss("page-area focus", ['borderTopColor', 'borderTopWidth', 'background-color']);
		let activeStyle = {
				borderWidth: parseInt(activeStyles["borderTopWidth"]),
				borderColor: activeStyles["borderTopColor"],
				fillColor: activeStyles["background-color"]
		}
		
		image.viewer.onOpened.subscribe(viewer => {
			if(_debug)console.log("page areas on viewer open ", config);
			image.viewer.openseadragon.addHandler("canvas-press", () => {
				this.dragging = false;
			})
			image.viewer.openseadragon.addHandler("canvas-drag", () => {
				this.dragging = true;
			})
			
			let activeAreas = this.areas.filter(a => a.logId === config.currentLogId);
			if(_debug)console.log("active areas: ", activeAreas);
			if(activeAreas.length > 0) {
				this.drawActiveAreas(activeAreas, image);
				this.initAreaClick(image, activeAreas);
			} else {
				let inactiveAreas = this.areas.filter(a => a.logId !== config.currentLogId);
				let inactiveLogIds = inactiveAreas.map(a => a.logId).filter((v, i, a) => a.indexOf(v) === i); //last filter to make logIds unique
				inactiveLogIds.forEach(logId => {
					let logIdAreas = this.areas.filter(a => a.logId === logId);
					logIdAreas.forEach( (area, index) => this.drawArea(area, index, image));				    
				})
				this.initAreaClick(image, inactiveAreas);				    
			}
		});
	}
	
	setBrowserLocation(pi, pageNo) {
		const url = new URL(window.location.href);
		let pathArray = url.pathname.split("/");
		let replacedPathPart = false;
		for (var i = pathArray.length-1; i >= 0; i--) {
			if(/^\d+$/.test(pathArray[i]) && pathArray[i] != pi) {
				//this is presumably the page number
				pathArray[i] = pageNo;
				replacedPathPart = true;
				break;
			}
		}
		if(!replacedPathPart) {
			if(pathArray[pathArray.length-1].length == 0) {
				pathArray.pop();
			}
			pathArray.push(pageNo)
		}
		url.pathname = pathArray.join("/");
		
		//update tab url
		//alternatively use history.addState to add an entry to the browser history, so navigating back leads to the previous page
		window.history.replaceState(null, "", url.toString());
	}
	
	drawActiveAreas(activeAreas, imageView) {
		let areasOnCanvas = [];
		activeAreas.forEach((activeArea, index) => {				        
			let rect = ImageView.CoordinateConversion.convertToOpenSeadragonObject(activeArea.coords);
			rect = ImageView.CoordinateConversion.scaleToOpenSeadragon(rect, imageView.viewer.openseadragon, imageView.viewer.getOriginalImageSize());
			areasOnCanvas.push(rect);
			this.drawArea(activeArea, index, imageView);
			let scrollPosition = window.sessionStorage.getItem("scrollPosition");
			$(document).scrollTop(parseInt(scrollPosition));
			window.sessionStorage.removeItem("scrollPosition");
		})
		let shadow = new ImageView.ImageFilters.HighlightArea(imageView.viewer, 0.5, areasOnCanvas, true);
		shadow.start();
	}

	initAreaClick(imageView, areas) {
		if(areas.length > 0) {
			let url = areas[0].url.replace("/" + areas[0].logId, "");

			const viewerClickPipe = imageView.viewer.onClick.pipe(rxjs.operators.map(() => url))
			const areaClicks = [];
			areas.forEach((area) => {
				const areaClickPipe = area.overlay?.onClick().pipe(rxjs.operators.map(() => area.url))
				areaClicks.push(areaClickPipe);
			});

			rxjs.merge(
				viewerClickPipe,
				...areaClicks
			)
			.pipe(
				rxjs.operators.filter(e => !this.dragging),
				rxjs.operators.debounceTime(10) 
			)
			.subscribe(url => {
				if(_debug)console.log("viewer click ", url, window.location.href);
				if(url && url.length && url !== window.location.href) {
					window.sessionStorage.setItem("scrollPosition", $(document).scrollTop());
					window.location.href = url;
				}
			})

		}
	}

	drawArea(area, shapeIndex, image, clickToLeave) {
		let rect = ImageView.CoordinateConversion.convertToOpenSeadragonObject(area.coords);
		rect = ImageView.CoordinateConversion.scaleToOpenSeadragon(rect, image.viewer.openseadragon, image.viewer.getOriginalImageSize());
		let $area = $("#pageAreaFrame_" + area.logId + "_" + shapeIndex);
		let $label = $("#pageAreaLabel_" + area.logId + "_" + shapeIndex);
		let overlayId = area.logId + "_" + shapeIndex;
		let overlayConfig = {
			element: $area.get(0),
		}
		area.overlay = new ImageView.Overlay(rect, overlayConfig, overlayId);

		area.tooltip = new ImageView.Tooltip(area.overlay, area.label, image.viewer, {
			onHover: true,
			className: "page-area-label page-area-label-text",
			element: $label.get(0),
			placement: "bottom"
		})
		document.body.append($label.get(0));
		//tooltip.show();
		area.overlay.draw(image.viewer);
		$area.hover(
			() => $(area.tooltip.element).addClass("hover"),
			() => $(area.tooltip.element).removeClass("hover")
		);

	}
}