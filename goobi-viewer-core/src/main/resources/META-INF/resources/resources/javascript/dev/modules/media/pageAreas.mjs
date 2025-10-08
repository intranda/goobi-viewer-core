

       	 	
		    //init page areas
       	 	function drawPageAreas(config, imageView) {
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
				
        	    imageView.viewer.addHandler("canvas-press", () => {
        	        imageView.dragging = false;
        	    })
        	    imageView.viewer.addHandler("canvas-drag", () => {
        	        imageView.dragging = true;
        	    })
				
				let activeAreas = config.areas.filter(a => a.logId === config.currentLogId);
				// console.log("active areas: ", activeAreas);
				if(activeAreas.length > 0) {
					drawActiveAreas(activeAreas, imageView);
					initAreaClick(imageView, activeAreas);
				} else {
					let inactiveAreas = config.areas.filter(a => a.logId !== config.currentLogId);
				    let inactiveLogIds = inactiveAreas.map(a => a.logId).filter((v, i, a) => a.indexOf(v) === i); //last filter to make logIds unique
				    inactiveLogIds.forEach(logId => {
				        let logIdAreas = config.areas.filter(a => a.logId === logId);
				        logIdAreas.forEach( (area, index) => drawArea(area, index, imageView));				    
				    })
				    initAreaClick(imageView, inactiveAreas);				    
				}

		    }
		    function setBrowserLocation(pi, pageNo) {
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
		    
		    function drawActiveAreas(activeAreas, imageView) {
		    	let areasOnCanvas = [];
				activeAreas.forEach((activeArea, index) => {				        
				    let rect = ImageView.CoordinateConversion.convertToOpenSeadragonObject(activeArea.coords);
		            rect = ImageView.CoordinateConversion.scaleToOpenSeadragon(rect, imageView.viewer, imageView.getOriginalImageSize());
		            areasOnCanvas.push(rect);
				    drawArea(activeArea, index, imageView);
	        	    let scrollPosition = window.sessionStorage.getItem("scrollPosition");
	        	    $(document).scrollTop(parseInt(scrollPosition));
	        	    window.sessionStorage.removeItem("scrollPosition");
			    })
	            let shadow = new ImageView.Tools.Filter.HighlightArea(imageView, 0.5, areasOnCanvas, true);
				shadow.start();
		    }
		    function initAreaClick(imageView, areas) {
	        	if(areas.length > 0) {
	        	    let $area = $(".page-area.active");
	        	    let url = areas[0].url.replace("/" + areas[0].logId, "");
	        	    $(imageView.viewer.canvas).css("cursor", "pointer");
	        	     imageView.viewer.addHandler("canvas-release", () => {
	        	        if(!imageView.dragging) {		        	            
		        	        let $target = $(event.target);
		        	        if($target.filter($area).length == 0) {
			        	    	window.location.href = url;
		        	        }
	        	        }
	        	    })
	        	    areas.forEach((area) => {
		        	    let $area = $(".page-area:not(.active)[id^=pageAreaFrame_"+area.logId+"_]");
			        	$area.on("click", () => {
			        	    if(!imageView.dragging) {		        	        
				        	    window.sessionStorage.setItem("scrollPosition", $(document).scrollTop());
				        	    window.location.href = area.url;
			        	    }
			        	});
			        });
	        	}
		    }
		    function drawArea(area, shapeIndex, imageView, clickToLeave) {
		        let rect = ImageView.CoordinateConversion.convertToOpenSeadragonObject(area.coords);
	            rect = ImageView.CoordinateConversion.scaleToOpenSeadragon(rect, imageView.viewer, imageView.getOriginalImageSize());
	        	let $area = $("#pageAreaFrame_" + area.logId + "_" + shapeIndex);
	        	let $label = $("#pageAreaLabel_" + area.logId + "_" + shapeIndex);
	            let overlay = new ImageView.Overlay({
	                rect: rect,
	                viewer: imageView.viewer,
	                rotateWithImage: true,
	                element: $area.get(0),
	                label: $label.get(0)
	            });
	        	overlay.draw();

	        	$area.hover(
	        		() => $label.addClass("hover"),
	        		() => $label.removeClass("hover")
	        	);

		    }
		    
		    
		    viewImage.observables.viewportUpdate.pipe(RxOp.first()).subscribe( () => {
       	 	    let originClean = viewImage.isOriginClean();
       	 	    if(originClean) {
       	    		drawPageAreas(pageAreaConfig, viewImage);
       	 	    }
       			highlightUGC();
       			enableGeoMapHighlighting();
		    })