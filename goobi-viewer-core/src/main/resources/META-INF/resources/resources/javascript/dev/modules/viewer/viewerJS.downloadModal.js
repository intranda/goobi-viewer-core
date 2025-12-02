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
 * Module which generates a download modal which dynamic content.
 * 
 * @version 3.2.0
 * @module viewerJS.downloadModal
 * @requires jQuery
 * 
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    // default variables
    var _debug = false;
    var _defaults = {
        dataType: null,
        dataTitle: null,
        dataId: null,
        dataPi: null,
        downloadBtn: null,
        reCaptchaSiteKey: '',
        useReCaptcha: true,
        path: '',
        iiifPath: '',
        apiUrl: '',
        userEmail: null,
        workInfo: {},
        modal: {
            id: '',
            label: '',
            string: {
                title: '',
                body: '',
                closeBtn: '',
                saveBtn: '',
            }
        },
        messages: {
            downloadInfo: {
                text: 'Informationen zum angeforderten Download',
                title: 'Werk',
                part: 'Teil',
                fileSize: 'Größe'
            },
            reCaptchaText: 'Um die Generierung von Dokumenten durch Suchmaschinen zu verhindern bestätigen Sie bitte das reCAPTCHA.',
            rcInvalid: 'Die Überprüfung war nicht erfolgreich. Bitte bestätigen Sie die reCAPTCHA Anfrage.',
            rcValid: 'Vielen Dank. Sie können nun ihre ausgewählte Datei generieren lassen.',
            eMailText: 'Um per E-Mail informiert zu werden sobald der Download zur Verfügung steht, können Sie hier optional Ihre E-Mail Adresse hinterlassen',
            eMailTextLoggedIn: 'Sie werden über Ihre registrierte E-Mail Adresse von uns über den Fortschritt des Downloads informiert.',
            eMail: ''
        }
    };
    var _loadingOverlay = null;
    
    viewer.downloadModal = {
        /**
         * Method to initialize the download modal mechanic.
         * 
         * @method init
         * @param {Object} config An config object which overwrites the defaults.
         * @param {String} config.dataType The data type of the current file to download.
         * @param {String} config.dataTitle The title of the current file to download.
         * @param {String} config.dataId The LOG_ID of the current file to download.
         * @param {String} config.dataPi The PI of the current file to download.
         * @param {Object} config.downloadBtn A collection of all buttons with the class
         * attribute 'download-modal'.
         * @param {String} config.reCaptchaSiteKey The site key for the google reCAPTCHA,
         * fetched from the viewer config.
         * @param {String} config.path The current application path.
         * @param {String} config.apiUrl The URL to trigger the ITM download task.
         * @param {String} config.userEmail The current user email if the user is logged
         * in. Otherwise the one which the user enters or leaves blank.
         * @param {Object} config.modal A configuration object for the download modal.
         * @param {String} config.modal.id The ID of the modal.
         * @param {String} config.modal.label The label of the modal.
         * @param {Object} config.modal.string An object of strings for the modal content.
         * @param {String} config.modal.string.title The title of the modal.
         * @param {String} config.modal.string.body The content of the modal as HTML.
         * @param {String} config.modal.string.closeBtn Buttontext
         * @param {String} config.modal.string.saveBtn Buttontext
         * @param {Object} config.messages An object of strings for the used text
         * snippets.
         * @example
         * 
         * <pre>
         * var downloadModalConfig = {
         *     downloadBtn: $( '.download-modal' ),
         *     path: '#{navigationHelper.applicationUrl}',
         *     userEmail: $( '#userEmail' ).val(),
         *     messages: {
         *         reCaptchaText: '#{msg.downloadReCaptchaText}',
         *         rcInvalid: '#{msg.downloadRcInvalid}',
         *         rcValid: '#{msg.downloadRcValid}',
         *         eMailText: '#{msg.downloadEMailText}',
         *         eMailTextLoggedIn: '#{msg.downloadEMailTextLoggedIn}',
         *         eMail: '#{msg.downloadEmail}',
         *         closeBtn: '#{msg.downloadCloseModal}',
         *         saveBtn: '#{msg.downloadGenerateFile}',
         *     }
         * };
         * 
         * viewerJS.downloadModal.init( downloadModalConfig );
         * </pre>
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'viewer.downloadModal.init' );
                console.log( '##############################' );
                console.log( 'viewer.downloadModal.init: config = ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // build loading overlay
            _loadingOverlay = $( '<div />' );
            _loadingOverlay.addClass( 'dl-modal__overlay' );
            $( 'body' ).append( _loadingOverlay );
            
            $(_defaults.downloadBtn).on( 'click', function(e) {
                // show loading overlay
                $( '.dl-modal__overlay' ).fadeIn( 'fast' );
                
                _defaults.dataType = $( this ).attr( 'data-type' );
                _defaults.dataTitle = $( this ).attr( 'data-title' );
                if ( $( this ).attr( 'data-id' ) !== '' ) {
                    _defaults.dataId = $( this ).attr( 'data-id' );
                }
                else {
                    _defaults.dataId = '-';
                }
                _defaults.dataPi = $( this ).attr( 'data-pi' );
                _getWorkInfo( _defaults.dataPi, _defaults.dataId, _defaults.dataType ).done( function( info ) {
                    _defaults.workInfo = info;
                    
                    _defaults.modal = {
                        id: _defaults.dataPi + '-Modal',
                        label: _defaults.dataPi + '-Label',
                        string: {
                            title: _defaults.dataTitle,
                            body: viewer.downloadModal.renderModalBody( _defaults.dataType, _defaults.workInfo ),
                            closeBtn: _defaults.messages.closeBtn,
                            saveBtn: _defaults.messages.saveBtn,
                        }
                    };
                    
                    // hide loading overlay
                    $( '.dl-modal__overlay' ).fadeOut( 'fast' );
                    
                    // init modal
                    viewer.downloadModal.initModal( _defaults );
                } );
            } );
        },
        /**
         * Method which initializes the download modal and its content.
         * 
         * @method initModal
         * @param {Object} params An config object which overwrites the defaults.
         */
        initModal: function( params ) {
            if ( _debug ) {
                console.log( '---------- viewer.downloadModal.initModal() ----------' );
                console.log( 'viewer.downloadModal.initModal: params = ', params );
            }
            $( 'body' ).append( viewer.helper.renderModal( params.modal ) );
            
            // disable submit button
            $( '#submitModal' ).attr( 'disabled', 'disabled' );
            
            // show modal
            $( '#' + CSS.escape(params.modal.id) ).modal( 'show' );
            
            // render reCAPTCHA to modal
            $( '#' + CSS.escape(params.modal.id) ).on( 'shown.bs.modal', function( e ) {
                if ( _defaults.useReCaptcha ) {
                    var rcWidget = grecaptcha.render( 'reCaptchaWrapper', {
                        sitekey: _defaults.reCaptchaSiteKey,
                        callback: function() {
                            var rcWidgetResponse = viewer.downloadModal.validateReCaptcha( grecaptcha.getResponse( rcWidget ) );
                            
                            if ( rcWidgetResponse ) {
                                $( '#modalAlerts' ).append( viewer.helper.renderAlert( 'alert-success', _defaults.messages.rcValid, true ) );
                                
                                // enable submit button
                                $( '#submitModal' ).removeAttr( 'disabled' ).on( 'click', function() {
                                    _defaults.userEmail = $( '#recallEMail' ).val();
                                    
                                    _defaults.apiUrl = viewer.downloadModal
                                            .buildAPICall( _defaults.path, _defaults.dataType, _defaults.dataPi, _defaults.dataId, _defaults.userEmail );
                                    
                                    fetch(_defaults.apiUrl, {method: 'PUT'})
                                    .then(response => response.json())
                                    .then(response => window.location.href = response.url);
                                    
                                } );
                            }
                            else {
                                $( '#modalAlerts' ).append( viewer.helper.renderAlert( 'alert-danger', _defaults.messages.rcInvalid, true ) );
                            }
                        }
                    } );
                }
                else {
                    // hide paragraph
                    $( this ).find( '.modal-body h3' ).next( 'p' ).hide();
                    
                    // enable submit button
                    $( '#submitModal' ).removeAttr( 'disabled' ).on( 'click', function() {
                        _defaults.userEmail = $( '#recallEMail' ).val();
                        
                        _defaults.apiUrl = viewer.downloadModal.buildAPICall( _defaults.path, _defaults.dataType, _defaults.dataPi, _defaults.dataId, _defaults.userEmail );
                        
                        fetch(_defaults.apiUrl, {method: 'PUT'})
                        .then(response => response.json())
                        .then(response => window.location.href = response.url);
                    } );
                }
            } );
            
            // remove modal from DOM after closing
            $( '#' + CSS.escape(params.modal.id) ).on( 'hidden.bs.modal', function( e ) {
                $( this ).remove();
            } );
        },
        /**
         * Method which returns a HTML-String to render the download modal body.
         * 
         * @method renderModalBody
         * @param {String} type The current file type to download.
         * @param {String} title The title of the current download file.
         * @returns {String} The HTML-String to render the download modal body.
         */
        renderModalBody: function( type, infos ) {
            if ( _debug ) {
                console.log( '---------- viewer.downloadModal.renderModalBody() ----------' );
                console.log( 'viewer.downloadModal.renderModalBody: type = ', type );
                console.log( 'viewer.downloadModal.renderModalBody: infos = ', infos );
            }
            var rcResponse = null;
            var modalBody = '';
            
            modalBody += '';
            // alerts
            modalBody += '<div id="modalAlerts"></div>';
            // Title
            if ( type === 'pdf' ) {
                modalBody += '<h3 class="modal__title">';
                modalBody += '<span class="icon-wrapper modal__title-icon" aria-hidden="true">'
                    + '<svg class="icon" focusable="false"><use href="' + _defaults.path + 'resources/icons/outline/file-type-pdf.svg#icon"></use></svg>'
                    + '</span>PDF-Download: ';
                modalBody += '</h3>';
            }
            else {
                modalBody += '<h3 class="modal__title">';
                modalBody += '<span class="icon-wrapper modal__title-icon" aria-hidden="true">'
                    + '<svg class="icon" focusable="false"><use href="' + _defaults.path + 'resources/icons/outline/file-text.svg#icon"></use></svg>'
                    + '</span>ePub-Download: ';
                modalBody += '</h3>';
            }
            // Info
            modalBody += '<p>' + _defaults.messages.downloadInfo.text + ':</p>';
            modalBody += '<dl class="dl-horizontal">';
            modalBody += '<dt>' + _defaults.messages.downloadInfo.title + ':</dt>';
            modalBody += '<dd>' + infos.title + '</dd>';
            if ( infos.div !== null ) {
                modalBody += '<dt>' + _defaults.messages.downloadInfo.part + ':</dt>';
                modalBody += '<dd>' + infos.div + '</dd>';
            }
            if ( infos.size ) {
                modalBody += '<dt>' + _defaults.messages.downloadInfo.fileSize + ':</dt>';
                modalBody += '<dd>~' + infos.size + '</dd>';
                modalBody += '</dl>';
            }
            // reCAPTCHA
            if ( _defaults.useReCaptcha ) {
                modalBody += '<hr />';
                modalBody += '<p><strong>reCAPTCHA</strong></p>';
                modalBody += '<p>' + _defaults.messages.reCaptchaText + ':</p>';
                modalBody += '<div id="reCaptchaWrapper"></div>';
            }
            // E-Mail
            modalBody += '<hr />';
            modalBody += '<form class="email-form">';
            modalBody += '<div class="form-group">';
            modalBody += '<label for="recallEMail">' + _defaults.messages.eMail + '</label>';
            if ( _defaults.userEmail != undefined ) {
                modalBody += '<p class="help-block">' + _defaults.messages.eMailTextLoggedIn + '</p>';
                modalBody += '<input type="email" class="form-control" id="recallEMail" value="' + _defaults.userEmail + '" disabled="disabled" />';
            }
            else {
                modalBody += '<p class="help-block">' + _defaults.messages.eMailText + ':</p>';
                modalBody += '<input type="email" class="form-control" id="recallEMail" />';
            }
            modalBody += '</div>';
            modalBody += '</form>';
            
            return modalBody;
        },
        /**
         * Method which checks the reCAPTCHA response.
         * 
         * @method validateReCaptcha
         * @param {String} response The reCAPTCHA response.
         * @returns {Boolean} Returns true if the reCAPTCHA sent a response.
         */
        validateReCaptcha: function( response ) {
            if ( _debug ) {
                console.log( '---------- viewer.downloadModal.validateReCaptcha() ----------' );
                console.log( 'viewer.downloadModal.validateReCaptcha: response = ', response );
            }
            if ( response == 0 ) {
                return false;
            }
            else {
                return true;
            }
        },
        /**
         * Method which returns an URL to trigger the ITM download task.
         * 
         * @method buildAPICall
         * @param {String} path The current application path.
         * @param {String} type The current file type to download.
         * @param {String} pi The PI of the current work.
         * @param {String} logid The LOG_ID of the current work.
         * @param {String} email The current user email.
         * @returns {String} The URL to trigger the ITM download task.
         */
        buildAPICall: function( path, type, pi, logid, email ) {
            if ( _debug ) {
                console.log( '---------- viewer.downloadModal.buildAPICall() ----------' );
                console.log( 'viewer.downloadModal.buildAPICall: path = ', path );
                console.log( 'viewer.downloadModal.buildAPICall: type = ', type );
                console.log( 'viewer.downloadModal.buildAPICall: pi = ', pi );
                console.log( 'viewer.downloadModal.buildAPICall: logid = ', logid );
                console.log( 'viewer.downloadModal.buildAPICall: email = ', email );
            } 
            var url = '';
            
            url += path + 'api/v1/downloads/';
            url += type;
            url += "/records/";
            url += pi;
            if(logid) {
                url += "/sections/";
                url += logid;
            }
            url += "/";
            if(email) {
                url += "?email=";
                url += email;
            }
            
            return encodeURI( url );
        }
    };
    
    /**
     * Method which returns a promise if the work info has been reached.
     * 
     * @method getWorkInfo
     * @param {String} pi The PI of the work.
     * @param {String} logid The LOG_ID of the work.
     * @returns {Promise} A promise object if the info has been reached.
     */
    function _getWorkInfo( pi, logid, type ) {
        if ( _debug ) {
            console.log( '---------- _getWorkInfo() ----------' );
            console.log( '_getWorkInfo: pi = ', pi );
            console.log( '_getWorkInfo: logid = ', logid );
            console.log( '_getWorkInfo: type = ', type );
        }
        
        var restCall = '';
        var workInfo = {}; 
        var iiifPath = _defaults.iiifPath.replace("/rest", "/api/v1");
        if ( logid !== '' && logid !== undefined && logid != '-' ) {
            restCall = iiifPath + "records/" + pi + "/sections/" + logid + '/' + type + '/info.json';
             
            if ( _debug ) {
                console.log( 'if' ); 
                console.log( '_getWorkInfo: restCall = ', restCall );
            }
        }
        else {
            restCall = iiifPath + "records/" + pi + "/" + type + '/info.json';
            
            if ( _debug ) {
                console.log( 'else' );
                console.log( '_getWorkInfo: restCall = ', restCall );
            }
        }
        
        return viewerJS.helper.getRemoteData( restCall );
    }
    
    return viewer;
    
} )( viewerJS || {}, jQuery );
