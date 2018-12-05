                
/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 * 
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Module to handle collapsible sidebar widgets
 * 
 * @version 3.2.0
 * @module viewerJS.collapse
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
        
    viewer.initWidgetCollapse = function() {

                    $(".widget-collapse").on("click", function() {
                    var $this = $(this);
                    var expand = $this.hasClass("expand");
                    var target = $this.data("collapse");
                    var $target = $(target);
//                    console.log("Click on collapse handler", " expand ", expand, " target ", $target);
                    if(expand) {
                        $target.slideDown();
                    } else {
                        $target.slideUp();
                    }
                    $this.siblings(".widget-collapse").show();
                    $this.hide();
                })
    }
    
    return viewer;
    
})( viewerJS || {}, jQuery );
                
                
                