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
 * @version 3.2.0
 * @module viewerJS.helper
 */
var viewerJS = (function(viewer) {
	'use strict';

	// default variables
	const _debug = false;

	const ignoredProperties = ["jsonSignature"];

	viewer.jsonValidator = {

		validate: function(obj, signature) {
			
			if(!signature && obj.hasOwnProperty("jsonSignature")) {
				signature = obj.jsonSignature;
			}
			
			// Check if the given object and signature are both objects
			if (typeof obj !== 'object' || typeof signature !== 'object' || obj === null || signature === null) {
				throw new Error('Both parameters must be objects.');
			}

			// Iterate through the signature properties
			for (let key in signature) {
				// Check if the property exists in the object
				if (signature.hasOwnProperty(key)) {
					// Check if the property type matches the signature
					if (obj[key] !== undefined && obj[key] !== null && typeof obj[key] !== signature[key]) {
						return false;
					}
				}
			}

			// Check for any extra properties in the object not defined in the signature
			for (let key in obj) {
				if(!ignoredProperties.includes(key)) {
					if (obj.hasOwnProperty(key) && !signature.hasOwnProperty(key)) {
						return false;
					}
				}
			}

			// If all checks pass, the object conforms to the signature
			return true;
		}

	}

	return viewer;

})(viewerJS || {}, jQuery);