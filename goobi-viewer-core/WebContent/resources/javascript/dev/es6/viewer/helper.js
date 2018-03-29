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
class Helper {
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
	truncateString(str, size) {
		let strSize = parseInt(str.length);

		if (strSize > size) {
			return str.substring(0, size) + "...";
		} else {
			return str;
		}
	}
}