(function(){function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s}return e})()({1:[function(require,module,exports){
"use strict";

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var Helper = function () {
  function Helper() {
    _classCallCheck(this, Helper);
  }

  _createClass(Helper, [{
    key: "truncateString",
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
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIm5vZGVfbW9kdWxlcy9icm93c2VyLXBhY2svX3ByZWx1ZGUuanMiLCJXZWJDb250ZW50L3Jlc291cmNlcy9qYXZhc2NyaXB0L2Rldi9lczYvdmlld2VyL2hlbHBlci5qcyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQTs7Ozs7OztJQ0FNLE07Ozs7Ozs7bUNBQ1csRyxFQUFLLEksRUFBTTtBQUN4QixVQUFJLFVBQVUsU0FBUyxJQUFJLE1BQWIsQ0FBZDs7QUFFQSxVQUFJLFVBQVUsSUFBZCxFQUFvQjtBQUNsQixlQUFPLElBQUksU0FBSixDQUFjLENBQWQsRUFBaUIsSUFBakIsSUFBeUIsS0FBaEM7QUFDRCxPQUZELE1BRU87QUFDTCxlQUFPLEdBQVA7QUFDRDtBQUNGIiwiZmlsZSI6ImdlbmVyYXRlZC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzQ29udGVudCI6WyIoZnVuY3Rpb24oKXtmdW5jdGlvbiBlKHQsbixyKXtmdW5jdGlvbiBzKG8sdSl7aWYoIW5bb10pe2lmKCF0W29dKXt2YXIgYT10eXBlb2YgcmVxdWlyZT09XCJmdW5jdGlvblwiJiZyZXF1aXJlO2lmKCF1JiZhKXJldHVybiBhKG8sITApO2lmKGkpcmV0dXJuIGkobywhMCk7dmFyIGY9bmV3IEVycm9yKFwiQ2Fubm90IGZpbmQgbW9kdWxlICdcIitvK1wiJ1wiKTt0aHJvdyBmLmNvZGU9XCJNT0RVTEVfTk9UX0ZPVU5EXCIsZn12YXIgbD1uW29dPXtleHBvcnRzOnt9fTt0W29dWzBdLmNhbGwobC5leHBvcnRzLGZ1bmN0aW9uKGUpe3ZhciBuPXRbb11bMV1bZV07cmV0dXJuIHMobj9uOmUpfSxsLGwuZXhwb3J0cyxlLHQsbixyKX1yZXR1cm4gbltvXS5leHBvcnRzfXZhciBpPXR5cGVvZiByZXF1aXJlPT1cImZ1bmN0aW9uXCImJnJlcXVpcmU7Zm9yKHZhciBvPTA7bzxyLmxlbmd0aDtvKyspcyhyW29dKTtyZXR1cm4gc31yZXR1cm4gZX0pKCkiLCJjbGFzcyBIZWxwZXIge1xuICB0cnVuY2F0ZVN0cmluZyhzdHIsIHNpemUpIHtcbiAgICBsZXQgc3RyU2l6ZSA9IHBhcnNlSW50KHN0ci5sZW5ndGgpO1xuXG4gICAgaWYgKHN0clNpemUgPiBzaXplKSB7XG4gICAgICByZXR1cm4gc3RyLnN1YnN0cmluZygwLCBzaXplKSArIFwiLi4uXCI7XG4gICAgfSBlbHNlIHtcbiAgICAgIHJldHVybiBzdHI7XG4gICAgfVxuICB9XG59XG4iXX0=
