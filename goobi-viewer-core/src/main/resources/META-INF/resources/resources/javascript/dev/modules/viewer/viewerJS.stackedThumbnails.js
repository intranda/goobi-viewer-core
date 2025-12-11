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
 * Module which renders CSS featured stacked thumbnails in search list for multivolume
 * works.
 *
 * @version 3.2.0
 * @module viewerJS.stackedThumbnails
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
  "use strict";

  var _debug = false;
  var _imgWidth = null;
  var _imgHeight = null;
  var _defaults = {
    thumbs: ".stacked-thumbnail",
    thumbsBefore: ".stacked-thumbnail-before",
    thumbsAfter: ".stacked-thumbnail-after",
  };

  viewer.stackedThumbnails = {
    /**
     * Method to initialize the timematrix slider and the events which builds the
     * matrix and popovers.
     *
     * @method init
     * @param {Object} config An config object which overwrites the defaults.
     * @param {Object} config.thumbs All jQuery objects of the stacked thumbnails.
     * @param {String} config.thumbsBefore The classname of the stacked thumbnail
     * before element.
     * @param {String} config.thumbsAfter The classname of the stacked thumbnail after
     * element.
     */
    init: function (config) {
      if (_debug) {
        console.log("##############################");
        console.log("viewer.stackedThumbnails.init");
        console.log("##############################");
        console.log("viewer.stackedThumbnails.init: config - ");
        console.log(config);
      }

      $.extend(true, _defaults, config);

      // hide stacked thumbs
      //    $( _defaults.thumbs ).hide();
      //    $( _defaults.thumbs ).siblings().hide();

      // iterate through thumbnails and set width and height for image stack
      /*   $( _defaults.thumbs ).each( function() {
                _imgWidth = $( this ).outerWidth();
                _imgHeight = $( this ).outerHeight();
                
                $( this ).css( {
                    'margin-left': '-' + ( _imgWidth / 2 ) + 'px'
                } );
                
                $( this ).siblings().css( {
                    'width': _imgWidth,
                    'height': _imgHeight,
                    'margin-left': '-' + ( _imgWidth / 2 ) + 'px'
                } );
                
                // show stacked thumbs after building them
                $( this ).show();
                $( this ).siblings( _defaults.thumbsBefore ).fadeIn( 'slow', function() {
                    $( this ).siblings( _defaults.thumbsAfter ).fadeIn();
                } );
            } );*/

      // fade in thumb paper stack effect on scroll
      $.fn.isInViewport = function () {
        let elementTop = $(this).offset().top;
        let elementBottom = elementTop + $(this).outerHeight();

        let viewportTop = $(window).scrollTop();
        let viewportBottom = viewportTop + $(window).height();

        return elementBottom > viewportTop && elementTop < viewportBottom;
      };

      var debounce_timer;

      $(window).scroll(function () {
        if (debounce_timer) {
          window.clearTimeout(debounce_timer);
        }
        debounce_timer = window.setTimeout(function () {
          $(".stacked-thumbnail-after, .stacked-thumbnail-before").each(
            function (i) {
              if ($(this).isInViewport()) {
                $(this).addClass("-shown");
              }
            }
          );
        }, 100);
      });

      // triggers thumb paper stack effect on initial render or if the page has no scroll
      $(".stacked-thumbnail").each(function () {
        // If the image is already loaded
        if (this.complete) {
          $(this)
            .siblings(".stacked-thumbnail-before, .stacked-thumbnail-after")
            .each(function () {
              if ($(this).isInViewport()) {
                $(this).addClass("-shown");
              }
            });
        } else {
          // If the image is still loading
          $(this).on("load", function () {
            $(this)
              .siblings(".stacked-thumbnail-before, .stacked-thumbnail-after")
              .each(function () {
                if ($(this).isInViewport()) {
                  $(this).addClass("-shown");
                }
              });
          });
        }
      });
    },
  };

  return viewer;
})(viewerJS || {}, jQuery);
