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
 * Module to render NER-Popovers in fulltext pages.
 * 
 * @module viewerJS.fulltextActions
 * @requires jQuery
 * 
 */
var viewerJS = (function(viewer) {
	'use strict';

	// define variables
	var _debug = false;
	var _defaults = {

	};

	viewer.fulltextActions = {
		/**
		 * Method which initializes the fulltext popovers for entity terms
		 * 
		 * @method init
		 * viewerJS.fulltextActions.init();
		 */
		init: function(config) {
			if (_debug) {
				console.log('##############################');
				console.log('viewer.fulltextActions.init');
				console.log('##############################');
			}

			$.extend(true, _defaults, config);

			_initFulltextPopovers();
		}
	};

	function _initFulltextPopovers() {

		// DISABLE ALL BUTTONS WITH NO URI
		$('[data-entity-type]:not([data-entity-authority-data-uri])').attr("disabled", true);

		// ITERATE THROUGH THE ENTITIES AND ENABLE BUTTON FUNCTIONALITY
		$('[data-entity-authority-data-uri]').each(function(normdataConfig) {

			// MSG KEYS
			var actionsPopoverHeadingMsg = _defaults.msg.fulltextPopoverActionsTermMsg;
			var authorityDataMsg = _defaults.msg.fulltextPopoverAuthorityDataMsg;
			var triggerSearchMsg = _defaults.msg.fulltextPopoverTriggerSearchMsg;

			var thisEntityElement = $(this);
			if (_debug) {
				console.log(thisEntityElement);
			}
			var searchLink = $(this).data("entity-authority-data-search");
			if (_debug) {
				console.log(searchLink);
			}
			var authorityDataUri = $(this).data("entity-authority-data-uri");
			if (_debug) {
				console.log(authorityDataUri);
			}

			// prepare base path for icons
			var iconBasePath = '';
			if (_defaults.normdataConfig && _defaults.normdataConfig.path) {
				iconBasePath = _defaults.normdataConfig.path;
				if (iconBasePath.slice(-1) !== '/') {
					iconBasePath += '/';
				}
			}

			// THE POPOVER HTML ELEMENT
			var popOverActionsElement = `<div class="hidden entity-popover-element">
	  			<div class="popover-heading">${actionsPopoverHeadingMsg}</div>
	  			
	  			<div class="popover-body"> 
	  				<div class="d-flex flex-column">
	  						<button class="view-fulltext__popover-button" type="button" data-remotecontent="${authorityDataUri}">
	  						<span class="icon-wrapper view-fulltext__popover-icon" aria-hidden="true">
	  							<svg class="icon" focusable="false"><use href="${iconBasePath}resources/icons/outline/list-details.svg#icon"></use></svg>
	  						</span><span>${authorityDataMsg}</span>
	  					</button>
	  					<a class="view-fulltext__popover-button" href="${searchLink}">
	  						<span class="icon-wrapper view-fulltext__popover-icon" aria-hidden="true">
	  							<svg class="icon" focusable="false"><use href="${iconBasePath}resources/icons/outline/search.svg#icon"></use></svg>
	  						</span><span>${triggerSearchMsg}</span></a>
	  				</div>
	  			</div>
	  		</div>`

			// INSERT HIDDEN POPOVER CONTENT AFTER ENTITY TERM
			$(this).after(popOverActionsElement);

			// CREATE EMPTY/PLACEHOLDER HTML ELEMENT FOR EACH ENTITY TERM TO ATTACH THE POPOVER TO THIS ELEMENT
			$(this).after('<span data-placeholder="forPopover"></span>');

			// VAR/SELECTORS FOR THE CONTENT, TITLE AND THE PLACEHOLDER ELEMENT
			var thisEntityBody = $(thisEntityElement).next().next().find('.popover-body').html();
			var thisEntityTitle = $(thisEntityElement).next().next().find('.popover-heading').html();
			var thisEntityAfterElement = $(thisEntityElement).next('[data-placeholder="forPopover"]');

			// EVENT ON EVERY ENTITY TERM: POPOVER WITH INSERTED TITLE AND CONTENT
			$(this).popover({
				html: true,
				sanitize: false,
				boundary: 'window',
				placement: 'top',
				trigger: 'click',
				container: thisEntityAfterElement,
				content: function() {
					var content = thisEntityBody;
					return content;

				},
				title: function() {
					var title = thisEntityTitle;
					return title;
				},
			});

			// AFTER CREATING POPOVER EXECUTE AUTHORITY DATA SCRIPT (viewerJS.normdata.init)
			$(this).on('shown.bs.popover', function() {
				$('[data-entity-type]').not(this).popover('hide');

				// AUTHORITY DATA INIT
				viewerJS.normdata.init(_defaults.normdataConfig);

				// CLOSE POPOVERS WHEN CLICKED OUTSIDE OF THE POPOVERS
				$('html').on('click', function(e) {
					if (typeof $(e.target).data('original-title') == 'undefined' && !$(e.target).parents().is('.popover')) {
						$('[data-original-title]').popover('hide');
					}
				});
			})
		});
	}

	return viewer;

})(viewerJS || {}, jQuery);
