/**
 * This Spec tests the viewer control Methods in viewImageJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ViewImage Controls Tests', function() {
    beforeEach( function() {
        jasmine.getFixtures().load( 'fullScreenTest.html' );
        var body = $("body");
        body.attr("id", "fullscreenTemplate");
    } );
    
    afterEach( function() {
        var body = $("body");
        body.removeAttr("id");
    } );
    
    describe( 'ViewImage Controls: fullscreenControlsFadeout', function() {
        it( ' is called on mouse move', function() {
            var config = {
                global: {},
                image: {}
            };
            viewImage.controls.init( config );
            spyOn( viewImage.controls, 'fullscreenControlsFadeout' );
            $( '#fullscreenTemplate' ).trigger( 'mousemove' );
            expect( viewImage.controls.fullscreenControlsFadeout ).toHaveBeenCalled();
        } );
    } );
} );
