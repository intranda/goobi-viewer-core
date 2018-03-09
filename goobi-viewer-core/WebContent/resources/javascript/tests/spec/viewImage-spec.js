
describe("viewImage tests", function() {
	
	var config = null;
	
	beforeEach( function(){
		
		jasmine.getFixtures().load("viewImage.html");
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

	describe("Open image ", function() {
		it("opens an image from a static image url ", function(done) {
			
			config.image.tileSource = "http://www.intranda.com/wp-content/uploads/2014/01/banner_digitisation_small.jpg";
			
			 viewImage.init( config )
			    .then(function(osViewer) {
			       expect(osViewer.viewer).toExist();
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