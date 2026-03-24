/**
 * Wrapper around @prettier/plugin-xml that prevents the embedded JS formatter
 * from reformatting HTML strings inside <script> tags in XHTML files.
 *
 * Root cause: the XML plugin calls textToDoc(source, { parser }) without
 * passing through user options like embeddedLanguageFormatting, so that option
 * defaults to 'auto' and Prettier detects + reformats HTML strings in $('...').
 *
 * Fix: wrap the embed function so that every textToDoc call inside it
 * receives embeddedLanguageFormatting: 'off'.
 */
import xmlPlugin from '@prettier/plugin-xml';

const originalEmbed = xmlPlugin.printers.xml.embed;

function patchedEmbed(path, opts) {
    const result = originalEmbed.call(this, path, opts);
    if (!result) {
        return result;
    }
    return async function (textToDoc, print, ...rest) {
        const patchedTextToDoc = (text, innerOpts) =>
            textToDoc(text, { ...innerOpts, embeddedLanguageFormatting: 'off' });
        return result(patchedTextToDoc, print, ...rest);
    };
}

export default {
    ...xmlPlugin,
    printers: {
        ...xmlPlugin.printers,
        xml: {
            ...xmlPlugin.printers.xml,
            embed: patchedEmbed,
        },
    },
};
