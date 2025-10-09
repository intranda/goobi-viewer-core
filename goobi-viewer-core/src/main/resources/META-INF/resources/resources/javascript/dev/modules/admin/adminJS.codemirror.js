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
 * @version 24.05
 * @module adminJS.codemirror
 * @description Module to initialize texta area fields with codemirror
 */

adminJS.codemirror = function(element, mode, readonly, config) {

	const baseConfig = {
		lineNumbers: true,
		mode: mode,
		theme: "default",
		autofocus: false,
		indentUnit: 2,
		tabSize: 2,
		styleActiveLine: true,
		indentWithTabs: true,
		extraKeys: {
			"F11": function(cm) {
				cm.setOption("fullScreen", !cm.getOption("fullScreen"));
			},
			"Esc": function(cm) {
				if (cm.getOption("fullScreen")) {
					cm.setOption("fullScreen", false);
				}
			},
			"Ctrl-D": function(cm) {
				cm.setOption("theme", cm.getOption("theme") == "default" ? "dracula" : "default");
			},
			"Ctrl-S": (cm) => {
				// if ( _debug ) {
				console.log('manually saved with key combo');
				// }
				if (!readonly) {
					document.querySelector('[data-cm="save"]').click();
				}
			},
			"Ctrl-E": "findPersistent",
		}
	}

	const cmConfig = $.extend(true, {}, baseConfig, config ? config : {});

	const cm = CodeMirror.fromTextArea(element, cmConfig);
	
	return {
		element: element,
		mode: cmConfig.mode,
		codemirror: cm
	}

}