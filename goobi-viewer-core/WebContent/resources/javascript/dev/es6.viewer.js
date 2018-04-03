(function(){function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s}return e})()({1:[function(require,module,exports){
"use strict";

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

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
 * Module which includes mostly used helper functions.
 * 
 * @version 3.4.0
 * @requires jQuery
 */
var Helper = function () {
  function Helper() {
    _classCallCheck(this, Helper);
  }

  _createClass(Helper, [{
    key: "truncateString",

    /**
        * Method to truncate a string to a given length.
        * 
        * @method truncateString
        * @param {String} str The string to truncate.
        * @param {Number} size The number of characters after the string should be
        * croped.
        * @returns {String} The truncated string.
        * @example
        * 
        * <pre>
        * viewerJS.helper.truncateString( $( '.something' ).text(), 75 );
        * </pre>
        */
    value: function truncateString(str, size) {
      var strSize = parseInt(str.length);

      if (strSize > size) {
        return str.substring(0, size) + "...";
      } else {
        return str;
      }
    }
  }]);

  return Helper;
}();

},{}]},{},[1])
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIm5vZGVfbW9kdWxlcy9icm93c2VyLXBhY2svX3ByZWx1ZGUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9lczYvdmlld2VyL2hlbHBlci5qcyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQTs7Ozs7OztBQ0FBOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7OztJQXVCTSxNOzs7Ozs7OztBQUNMOzs7Ozs7Ozs7Ozs7OzttQ0FjZSxHLEVBQUssSSxFQUFNO0FBQ3pCLFVBQUksVUFBVSxTQUFTLElBQUksTUFBYixDQUFkOztBQUVBLFVBQUksVUFBVSxJQUFkLEVBQW9CO0FBQ25CLGVBQU8sSUFBSSxTQUFKLENBQWMsQ0FBZCxFQUFpQixJQUFqQixJQUF5QixLQUFoQztBQUNBLE9BRkQsTUFFTztBQUNOLGVBQU8sR0FBUDtBQUNBO0FBQ0QiLCJmaWxlIjoiZ2VuZXJhdGVkLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXNDb250ZW50IjpbIihmdW5jdGlvbigpe2Z1bmN0aW9uIGUodCxuLHIpe2Z1bmN0aW9uIHMobyx1KXtpZighbltvXSl7aWYoIXRbb10pe3ZhciBhPXR5cGVvZiByZXF1aXJlPT1cImZ1bmN0aW9uXCImJnJlcXVpcmU7aWYoIXUmJmEpcmV0dXJuIGEobywhMCk7aWYoaSlyZXR1cm4gaShvLCEwKTt2YXIgZj1uZXcgRXJyb3IoXCJDYW5ub3QgZmluZCBtb2R1bGUgJ1wiK28rXCInXCIpO3Rocm93IGYuY29kZT1cIk1PRFVMRV9OT1RfRk9VTkRcIixmfXZhciBsPW5bb109e2V4cG9ydHM6e319O3Rbb11bMF0uY2FsbChsLmV4cG9ydHMsZnVuY3Rpb24oZSl7dmFyIG49dFtvXVsxXVtlXTtyZXR1cm4gcyhuP246ZSl9LGwsbC5leHBvcnRzLGUsdCxuLHIpfXJldHVybiBuW29dLmV4cG9ydHN9dmFyIGk9dHlwZW9mIHJlcXVpcmU9PVwiZnVuY3Rpb25cIiYmcmVxdWlyZTtmb3IodmFyIG89MDtvPHIubGVuZ3RoO28rKylzKHJbb10pO3JldHVybiBzfXJldHVybiBlfSkoKSIsIi8qKlxuICogVGhpcyBmaWxlIGlzIHBhcnQgb2YgdGhlIEdvb2JpIHZpZXdlciAtIGEgY29udGVudCBwcmVzZW50YXRpb24gYW5kIG1hbmFnZW1lbnRcbiAqIGFwcGxpY2F0aW9uIGZvciBkaWdpdGl6ZWQgb2JqZWN0cy5cbiAqIFxuICogVmlzaXQgdGhlc2Ugd2Vic2l0ZXMgZm9yIG1vcmUgaW5mb3JtYXRpb24uIC0gaHR0cDovL3d3dy5pbnRyYW5kYS5jb20gLVxuICogaHR0cDovL2RpZ2l2ZXJzby5jb21cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGZyZWUgc29mdHdhcmU7IHlvdSBjYW4gcmVkaXN0cmlidXRlIGl0IGFuZC9vciBtb2RpZnkgaXQgdW5kZXIgdGhlIHRlcm1zXG4gKiBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYXMgcHVibGlzaGVkIGJ5IHRoZSBGcmVlIFNvZnR3YXJlIEZvdW5kYXRpb247IGVpdGhlclxuICogdmVyc2lvbiAyIG9mIHRoZSBMaWNlbnNlLCBvciAoYXQgeW91ciBvcHRpb24pIGFueSBsYXRlciB2ZXJzaW9uLlxuICogXG4gKiBUaGlzIHByb2dyYW0gaXMgZGlzdHJpYnV0ZWQgaW4gdGhlIGhvcGUgdGhhdCBpdCB3aWxsIGJlIHVzZWZ1bCwgYnV0IFdJVEhPVVQgQU5ZXG4gKiBXQVJSQU5UWTsgd2l0aG91dCBldmVuIHRoZSBpbXBsaWVkIHdhcnJhbnR5IG9mIE1FUkNIQU5UQUJJTElUWSBvciBGSVRORVNTIEZPUiBBXG4gKiBQQVJUSUNVTEFSIFBVUlBPU0UuIFNlZSB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgZm9yIG1vcmUgZGV0YWlscy5cbiAqIFxuICogWW91IHNob3VsZCBoYXZlIHJlY2VpdmVkIGEgY29weSBvZiB0aGUgR05VIEdlbmVyYWwgUHVibGljIExpY2Vuc2UgYWxvbmcgd2l0aCB0aGlzXG4gKiBwcm9ncmFtLiBJZiBub3QsIHNlZSA8aHR0cDovL3d3dy5nbnUub3JnL2xpY2Vuc2VzLz4uXG4gKiBcbiAqIE1vZHVsZSB3aGljaCBpbmNsdWRlcyBtb3N0bHkgdXNlZCBoZWxwZXIgZnVuY3Rpb25zLlxuICogXG4gKiBAdmVyc2lvbiAzLjQuMFxuICogQHJlcXVpcmVzIGpRdWVyeVxuICovXG5jbGFzcyBIZWxwZXIge1xuXHQvKipcbiAgICAgKiBNZXRob2QgdG8gdHJ1bmNhdGUgYSBzdHJpbmcgdG8gYSBnaXZlbiBsZW5ndGguXG4gICAgICogXG4gICAgICogQG1ldGhvZCB0cnVuY2F0ZVN0cmluZ1xuICAgICAqIEBwYXJhbSB7U3RyaW5nfSBzdHIgVGhlIHN0cmluZyB0byB0cnVuY2F0ZS5cbiAgICAgKiBAcGFyYW0ge051bWJlcn0gc2l6ZSBUaGUgbnVtYmVyIG9mIGNoYXJhY3RlcnMgYWZ0ZXIgdGhlIHN0cmluZyBzaG91bGQgYmVcbiAgICAgKiBjcm9wZWQuXG4gICAgICogQHJldHVybnMge1N0cmluZ30gVGhlIHRydW5jYXRlZCBzdHJpbmcuXG4gICAgICogQGV4YW1wbGVcbiAgICAgKiBcbiAgICAgKiA8cHJlPlxuICAgICAqIHZpZXdlckpTLmhlbHBlci50cnVuY2F0ZVN0cmluZyggJCggJy5zb21ldGhpbmcnICkudGV4dCgpLCA3NSApO1xuICAgICAqIDwvcHJlPlxuICAgICAqL1xuXHR0cnVuY2F0ZVN0cmluZyhzdHIsIHNpemUpIHtcblx0XHRsZXQgc3RyU2l6ZSA9IHBhcnNlSW50KHN0ci5sZW5ndGgpO1xuXG5cdFx0aWYgKHN0clNpemUgPiBzaXplKSB7XG5cdFx0XHRyZXR1cm4gc3RyLnN1YnN0cmluZygwLCBzaXplKSArIFwiLi4uXCI7XG5cdFx0fSBlbHNlIHtcblx0XHRcdHJldHVybiBzdHI7XG5cdFx0fVxuXHR9XG59Il19
