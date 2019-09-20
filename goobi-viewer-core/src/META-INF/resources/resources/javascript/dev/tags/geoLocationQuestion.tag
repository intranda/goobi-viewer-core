<geoLocationQuestion>
	

	
	<div if="{this.showInstructions()}" class="annotation_instruction">
		<label>Halten sie die Shift-Taste gedr&#x00FCckt und ziehen Sie im Bild einen Bereich mit der Maus auf.</label>
	</div>
	
	<div id="geoMap"></div>
	
	<div id="annotation_{index}" each="{anno, index in this.annotations}">
		
	</div>

<script>

on("mount", function() {
    this.initMap();
})

initMap() {
    var map = new L.Map('geoMap');
    var osm = new L.TileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      minZoom: 0,
      maxZoom: 20,
      attribution: 'Map data &copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors'
    });
 
    // define view
    map.setView(new L.LatLng(49.451993, 11.073397), 5);
    map.addLayer(osm);
}

showInstructions() {
    return !this.opts.item.isReviewMode() && this.question.targetSelector == Crowdsourcing.Question.Selector.RECTANGLE && this.question.annotations.length == 0;
}

</script>


</geoLocationQuestion>

