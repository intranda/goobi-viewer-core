(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
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
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIm5vZGVfbW9kdWxlcy9icm93c2VyLXBhY2svX3ByZWx1ZGUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9lczYvdmlld2VyL2hlbHBlci5qcyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQTs7Ozs7OztBQ0FBOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7OztJQXVCTSxNOzs7Ozs7OztBQUNMOzs7Ozs7Ozs7Ozs7OzttQ0FjZSxHLEVBQUssSSxFQUFNO0FBQ3pCLFVBQUksVUFBVSxTQUFTLElBQUksTUFBYixDQUFkOztBQUVBLFVBQUksVUFBVSxJQUFkLEVBQW9CO0FBQ25CLGVBQU8sSUFBSSxTQUFKLENBQWMsQ0FBZCxFQUFpQixJQUFqQixJQUF5QixLQUFoQztBQUNBLE9BRkQsTUFFTztBQUNOLGVBQU8sR0FBUDtBQUNBO0FBQ0QiLCJmaWxlIjoiZ2VuZXJhdGVkLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXNDb250ZW50IjpbIihmdW5jdGlvbigpe2Z1bmN0aW9uIHIoZSxuLHQpe2Z1bmN0aW9uIG8oaSxmKXtpZighbltpXSl7aWYoIWVbaV0pe3ZhciBjPVwiZnVuY3Rpb25cIj09dHlwZW9mIHJlcXVpcmUmJnJlcXVpcmU7aWYoIWYmJmMpcmV0dXJuIGMoaSwhMCk7aWYodSlyZXR1cm4gdShpLCEwKTt2YXIgYT1uZXcgRXJyb3IoXCJDYW5ub3QgZmluZCBtb2R1bGUgJ1wiK2krXCInXCIpO3Rocm93IGEuY29kZT1cIk1PRFVMRV9OT1RfRk9VTkRcIixhfXZhciBwPW5baV09e2V4cG9ydHM6e319O2VbaV1bMF0uY2FsbChwLmV4cG9ydHMsZnVuY3Rpb24ocil7dmFyIG49ZVtpXVsxXVtyXTtyZXR1cm4gbyhufHxyKX0scCxwLmV4cG9ydHMscixlLG4sdCl9cmV0dXJuIG5baV0uZXhwb3J0c31mb3IodmFyIHU9XCJmdW5jdGlvblwiPT10eXBlb2YgcmVxdWlyZSYmcmVxdWlyZSxpPTA7aTx0Lmxlbmd0aDtpKyspbyh0W2ldKTtyZXR1cm4gb31yZXR1cm4gcn0pKCkiLCIvKipcbiAqIFRoaXMgZmlsZSBpcyBwYXJ0IG9mIHRoZSBHb29iaSB2aWV3ZXIgLSBhIGNvbnRlbnQgcHJlc2VudGF0aW9uIGFuZCBtYW5hZ2VtZW50XG4gKiBhcHBsaWNhdGlvbiBmb3IgZGlnaXRpemVkIG9iamVjdHMuXG4gKiBcbiAqIFZpc2l0IHRoZXNlIHdlYnNpdGVzIGZvciBtb3JlIGluZm9ybWF0aW9uLiAtIGh0dHA6Ly93d3cuaW50cmFuZGEuY29tIC1cbiAqIGh0dHA6Ly9kaWdpdmVyc28uY29tXG4gKiBcbiAqIFRoaXMgcHJvZ3JhbSBpcyBmcmVlIHNvZnR3YXJlOyB5b3UgY2FuIHJlZGlzdHJpYnV0ZSBpdCBhbmQvb3IgbW9kaWZ5IGl0IHVuZGVyIHRoZSB0ZXJtc1xuICogb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFzIHB1Ymxpc2hlZCBieSB0aGUgRnJlZSBTb2Z0d2FyZSBGb3VuZGF0aW9uOyBlaXRoZXJcbiAqIHZlcnNpb24gMiBvZiB0aGUgTGljZW5zZSwgb3IgKGF0IHlvdXIgb3B0aW9uKSBhbnkgbGF0ZXIgdmVyc2lvbi5cbiAqIFxuICogVGhpcyBwcm9ncmFtIGlzIGRpc3RyaWJ1dGVkIGluIHRoZSBob3BlIHRoYXQgaXQgd2lsbCBiZSB1c2VmdWwsIGJ1dCBXSVRIT1VUIEFOWVxuICogV0FSUkFOVFk7IHdpdGhvdXQgZXZlbiB0aGUgaW1wbGllZCB3YXJyYW50eSBvZiBNRVJDSEFOVEFCSUxJVFkgb3IgRklUTkVTUyBGT1IgQVxuICogUEFSVElDVUxBUiBQVVJQT1NFLiBTZWUgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGZvciBtb3JlIGRldGFpbHMuXG4gKiBcbiAqIFlvdSBzaG91bGQgaGF2ZSByZWNlaXZlZCBhIGNvcHkgb2YgdGhlIEdOVSBHZW5lcmFsIFB1YmxpYyBMaWNlbnNlIGFsb25nIHdpdGggdGhpc1xuICogcHJvZ3JhbS4gSWYgbm90LCBzZWUgPGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy8+LlxuICogXG4gKiBNb2R1bGUgd2hpY2ggaW5jbHVkZXMgbW9zdGx5IHVzZWQgaGVscGVyIGZ1bmN0aW9ucy5cbiAqIFxuICogQHZlcnNpb24gMy40LjBcbiAqIEByZXF1aXJlcyBqUXVlcnlcbiAqL1xuY2xhc3MgSGVscGVyIHtcblx0LyoqXG4gICAgICogTWV0aG9kIHRvIHRydW5jYXRlIGEgc3RyaW5nIHRvIGEgZ2l2ZW4gbGVuZ3RoLlxuICAgICAqIFxuICAgICAqIEBtZXRob2QgdHJ1bmNhdGVTdHJpbmdcbiAgICAgKiBAcGFyYW0ge1N0cmluZ30gc3RyIFRoZSBzdHJpbmcgdG8gdHJ1bmNhdGUuXG4gICAgICogQHBhcmFtIHtOdW1iZXJ9IHNpemUgVGhlIG51bWJlciBvZiBjaGFyYWN0ZXJzIGFmdGVyIHRoZSBzdHJpbmcgc2hvdWxkIGJlXG4gICAgICogY3JvcGVkLlxuICAgICAqIEByZXR1cm5zIHtTdHJpbmd9IFRoZSB0cnVuY2F0ZWQgc3RyaW5nLlxuICAgICAqIEBleGFtcGxlXG4gICAgICogXG4gICAgICogPHByZT5cbiAgICAgKiB2aWV3ZXJKUy5oZWxwZXIudHJ1bmNhdGVTdHJpbmcoICQoICcuc29tZXRoaW5nJyApLnRleHQoKSwgNzUgKTtcbiAgICAgKiA8L3ByZT5cbiAgICAgKi9cblx0dHJ1bmNhdGVTdHJpbmcoc3RyLCBzaXplKSB7XG5cdFx0bGV0IHN0clNpemUgPSBwYXJzZUludChzdHIubGVuZ3RoKTtcblxuXHRcdGlmIChzdHJTaXplID4gc2l6ZSkge1xuXHRcdFx0cmV0dXJuIHN0ci5zdWJzdHJpbmcoMCwgc2l6ZSkgKyBcIi4uLlwiO1xuXHRcdH0gZWxzZSB7XG5cdFx0XHRyZXR1cm4gc3RyO1xuXHRcdH1cblx0fVxufSJdfQ==
