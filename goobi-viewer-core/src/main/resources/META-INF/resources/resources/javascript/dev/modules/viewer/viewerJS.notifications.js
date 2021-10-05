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
 * <Short Module Description>
 * 
 * @version 21.04
 * @module viewerJS.notifications
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = false; 
	
    viewer.notifications = {
		success : (titleAlert, message) => viewer.notifications.notify(titleAlert, message, "success"),
		error : (titleAlert, message) => viewer.notifications.notify(titleAlert, message, "error"),
		warn : (titleAlert, message) => viewer.notifications.notify(titleAlert, message, "warn"),
		confirm : (message, confirmText, denyText, titleAlert) => {
			return viewer.translator.addTranslations(["cancel", "ok"])
			.then( () => {
				confirmText = confirmText ? confirmText : viewerJS.translator.translate("ok");
				denyText = denyText ? denyText : viewerJS.translator.translate("cancel");
				if(typeof(Swal) !== 'undefined') {
					return Swal.fire({
						title: titleAlert,
						text: message,
						icon: 'warning',
						showCancelButton: true,
						confirmButtonText: confirmText,
	  					cancelButtonText: denyText,
					    buttonsStyling: false,
						showClass: {
							popup: '-sweetAlertShowAnimation'
						},
						hideClass: {
							popup: '-sweetAlertHideAnimation'
						},
	  					customClass: {
						    confirmButton: 'btn btn--full',
						    cancelButton: 'btn btn--default'
						  }
					})
					.then(result => {
						return result.isConfirmed ? Promise.resolve() : Promise.reject();
					});
				} else {
					return window.confirm(message) ? Promise.resolve() : Promise.reject();
				}		
				
			});
		},
		notify : (titleAlert, message, type) => {
			if(typeof Swal !== 'undefined') {
				Swal.fire({
					title: titleAlert,
					text: message,
					icon: type,
				    buttonsStyling: false,
					showClass: {
						popup: '-sweetAlertShowAnimation'
					},
					hideClass: {
						popup: '-sweetAlertHideAnimation'
					},
					customClass: {
					    confirmButton: 'btn btn--full',
					    cancelButton: 'btn btn--default'
					  }
				});
			} else if(typeof sweetAlert !== 'undefined') {
				swal(message, "", type);
			} else if(jQuery().overhang) {
				$("body").overhang({
				  type: type,
				  message: message
				});

			} else {
				alert(message);
			}			
		}
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
