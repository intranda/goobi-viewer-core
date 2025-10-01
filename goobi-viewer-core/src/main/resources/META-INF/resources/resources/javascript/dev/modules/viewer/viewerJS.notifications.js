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
        success: (titleAlert, message) => viewer.notifications.notify(titleAlert, message, "success"),
        error: (titleAlert, message) => viewer.notifications.notify(titleAlert, message, "error"),
        warn: (titleAlert, message) => viewer.notifications.notify(titleAlert, message, "warning"),

        confirm: (message, confirmText, denyText, titleAlert, opts = {}) => {
            return viewer.translator.addTranslations(["cancel", "ok", "delete"])
                .then(() => {
                    confirmText = confirmText ? confirmText : viewerJS.translator.translate("delete");
                    denyText = denyText ? denyText : viewerJS.translator.translate("cancel");

                    const {
                        icon = 'error',
                        confirmButtonClass = 'btn btn--danger',
                        cancelButtonClass = 'btn btn--default'
                    } = opts;

                    if (typeof (Swal) !== 'undefined') {
                        const swalOptions = {
                            scrollbarPadding: false,
                            title: titleAlert,
                            text: message,
                            icon: icon,
                            showCancelButton: true,
                            confirmButtonText: confirmText,
                            cancelButtonText: denyText,
                            buttonsStyling: false,
                            reverseButtons: true,
                            showClass: {
                                popup: '-sweetAlertShowAnimation'
                            },
                            hideClass: {
                                popup: '-sweetAlertHideAnimation'
                            },
                            customClass: {
                                confirmButton: confirmButtonClass,
                                cancelButton: cancelButtonClass
                            }
                        };

                        return Swal.fire(swalOptions)
                            .then(result => result.isConfirmed ? Promise.resolve() : Promise.reject());
                    } else {
                        return window.confirm(message) ? Promise.resolve() : Promise.reject();
                    }
                });
        },

        notify: (titleAlert, message, type) => {
            if (typeof Swal !== 'undefined') {
                return Swal.fire({
                    scrollbarPadding: false,
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
            } else if (typeof sweetAlert !== 'undefined') {
                return swal(message, "", type);
            } else if (jQuery().overhang) {
                $("body").overhang({
                    type: type,
                    message: message
                });
            } else {
                alert(message);
            }
        }
    }

    // DEFINE SWEETALERT TOAST (SMALL BOX NOTIFICATION) BEHAVIOUR
    const swalToast = (typeof Swal !== 'undefined') ? Swal.mixin({
        toast: true,
        position: 'top-end',
        background: '#1b2330',
        showConfirmButton: false,
        animation: true,
        timer: 5000,
        // backdrop: `rgba(0,0,0,0.5)`,
        timerProgressBar: true,
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer)
            toast.addEventListener('mouseleave', Swal.resumeTimer)
        }
    }) : {};

    viewer.swaltoasts = {
        success: (titleAlert, message) => viewer.swaltoasts.toast(titleAlert, message, "success"),
        error: (titleAlert, message) => viewer.swaltoasts.toast(titleAlert, message, "error"),
        warn: (titleAlert, message) => viewer.swaltoasts.toast(titleAlert, message, "warn"),

        toast: (titleAlert, message, type) => {
            if (typeof Swal !== 'undefined') {
                swalToast.fire({
                    scrollbarPadding: false,
                    icon: type,
                    title: titleAlert,
                });
            } else if (typeof sweetAlert !== 'undefined') {
                swal(message, "", type);
            } else if (jQuery().overhang) {
                $("body").overhang({
                    type: type,
                    message: message
                });

            } else {
                alert(message);
            }
        },
        /**
         return viewer.swaltoasts if the status of event is "success". Otherwise return a psuedo viewer.swaltoasts which does nothing
         Use to filter messages from ajax requests so that only the success state produces a message:
         <code> event => viewer.swaltoasts.onSuccess(event).success("message")</code>
         **/
        onSuccess: function (event) {
            if (event.status == "success") {
                return this;
            } else {
                return {
                    success: (titleAlert, message) => {
                    },
                    error: (titleAlert, message) => {
                    },
                    warn: (titleAlert, message) => {
                    },
                }
            }
        }

    }


    function showJsfMessagesAsSweetAlert(e) {
        if (e == undefined || (e.responseText && e.responseText.includes("<ul id=\"messages\">"))) {	//only apply if messages div has been updated
            let $messages = $(".messages #messages");
            //handle error messages
            $messages.children(".alert.alert-danger").each((index, child) => {
                //show error message
                let text = $(child).text();
                if (text && text.trim().length > 0) {
                    viewerJS.swaltoasts.error(text);
                    //scroll to first validation message
                    let $elementsWithValidationError = $(".-validation-message.-danger");
                    if ($elementsWithValidationError.length > 0) {
                        $elementsWithValidationError.get(0).scrollIntoView({block: "center"});
                    }
                }
            });
            //show success message
            $messages.children(".alert.alert-success").each((index, child) => {
                let text = $(child).text();
                if (text && text.trim().length > 0) {
                    viewerJS.swaltoasts.success(text);
                }
            });
        }
    }

    //post notification on ajax/success
    viewer.jsfAjax.success.subscribe(e => {
        showJsfMessagesAsSweetAlert(e);
    });
    $(document).ready(() => showJsfMessagesAsSweetAlert());

    //confirmation dialogs for jsf control elements
    function initJsfConfirmationDialogs() {
        let dialogs = $('[data-require-confirmation="true"]');

        $('[data-require-confirmation="true"]')
            .each((index, element) => {
                //store the original (jsf) event in the element. If initJsfConfirmationDialogs gets retriggered due to ajax without the element
                //being changed, the original jsf-call is kept as the original event
                if (!element.hasConfirmationEvent) {
                    element.hasConfirmationEvent = true;
                    element.originalEvent = element.onclick;
                    element.onclick = e => {
                        const ds = e.currentTarget.dataset;

                        const alertIconName = ds.icon || 'error';
                        const alertIcon = (alertIconName === 'warn') ? 'warning' : alertIconName;

                        const confirmButtonClass = ds.confirmButton;
                        const cancelButtonClass = ds.cancelButton;
                        const confirmButtonText = ds.confirmButtonText;
                        const cancelButtonText = ds.cancelButtonText;
                        const confirmationText = ds.confirmationText;

                        const opts = {
                            icon: alertIcon,
                            confirmButtonClass: confirmButtonClass,
                            cancelButtonClass: cancelButtonClass,
                        };

                        viewer.notifications.confirm(
                            undefined,
                            confirmButtonText,
                            cancelButtonText,
                            confirmationText,
                            opts
                        )
                            .then(() => {
                                if (element.dataset.confirmationTarget) {
                                    const target = document.querySelector(element.dataset.confirmationTarget);
                                    if (target) {
                                        target.click();
                                    }
                                } else if (element.originalEvent) {
                                    element.originalEvent(e);
                                } else {
                                    let form = $(element).closest("form").get(0);
                                    form.requestSubmit(element);
                                }
                            })
                            .catch(() => {
                            });
                        //return false to this event to stop submit action
                        return false;
                    }
                }
            });
    }

    viewer.jsfAjax.success.subscribe(e => {
        initJsfConfirmationDialogs();
    });
    $(document).ready(() => initJsfConfirmationDialogs());

    return viewer;

})(viewerJS || {}, jQuery);
