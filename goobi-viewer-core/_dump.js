const less = require('less'),
    fs = require('fs'),
    path = require('path');
const T = '../goobi-viewer-theme-reference/WebContent/resources/themes/reference/css/less';
const src = `@import '${T}/misc/variables.less';\n@import '${T}/misc/palette.less';\n@import '${T}/misc/tokens.less';\n`;
less.render(src, { filename: path.resolve('x.less'), math: 'always' })
    .then((o) => {
        fs.writeFileSync('/tmp/claude-501/-Users-sebastianlemling-code-work-viewer-goobi-viewer-core/a4567266-fa80-4124-8303-13258a5ea4c0/scratchpad/tokens.css', o.css);
        console.log('OK bytes', o.css.length);
    })
    .catch((e) => {
        console.error('LESS ERROR', e.message, e.filename, e.line);
        process.exit(1);
    });
