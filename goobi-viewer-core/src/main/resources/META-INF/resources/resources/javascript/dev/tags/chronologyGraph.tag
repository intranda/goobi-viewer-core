<chronologyGraph>

	<div class="widget-chronology-slider__item chronology-slider" if="{this.yearList.length > 0}">
		<div class="chronology-slider__container" ref="container">
			<canvas class="chronology-slider__chart" ref="chart"></canvas>
			<canvas class="chronology-slider__draw" ref="draw"></canvas>
		</div>
		<div class="chronology-slider__input-wrapper">
			<input onchange="{setStartYear}" data-input='number' class="form-control chronology-slider__input-start" ref="input_start" value="{startYear}"></input>
			<div class="chronology-slider__between-year-symbol">-</div>
			<input onchange="{setEndYear}" data-input='number' class="form-control chronology-slider__input-end" ref="input_end" value="{endYear}"></input>
			<button class="btn btn--full chronology-slider__ok-button" data-trigger="triggerFacettingGraph" onclick="{setRange}">{msg.ok}</button>
		</div>
	</div>
	<div hidden ref="line" class="chronology-slider__graph-line"></div>
	<div hidden ref="area" class="chronology-slider__graph-area"></div>
	<div hidden ref="range" class="chronology-slider__graph-range"></div>
	<script>
	
	
		this.yearList = [1];
		this.msg = {};
		this.on( 'mount', function() {   
			
			
			this.lineColor = window.getComputedStyle(this.refs?.line)?.color;
			this.areaColor = window.getComputedStyle(this.refs?.area)?.color;
			this.rangeBorderColor = window.getComputedStyle(this.refs?.range)?.color;
			this.rangeFillColor = window.getComputedStyle(this.refs?.range)?.backgroundColor;
			this.rangeOpacity = window.getComputedStyle(this.refs?.range)?.opacity;
			
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
								borderWidth: 1,
								borderColor: this.lineColor,
								backgroundColor: this.areaColor,
								fill: "origin",
							}
						]
					},
					options: {
						// events: ['mousemove', 'mouseout', 'click', 'touchstart', 'touchmove'],
						elements: {
							point: {
								pointStyle: false,
							}
						},
						plugins: {			 		
							legend: {
						        display: false
						    },
							tooltip: {
								  enabled: this.opts.showTooltip?.toLowerCase() == "true",
							      mode: 'index',
							      intersect: false,
							      displayColors: false,
							      
							      callbacks: {
							    	  label: item => item.raw + " " + this.msg.hits
// 							    	  label: () => "",
// 							    	  title: (item) => {
// 							    		  let year = item[0].label;
// 							    		  let value = item[0].raw;
// 							    		  return year + " : " + value + " " + this.msg.hits;
// 							    	  }
							      }
							},
						},
						scales: {
							y: {
								beginAtZero: true,
								display: false,
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
			if(this.refs.chart) {				
				console.log("init chart with config ", this.chartConfig);
				this.chart = new Chart(chartElement, this.chartConfig);
				this.initDraw();
			
				if(this.startYear > this.yearList[0] || this.endYear < this.yearList[this.yearList.length-1]) {		
					this.drawInitialRange();
				}
				this.update();
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
			this.refs.draw.style.position = "absolute";
			this.refs.draw.style.top = 0;
			this.refs.container.style.position = "relative";

			let startPoint = undefined;
 			let initialYear = undefined;
			let drawing = false;
			
			this.refs.draw.addEventListener("mousedown", e => {
				initialYear = this.calculateYearFromEvent(e);
				this.startYear = initialYear;
				this.endYear = initialYear;
				startPoint = this.getPointFromEvent(e, this.refs.draw);
				drawing = true;
				this.refs.draw.getContext("2d").clearRect(0, 0, this.refs.draw.width, this.refs.draw.height);
				this.update();
			})
			this.refs.draw.addEventListener("mousemove", e => {
				if(drawing) {			
					let year = this.calculateYearFromEvent(e);
					if(year < initialYear) {
						this.endYear = initialYear;
						this.startYear = year;
					} else {
						this.endYear = year;
						this.startYear = initialYear;
					}
					this.startYear = Math.min(year, this.startYear);
					this.endYear = Math.max(year, this.endYear);
					let currPoint = this.getPointFromEvent(e, this.refs.draw);
					this.drawRect(startPoint.x, currPoint.x, this.refs.draw);
					this.update();
				} else {
					let event = new MouseEvent("mousemove", {
						bubbles: false,
						target: e.target,
						clientX: e.clientX,
						clientY: e.clientY
					});
					this.refs.chart.dispatchEvent(event);			
				}
			})
			this.refs.draw.addEventListener("mouseup", e => {
				if(drawing) {	
					drawing = false;
					if(this.startYear && this.endYear) {
						this.setRange();
					}
					this.update();
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
			drawContext.rect(x1Scaled, 1, x2Scaled-x1Scaled, canvas.height-1);
			drawContext.globalAlpha = 1;
			drawContext.strokeStyle = this.rangeBorderColor;
			drawContext.stroke();
 			drawContext.globalAlpha = this.rangeOpacity;
			drawContext.fillStyle = this.rangeFillColor;
			drawContext.fill();
		}
		
		setStartYear(e) {
			e.preventUpdate = true;
			let year = parseInt(e.target.value);
			this.startYear = Math.min.apply(Math, this.yearList.filter((x) => x >= year));
			this.fixDateOrder();
		}
		
		setEndYear(e) {
			e.preventUpdate = true;
			let year = parseInt(e.target.value);
			this.endYear = Math.max.apply(Math, this.yearList.filter((x) => x <= year));
			this.fixDateOrder();
		}
		
		fixDateOrder() {
			if(this.startYear > this.endYear) {
				let temp = this.startYear;
				this.startYear = this.endYear;
				this.endYear = temp;
			}
		}
		
		setRange() {
			    // show loader
			    $( this.loader ).addClass( 'active' );
			    
			    // set query to hidden input
			    let value = '[' + this.startYear + ' TO ' + this.endYear + ']' ;
			    $( this.valueInput ).val(value);
			    // submit form
			    $( this.updateFacet ).click();
		}
			
		calculateYearFromEvent(e) {
			var activePoints = this.chart.getElementsAtEventForMode(e, 'nearest', { axis: "x" }, true);
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
		
		// TRIGGER OK CHRONOLOGY GRAPH BUTTON ON ENTER IN INPUTS
	  this.on('update', function(){
		$(".chronology-slider__input-start, .chronology-slider__input-end").keyup(function(event) {
		    if (event.keyCode === 13) {
		        $('[data-trigger="triggerFacettingGraph"]').click();
		    }
		});
	  })

	  this.on('mount', function(){
		$(".chronology-slider__input-start, .chronology-slider__input-end").keyup(function(event) {
		    if (event.keyCode === 13) {
		        $('[data-trigger="triggerFacettingGraph"]').click();
		    }
		});
	  })

		

		
	
	</script>

</chronologyGraph>