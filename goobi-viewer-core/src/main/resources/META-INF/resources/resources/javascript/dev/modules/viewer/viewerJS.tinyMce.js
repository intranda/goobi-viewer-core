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
 * @version 3.2.0
 * @module viewerJS.tinyMce
 * @requires jQuery
 */
var viewerJS = (function (viewer) {
    'use strict';

    var _debug = false;
    var _defaults = {
        currLang: 'de',
        selector: 'textarea.tinyMCE',
        width: '100%',
        height: 400,
        theme: 'silver',
        license_key: 'gpl',
        // TinyMCE 8 defaults this to true, rewriting existing <object>/
        // <embed> markup into <iframe>/<video>/<audio> on load and again
        // on save. Only one of the ~14 editor surfaces using this config
        // (the CMS htmltext field) runs its saved value through a
        // server-side sanitizer at all; the rest store whatever TinyMCE
        // produces unfiltered. Disabled globally until real content on
        // the unsanitized surfaces has been checked (see the plan's Task
        // 16 content-safety spot check).
        convert_unsafe_embeds: false,
        // TinyMCE 6+ flipped this default to true; without it, pasting an
        // image embeds it as a base64 data: URI, which HtmlSanitizer then
        // strips outright on the sanitized CMS field (see Task 0 decision
        // #11) and which can overflow TEXT columns on unsanitized ones.
        // false restores exactly the v5 behavior.
        paste_data_images: false,
        skin: 'tinymce-5',
        content_css: 'tinymce-5',
        plugins:
            'preview searchreplace autolink directionality code visualblocks visualchars fullscreen image link media codesample table charmap pagebreak nonbreaking anchor insertdatetime advlist lists wordcount help',
        toolbar:
            'blocks fontsize | undo redo | bold italic underline strikethrough | removeformat | alignleft aligncenter alignright alignjustify | bullist numlist | link anchor image media | table | code',
        font_size_formats: '10pt 12pt 14pt 15pt 16pt 18pt 24pt 36pt',
        menubar: false,
        statusbar: false,
        pagebreak_separator: '<span class="pagebreak"></span>',
        relative_urls: false,
        language: 'de',
        valid_children: '+a[div]',
        setup: function (ed) {
            // listen to changes on tinymce input fields
            ed.on('init', function (e) {
                viewerJS.stickyElements.refresh.next();
            });

            ed.on('change input paste', function (e) {
                ed.save();
                //tinymce.triggerSave();
                //trigger a change event on the underlying textArea
                $(ed.targetElm).change();
                // currentPage is only ever declared by the reference theme's
                // own page chrome; other consumers of this shared _defaults
                // (e.g. the crowdsourcing module's OCR editor) never declare
                // it, so this guard is required to avoid a ReferenceError on
                // every keystroke there.
                if (typeof currentPage !== 'undefined' && currentPage === 'adminCmsNewPage') {
                    createPageConfig.prevBtn.attr('disabled', true);
                    createPageConfig.prevDescription.show();
                }
            });
            ed.on('blur', function (e) {
                $(ed.targetElm).blur();
            });

            //			ed.ui.registry.addButton('myCustomToolbarButton', {
            //				text: 'My Custom Button',
            //	              onAction: function () {
            //	                alert('Button clicked!');
            //	              }
            //    		});
        },
    };

    viewer.tinyMce = {
        getConfig: function (config) {
            let c = $.extend(true, {}, _defaults, config);
            return c;
        },
        init: function (config) {
            this.config = $.extend(true, {}, _defaults, config);
            if (_debug) {
                console.log('##############################');
                console.log('viewer.tinyMce.init');
                console.log('##############################');
                console.log('viewer.tinyMce.init: config - ', this.config);
            }

            // check current language
            switch (this.config.currLang) {
                case 'de':
                    this.config.language = 'de';
                    break;
                case 'es':
                    this.config.language = 'es';
                    break;
                case 'pt':
                    this.config.language = 'pt_PT';
                    break;
                case 'ru':
                    this.config.language = 'ru';
                    break;
            }
            // console.log("tinymce init ", this.config);
            tinymce.init(this.config);
        },
        close: function () {
            tinymce.remove();
        },
    };

    return viewer;
})(viewerJS || {}, jQuery);

if (typeof module !== 'undefined' && module.exports) {
    module.exports = viewerJS;
}
