/**
 * 
 */

describe( "ImageView.TileSourceResolver Tests ", function() {

    var iiifInfoURI = "https://viewer-demo01.intranda.com/viewer/rest/image/-/file:U002FU002FU002FoptU002FdigiversoU002FviewerU002Fcms_mediaU002Fiiif-test.png/info.json";
    var iiifImageURI = "https://viewer-demo01.intranda.com/viewer/rest/image/-/file:U002FU002FU002FoptU002FdigiversoU002FviewerU002Fcms_mediaU002Fiiif-test.png/full/max/0/default.jpg";
    var simpleImageURI = "https://viewer-demo01.intranda.com/viewer/resources/images/multivolume_thumbnail.jpg";
    var iiifInfoObject = {
            "@context": "http://iiif.io/api/image/2/context.json",
            "@id": "https://viewer-demo01.intranda.com:443/viewer/rest/image/-/file%3AU002FU002FU002FoptU002FdigiversoU002FviewerU002Fcms_mediaU002Fiiif-test.png",
            "protocol": "http://iiif.io/api/image",
            "width": 1000,
            "height": 1000,
            "profile": "http://iiif.io/api/image/2/level2.json"
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
            
            ImageView.TileSourceResolver.loadIfJsonURL( iiifInfoURI )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } )

        it( "fails for a iiif image url", function( done ) {
            
            ImageView.TileSourceResolver.loadIfJsonURL( iiifImageURI )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )
        
        it( "fails for an image url", function( done ) {
            
            ImageView.TileSourceResolver.loadIfJsonURL( simpleImageURI )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )
        
        it( "fails for a string other than a url", function( done ) {
            
            ImageView.TileSourceResolver.loadIfJsonURL( iiifInfoString )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )
        
        it( "fails for a json object", function( done ) {
            
            ImageView.TileSourceResolver.loadIfJsonURL( iiifInfoObject )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } )

    })
    

    
   describe( "Test resolveAsJson", function() {

        it( "returns a json object from image info url", function( done ) {
            
            ImageView.TileSourceResolver.resolveAsJson( iiifInfoURI, true )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } );
        it( "fails for a iiif image url", function( done ) {
            
            ImageView.TileSourceResolver.resolveAsJson( iiifImageURI, true )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } );
        it( "fails for an image url", function( done ) {
            
            ImageView.TileSourceResolver.resolveAsJson( simpleImageURI, true )
            .then( checkJson, checkError )
            .then( done.fail, done );
        } );
        it( "returns the inserted object if it already is a json object", function( done ) {
            
            ImageView.TileSourceResolver.resolveAsJson( iiifInfoObject, true )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } );
        it( "returns the resolved json object is it is a stringified object", function( done ) {
            
            ImageView.TileSourceResolver.resolveAsJson( iiifInfoString, true )
            .then( checkJson, checkError )
            .then( done, done.fail );
        } );
    } );
        
    describe( "Test isJsonURI ", function() {
        it( "returns true if object is an uri and represents a json object ", function() {
            expect(ImageView.TileSourceResolver.isJsonURI(iiifInfoURI)).toBe(true);
            expect(ImageView.TileSourceResolver.isJsonURI(iiifInfoURI + "/")).toBe(true);
            expect(ImageView.TileSourceResolver.isJsonURI(iiifInfoURI + "?a=asdas&b=las")).toBe(true);
            expect(ImageView.TileSourceResolver.isJsonURI(iiifImageURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isJsonURI(simpleImageURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isJsonURI(iiifInfoObject)).toBe(false);
            expect(ImageView.TileSourceResolver.isJsonURI(iiifInfoString)).toBe(false);
        } )
    } )
    
    describe( "Test isURI ", function() {
        it( "returns true if object is an uri and leads to an image ", function() {
            expect(ImageView.TileSourceResolver.isURI(iiifInfoURI)).toBe(true);
            expect(ImageView.TileSourceResolver.isURI(iiifImageURI)).toBe(true);
            expect(ImageView.TileSourceResolver.isURI(simpleImageURI)).toBe(true);
            expect(ImageView.TileSourceResolver.isURI(iiifInfoObject)).toBe(false);
            expect(ImageView.TileSourceResolver.isURI(iiifInfoString)).toBe(false);
        } )
    } )
    
    describe( "Test isStringifiedJson", function() {
        it("returns true of parameter is a stringified json object", function() {
            expect(ImageView.TileSourceResolver.isStringifiedJson(iiifInfoURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isStringifiedJson(iiifImageURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isStringifiedJson(simpleImageURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isStringifiedJson(iiifInfoObject)).toBe(false);
            expect(ImageView.TileSourceResolver.isStringifiedJson(iiifInfoString)).toBe(true);
        })
    })
    
        describe( "Test isJson", function() {
        it("returns true if parameter is a json object", function() {
            expect(ImageView.TileSourceResolver.isJson(iiifInfoURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isJson(iiifImageURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isJson(simpleImageURI)).toBe(false);
            expect(ImageView.TileSourceResolver.isJson(iiifInfoObject)).toBe(true);
            expect(ImageView.TileSourceResolver.isJson(iiifInfoString)).toBe(false);
        })
    })
} )
