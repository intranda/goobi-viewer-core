
describe("imageView tests", function() {
	
	var config = null;
	
	beforeEach( function(){

		$('<span id="test"><style> #map { width: 800px; }</style><form id="tagListForm"><div id="map"></div></form></span>').appendTo('body');

		config = {
				    global: {
				    	divId: "map",
				        footerHeight: 0,
				        adaptContainerHeight: true,
				        persistZoom: false,
			            persistRotation: false,
			            persistenceId: "",
				    },
				    image: {
				        mimeType: "image/jpg",
					    tileSource : 'some/image/url',
				        baseFooterUrl : "",
				        highlightCoords: [{
				                name: "searchHighlighting",
				                coordinates: [],
				                displayTooltip: false
				            }, 
				            {
				                name: "ugc",
				                coordinates: [],
				                displayTooltip: true
				            }
				        ]				   
				    }
				};
	});

	afterEach(function(){
		$('#test').remove()
	});

	describe("Open image ", function() {
		it("opens an image from a static image url ", function(done) {

			config.image.tileSource = "http://www.intranda.com/wp-content/uploads/2014/01/banner_digitisation_small.jpg";

			 var viewImage = new ImageView.Image( config )
			 viewImage.load()
			    .then(function(osViewer) {
			       // expect(osViewer.viewer).toExist();
			       expect(osViewer.viewer.viewport._contentSize.x).toBeGreaterThan(0);
			       done();
			    })
			    .catch(function(error){
				console.log(error);
			        done.fail("Faile loading image: " + error.message);
			    })
		})
	})
	
})
