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
 * Base-Module which initialize the global Content Management System (CMS) object.
 *
 * @version 3.2.0
 * @module cmsJS
 * @requires jQuery
 */
var cmsJS = (function () {
    'use strict';

    var _debug = false;
    var cms = {};

    /**
     * Method which initializes the CMS.
     *
     * @method init
     * @example
     *
     * <pre>
     * cmsJS.init();
     * </pre>
     */
    cms.init = function () {
        if (_debug) {
            console.log('##############################');
            console.log('cmsJS.init');
            console.log('##############################');
        }

        // AJAX Loader Eventlistener
        //It appears this is never used!
        const ajaxLoader = document.getElementById('AJAXLoader');
        if (ajaxLoader) {
            viewerJS.jsfAjax.begin.subscribe((data) => (ajaxLoader.style.display = 'block'));
            viewerJS.jsfAjax.complete.subscribe((data) => (ajaxLoader.style.display = 'none'));
        }
    };

    return cms;
})(jQuery);
