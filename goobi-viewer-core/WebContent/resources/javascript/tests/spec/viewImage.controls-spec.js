/**
 * This Spec tests the viewer control Methods in viewImageJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ImageView Controls Tests', function() {
    beforeEach( function() {
        var body = $("body");
        body.attr("id", "fullscreenTemplate");
    } );
    
    afterEach( function() {
        var body = $("body");
        body.removeAttr("id");
    } );
    
    describe( 'ImageView Controls: fullscreenControlsFadeout', function() {
        it( ' is called on mouse move', function() {
            var config = {
                global: {},
                image: {}
            };
            var viewImage = new ImageView.Image(config);
            var controls = new ImageView.Controls( config, viewImage );
            spyOn( controls, 'fullscreenControlsFadeout' );
            $( '#fullscreenTemplate' ).trigger( 'mousemove' );
            expect( controls.fullscreenControlsFadeout ).toHaveBeenCalled();
        } );
    } );
} );
