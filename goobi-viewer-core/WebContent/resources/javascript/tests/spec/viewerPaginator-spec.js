/**
 * This Spec tests the paginator methods in viewerJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ViewerJS Paginator Tests', function() {
    
    var config;
    
        beforeEach(function() {
            jasmine.getFixtures().load("paginatorTest.html");
            config = {
                    previous: ".navigate_prev a",
                    next: ".navigate_next a",
                    first: ".navigate_first a",
                    last: ".navigate_last a"
            }
            
            viewerJS.paginator.init(config);
            
            jasmine.addMatchers({
                toHaveClass: function() {
                    return {
                        compare: function(actual, expected) {
                            var ret = {};
                            ret.message = actual + " does not contain class " + expected;
                            var classNames = $(actual).attr("class");               
                            if(classNames) {
                                var classList = classNames.split(/\s+/); 
                                ret.pass = classList.includes(expected);
                            } else {
                                ret.pass = false;
                            }
                            return ret;
                        }
                    }
                }
            })
        });
        
        afterEach(function() {
            viewerJS.paginator.close();
        })
        
        fit( 'should start with 12 elements with only the 6th element active', function() {
            expect($("#paginatorForm li").length).toBe(12);
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:lt(5)").not.toHaveClass(activeClass);
            expect("#paginatorForm li:gt(5)").not.toHaveClass(activeClass);

        } );
    
        fit( 'should should move one left on left arrow key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(4)").not.toHaveClass(activeClass);
            
            keyPress(37);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(4)").toHaveClass(activeClass);
                done();
            }, 0);
        } );
        
        fit( 'should should move one right on right arrow key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(6)").not.toHaveClass(activeClass);
            
            keyPress(39);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(6)").toHaveClass(activeClass);
                done();
            }, 0);
        } );
        
        fit( 'should should move to first element on double left key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(1)").not.toHaveClass(activeClass);
            
            keyPress(37);
            keyPress(37);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(1)").toHaveClass(activeClass);
                done();
            }, 0);
        } );
        
        fit( 'should should move to last element on double right key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(10)").not.toHaveClass(activeClass);
            
            keyPress(39);
            keyPress(39);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(10)").toHaveClass(activeClass);
                done();
            }, 0);
        } );


        
        function keyPress(key) {
            console.log("press key " + key);
            var press = jQuery.Event("keyup");
            press.key = key;
            press.keyCode = key;
            $("body").trigger(press);
          }
        
} );
