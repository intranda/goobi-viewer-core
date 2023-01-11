<chronologyGraph>

<div>
  <canvas ref="chart"></canvas>
</div>

<script>

this.on( 'mount', function() {    	

	let chartElement = this.refs.chart;
	console.log("render chart in ", chartElement);
	
	this.chartConfig = {
			type: "bar",
			data: {
				labels: ['1700', '1800', '1900', '2000'],
				datasets: [
					{
						label: 'Artists born',
						data: [34, 36, 78, 8],
						borderWidth: 1
					},
					{
						label: 'Artists died',
						data: [31, 51, 12, 21],
						borderWidth: 2
					}
				]
			},
			options: {
				scales: {
					y: {
						beginAtZero: true
					}
				}
			}
			
	}
	
	this.chart = new Chart(chartElement, this.chartConfig);

})

</script>



</chronologyGraph>