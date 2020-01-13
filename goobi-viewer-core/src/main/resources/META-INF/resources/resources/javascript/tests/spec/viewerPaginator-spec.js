/**
 * This Spec tests the paginator methods in viewerJS.
 * 
 * @version 1.0
 * @author Marc Lettau-Poelchen
 * @copyright intranda GmbH 2017
 */
describe( 'ViewerJS Paginator Tests', function() {
    
	var config;
	var currentPage = 5;
    

        beforeEach(function() {
            $('<form id="paginatorForm"><nav><ul><li class="navigate_prev"><a onclick="setPage(currentPage-1)">&lt;&lt;</a></li><li class="navigate_first"><a onclick="setPage(1)">1</a></li><li><a onclick="setPage(2)">2</a></li><li><a onclick="setPage(3)">3</a></li><li><a onclick="setPage(4)">4</a></li><li><a onclick="setPage(5)">5</a></li><li><a onclick="setPage(6)">6</a></li><li><a onclick="setPage(7)">7</a></li><li><a onclick="setPage(8)">8</a></li><li><a onclick="setPage(9)">9</a></li><li class="navigate_last"><a onclick="setPage(10)">10</a></li><li class="navigate_next"><a onclick="setPage(currentPage+1)">&gt;&gt;</a></li></ul></nav> </form').appendTo('body');
            
            config = {
                    previous: ".navigate_prev a",
                    next: ".navigate_next a",
                    first: ".navigate_first a",
                    last: ".navigate_last a"
            }
            currentPage = 5;
            setPage(5);
            
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
            $('#paginatorForm').remove()
        })
        
        it( 'should start with 12 elements with only the 6th element active', function() {
            expect($("#paginatorForm li").length).toBe(12);
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:lt(5)").not.toHaveClass(activeClass);
            expect("#paginatorForm li:gt(5)").not.toHaveClass(activeClass);

        } );
    
        it( 'should should move one left on left arrow key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(4)").not.toHaveClass(activeClass);
            
            keyPress(37);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(4)").toHaveClass(activeClass);
                done();
            }, 500);
        } );
        
        it( 'should should move one right on right arrow key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(6)").not.toHaveClass(activeClass);
            
            keyPress(39);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(6)").toHaveClass(activeClass);
                done();
            }, 500);
        } );
        
        it( 'should should move to first element on double left key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(1)").not.toHaveClass(activeClass);
            
            keyPress(37);
            keyPress(37);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(1)").toHaveClass(activeClass);
                done();
            }, 500);
        } );
        
        it( 'should should move to last element on double right key press', function(done) {
            expect("#paginatorForm li:eq(5)").toHaveClass(activeClass);
            expect("#paginatorForm li:eq(10)").not.toHaveClass(activeClass);
            
            keyPress(39);
            keyPress(39);
            
            setTimeout(function() {
                expect("#paginatorForm li:eq(5)").not.toHaveClass(activeClass);
                expect("#paginatorForm li:eq(10)").toHaveClass(activeClass);
                done();
            }, 500);
        } );


        

        

} );

var activeClass = "numeric-paginator__active";

function setPage(no) {
    currentPage = Math.max(Math.min(10,no),1);
    $("#paginatorForm li").removeClass(activeClass);
    $("#paginatorForm li:eq("+currentPage+")").addClass(activeClass);
}

function keyPress(key) {
    var press = jQuery.Event("keyup");
    press.key = key;
    press.keyCode = key;
    $("body").trigger(press);
}

