/**
 * This Spec tests the helper methods in viewerJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ViewerJS Helper Tests', function() {
    describe( 'ViewerJS Helper: truncateString', function() {
        it( 'should take a string and a number to truncate the string to the given length.', function() {
            var string = 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.';
            var size = 25;
            var result = viewerJS.helper.truncateString( string, size );
            
            expect( result.length ).toBe( size + 3 );
        } );
    } );
    
    describe( 'ViewerJS Helper: getRemoteData', function() {
        it( 'should take an URL and return an object.', function( done ) {
            var url = 'http://libimages.princeton.edu/loris/pudl0001%2F4609321%2Fs42%2F00000001.jp2/info.json';
            
            viewerJS.helper.getRemoteData( url ).then( function( json ) {
                expect( json ).toEqual( jasmine.any( Object ) );
                console.log( 'TEST: viewerJS.helper.getRemoteData - ', json );
                done();
            } );
        } );
    } );
} );
