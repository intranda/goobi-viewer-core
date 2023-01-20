<chronologyGraph>

<div ref="container" class="widget-chronology-slider__item chronology-slider">
  <canvas ref="chart"></canvas>
  <canvas ref="draw"></canvas>
</div>

<script>

this.on( 'mount', function() {    	

	let chartElement = this.refs.chart;
	this.yearList = Array.from(this.opts.datamap.keys()).map(y => parseInt(y));
	this.yearValues = Array.from(this.opts.datamap.values());
	this.startYear = parseInt(opts.startYear);
	this.endYear = parseInt(opts.endYear);
	this.minYear = this.yearList[0];
	this.maxYear = this.yearList[this.yearList.length - 1];
	this.valueInput = document.getElementById(opts.valueInput);
	this.updateFacet = document.getElementById(opts.updateFacet);
	this.loader = document.getElementById(opts.loader);	
	this.msg = opts.msg;
	this.rtl = $( this.refs.slider ).closest('[dir="rtl"]').length > 0;
	
	this.chartConfig = {
			type: "line",
			data: {
				labels: this.yearList,
				datasets: [
					{
						data: this.yearValues,
						borderWidth: 1
					}
				]
			},
			options: {
				elements: {
					point: {
						pointStyle: false,
					}
				},
				plugins: {					
					legend: {
				        display: false
				    },
				},
				scales: {
					y: {
						beginAtZero: true,
					},
					x: {
						type: "time",
						//min: this.yearList[0]-this.yearList[0]%100-1,
						time: {
							unit: "year",
							tooltipFormat: "yyyy",
							displayFormats: {
								"year" : "yyyy"
							},
							parser: s => {
								let date = new Date();
								date.setYear(parseInt(s));
								return date.getTime();
							}
						},
					    ticks: {
					    	maxTicksLimit: 5,
					    	maxRotation: 0,
					    }
					}
				}
			}
			
	}
	console.log("init chart with config ", this.chartConfig);
	this.chart = new Chart(chartElement, this.chartConfig);
	this.initDraw();

	if(this.startYear > this.yearList[0] || this.endYear < this.yearList[this.yearList.length-1]) {		
		this.drawInitialRange();
	}

})

drawInitialRange() {
	var points = this.chart.getDatasetMeta(0).data;
	if(points && points.length){		
		let startYearIndex = this.yearList.indexOf(this.startYear);
		let endYearIndex = this.yearList.indexOf(this.endYear);
		let x1 = points[startYearIndex].x;
		let x2 = points[endYearIndex].x;
		this.drawRect(x1, x2, this.refs.draw);
	}
}

initDraw() {
	let width = this.refs.chart.offsetWidth;
	let height = this.refs.chart.offsetHeight;
	this.refs.draw.style.width = width + "px";
	this.refs.draw.style.height = height + "px";
// 	this.refs.draw.style.opacity = 0.3;
// 	this.refs.draw.style.background = "orange";
	this.refs.draw.style.position = "absolute";
	this.refs.draw.style.top = 0;
	this.refs.container.style.position = "relative";
	
	let drawContext = this.refs.draw.getContext("2d");
// 	drawContext.lineWidth = 2;
// 	drawContext.strokeStyle = "black";
	let startPoint = undefined;
	let startYear = undefined;
	let endYear = undefined;
	let drawing = false;
	this.refs.draw.addEventListener("mousedown", e => {
		startYear = this.calculateYearFromEvent(e);
		endYear = undefined;
		startPoint = this.getPointFromEvent(e, this.refs.draw);
		drawing = true;
		drawContext.clearRect(0, 0, this.refs.draw.width, this.refs.draw.height);
	})
	this.refs.draw.addEventListener("mousemove", e => {
		if(drawing) {			
			let currPoint = this.getPointFromEvent(e, this.refs.draw);
			this.drawRect(startPoint.x, currPoint.x, this.refs.draw)
			
		}
	})
	this.refs.draw.addEventListener("mouseup", e => {
		if(drawing) {	
			endYear = this.calculateYearFromEvent(e);
			drawing = false;
			if(startYear && endYear) {
				this.setRange(startYear, endYear);
			}
		}
	})
}

drawRect(x1, x2, canvas) {
	let scaleX = canvas.width/canvas.getBoundingClientRect().width;
	
    let x1Scaled = x1*scaleX;
    let x2Scaled = x2*scaleX;
	let drawContext = canvas.getContext("2d");
	drawContext.clearRect(0,0, canvas.width, canvas.height);
	drawContext.beginPath();
	//drawContext.rect(startPoint.x, startPoint.y, width, height);
	drawContext.rect(x1Scaled, 0, x2Scaled-x1Scaled, canvas.height);
	drawContext.stroke();
}

setRange(startDate, endDate) {
	    // show loader
	    $( this.loader ).addClass( 'active' );
	    
	    // set query to hidden input
	    let value = '[' + startDate + ' TO ' + endDate + ']' ;
	    $( this.valueInput ).val(value);
	    // submit form
	    $( this.updateFacet ).click();
}
	
calculateYearFromEvent(e) {
	var activePoints = this.chart.getElementsAtEventForMode(e, 'nearest', { axis: "x" }, true);
    //console.log("points", activePoints);
    if(activePoints.length > 0) {			        	
    	let year = this.yearList[activePoints[0].index];
    	return year;
    }
}

getPointFromEvent(e, canvas) {
	let currX = e.clientX - canvas.getBoundingClientRect().left;
    let currY = e.clientY - canvas.getBoundingClientRect().top;
    return {x: currX, y: currY};
}

</script>



</chronologyGraph>