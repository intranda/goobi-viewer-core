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
 *
 * @description Contains functions for increased accessibility
 * @version 3.2.0
 * @module viewerJS.accessibility
 */
var viewerJS = (function (viewer) {
  "use strict";

  // Default variables
  var _debug = false;

  viewer.accessibility = {
    // Detect if a user uses the tab key to navigate
    // And add/remove a class to the body accordingly
    // The class helps adding styles specific to keyboard navigation
    detectKeyboardUsage: function () {
      // Keyboard is used
      document.body.addEventListener("keydown", function (e) {
        // Check if tab key was pressed
        if (e.keyCode === 9 || e.key == "Tab") {
          document.body.classList.add("using-keyboard");
        }
      });
      // Mouse is used
      document.body.addEventListener("mousedown", function () {
        document.body.classList.remove("using-keyboard");
      });
    },

    // Jump to footer if link or button in footer is focused with keyboard (tab)
    jumpToFooter: function () {
      if ($("#pageFooter").length) {
        $(document).on("keydown", function (e) {
          if (e.which == 9) {
            setTimeout(function () {
              if (
                $("#pageFooter a").is(":focus") ||
                $("#pageFooter button").is(":focus")
              ) {
                window.scrollTo(0, document.body.scrollHeight);
              }
            }, 20);
          }
        });
      }
    },

    // ###########################
    // WHAT: Make certain elements behave like buttons
    // Condition: Elements must have a role="button" attribute
    // WHY: Sometimes click events are assigned to non interactive elements (like <span>) or <a> tags should actually be marked up as buttons
    // if this markup cannot be changed, these fns create the same ux as a native <button> element when using keyboard navigation or assistive techonlogy

    // Find elements (role="button")
    // Assign an event listener + fire triggerClick()
    findPseudoBtns: function () {
      const interactiveEls = document.querySelectorAll('[role="button"]');
      interactiveEls.forEach((el) =>
        el.addEventListener("keydown", this.triggerClick)
      );
    },

    // Trigger a click when pressing the space bar or Enter on a Pseudo button
    triggerClick: function (e) {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        this.click();
      }
    },

    // ###########################
    // WHAT: Show/hide "skip to sidebar" link depending on sidebar content
    // CONDITION: Only display if #sidebarGroup contains any child elements or text
    // WHY: Avoid rendering a useless skip link if sidebar is empty
    checkSidebar: function () {
      const sidebar = document.getElementById("sidebarGroup");
      const skipLink = document.getElementById("skip-to-sidebar");

      if (!sidebar || !skipLink) return;

      if (
        sidebar.children?.length > 0 ||
        sidebar.textContent?.trim()?.length > 0
      )
        skipLink.style.display = "";
      else skipLink.style.display = "none";
    },

    // ###########################

    init: function () {
      if (_debug) console.log("init `viewerJS.accessibility`");
      this.detectKeyboardUsage();
      this.jumpToFooter();
      this.findPseudoBtns();
      this.checkSidebar();
    },
  };

  return viewer;
})(viewerJS);
