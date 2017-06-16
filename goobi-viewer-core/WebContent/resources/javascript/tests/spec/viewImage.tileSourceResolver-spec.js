/**
 * 
 */

describe( "ViewImage.TileSourceResolver Tests ", function() {

    var iiifInfoURI = "http://libimages.princeton.edu/loris/pudl0001%2F4609321%2Fs42%2F00000001.jp2/info.json";
    var iiifImageURI = "https://libimages.princeton.edu/loris/pudl0001%2F4609321%2Fs42%2F00000001.jp2/full/full/0/native.jpg";
    var simpleImageURI = "http://www.intranda.com/wp-content/uploads/2014/01/banner_digitisation_small.jpg";
    var iiifInfoObject = {
        "profile": "http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
        "scale_factors": [ 1, 2, 4, 8, 16, 32 ],
        "tile_height": 1024,
        "height": 7200,
        "width": 5233,
        "tile_width": 1024,
        "qualities": [ "native", "bitonal", "grey", "color" ],
        "formats": [ "jpg", "png", "gif" ],
        "@context": "http://library.stanford.edu/iiif/image-api/1.1/context.json",
        "@id": "https://libimages.princeton.edu/loris/pudl0001%2F4609321%2Fs42%2F00000001.jp2"
    };
    var iiifInfoString = JSON.stringify( iiifInfoObject );
    
    function checkJson( json ) {
        expect( json[ "@id" ].replace("https", "http") ).toEqual( iiifInfoObject[ "@id" ].replace("https", "http") );
        return Q.resolve( json );
    }
    function checkError( error ) {
        expect( error ).toEqual( jasmine.any( String ) );
        return Q.reject( error );
    }

    describe( "Test loadIfJsonURL ", function() {
    	
    	beforeEach(function() {
    		jasmine.DEFAULT_TIMEOUT_INTERVAL = 15000;
    	})
        
        it( "returns the json object behind the url", function( done ) {
            
            viewImage.tileSourceResolver.loadIfJsonURL( iiifInfoURI )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } )

        it( "fails for a iiif image url", function( done ) {
            
            viewImage.tileSourceResolver.loadIfJsonURL( iiifImageURI )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )
        
        it( "fails for an image url", function( done ) {
            
            viewImage.tileSourceResolver.loadIfJsonURL( simpleImageURI )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )
        
        it( "fails for a string other than a url", function( done ) {
            
            viewImage.tileSourceResolver.loadIfJsonURL( iiifInfoString )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )
        
        it( "fails for a json object", function( done ) {
            
            viewImage.tileSourceResolver.loadIfJsonURL( iiifInfoObject )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )

    })
    

    
   describe( "Test resolveAsJson", function() {

        it( "returns a json object from image info url", function( done ) {
            
            viewImage.tileSourceResolver.resolveAsJson( iiifInfoURI, true )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } );
        it( "fails for a iiif image url", function( done ) {
            
            viewImage.tileSourceResolver.resolveAsJson( iiifImageURI, true )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } );
        it( "fails for an image url", function( done ) {
            
            viewImage.tileSourceResolver.resolveAsJson( simpleImageURI, true )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } );
        it( "returns the inserted object if it already is a json object", function( done ) {
            
            viewImage.tileSourceResolver.resolveAsJson( iiifInfoObject, true )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } );
        it( "returns the resolved json object is it is a stringified object", function( done ) {
            
            viewImage.tileSourceResolver.resolveAsJson( iiifInfoString, true )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } );
    } );
        
    describe( "Test isJsonURI ", function() {
        it( "returns true if object is an uri and represents a json object ", function() {
            expect(viewImage.tileSourceResolver.isJsonURI(iiifInfoURI)).toBe(true);
            expect(viewImage.tileSourceResolver.isJsonURI(iiifInfoURI + "/")).toBe(true);
            expect(viewImage.tileSourceResolver.isJsonURI(iiifInfoURI + "?a=asdas&b=las")).toBe(true);
            expect(viewImage.tileSourceResolver.isJsonURI(iiifImageURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isJsonURI(simpleImageURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isJsonURI(iiifInfoObject)).toBe(false);
            expect(viewImage.tileSourceResolver.isJsonURI(iiifInfoString)).toBe(false);
        } )
    } )
    
    describe( "Test isURI ", function() {
        it( "returns true if object is an uri and leads to an image ", function() {
            expect(viewImage.tileSourceResolver.isURI(iiifInfoURI)).toBe(true);
            expect(viewImage.tileSourceResolver.isURI(iiifImageURI)).toBe(true);
            expect(viewImage.tileSourceResolver.isURI(simpleImageURI)).toBe(true);
            expect(viewImage.tileSourceResolver.isURI(iiifInfoObject)).toBe(false);
            expect(viewImage.tileSourceResolver.isURI(iiifInfoString)).toBe(false);
        } )
    } )
    
    describe( "Test isStringifiedJson", function() {
        it("returns true of parameter is a stringified json object", function() {
            expect(viewImage.tileSourceResolver.isStringifiedJson(iiifInfoURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isStringifiedJson(iiifImageURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isStringifiedJson(simpleImageURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isStringifiedJson(iiifInfoObject)).toBe(false);
            expect(viewImage.tileSourceResolver.isStringifiedJson(iiifInfoString)).toBe(true);
        })
    })
    
        describe( "Test isJson", function() {
        it("returns true if parameter is a json object", function() {
            expect(viewImage.tileSourceResolver.isJson(iiifInfoURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isJson(iiifImageURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isJson(simpleImageURI)).toBe(false);
            expect(viewImage.tileSourceResolver.isJson(iiifInfoObject)).toBe(true);
            expect(viewImage.tileSourceResolver.isJson(iiifInfoString)).toBe(false);
        })
    })
} )
