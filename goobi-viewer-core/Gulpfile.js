const gulp = require('gulp');
const path = require('path');
const fs = require('fs');
const os = require('os');
const XML = require('pixl-xml');

const less = require('gulp-less');
const sourcemaps = require('gulp-sourcemaps');
const concat = require('gulp-concat');
const header = require('gulp-header');
const riot = require('gulp-riot');
const rename = require('gulp-rename');
const merge = require('merge-stream');
const through = require('through2');
const svgmin = require('gulp-svgmin');
const cheerio = require('cheerio');
const colors = require('ansi-colors');
const log = require('fancy-log');
const {spawn} = require('child_process');

const { rollup } = require('rollup');
const terser = require('gulp-terser');

const postcss = require('gulp-postcss');
const autoprefixer = require('autoprefixer');
const reporter = require('postcss-reporter');
const {depsPathsJS, depsPathsCSS, tablerIconSources} = require('./gulp/depsPaths');

const isWin = process.platform === 'win32';
const toPosix = (p) => (p ? p.replace(/\\/g, '/') : p);
const joinPosix = (...segs) => toPosix(path.join(...segs));

const paths = {
    jsDevRoot: 'src/main/resources/META-INF/resources/resources/javascript/dev/',
    jsModulesRoot: 'src/main/resources/META-INF/resources/resources/javascript/dev/modules/',
    jsDistRoot: 'src/main/resources/META-INF/resources/resources/javascript/dist/',
    cssRoot: 'src/main/resources/META-INF/resources/resources/css/',
    cssDistRoot: 'src/main/resources/META-INF/resources/resources/css/dist/',
    lessRoot: 'src/main/resources/META-INF/resources/resources/css/less/',
    staticRoot: 'src/main/resources/META-INF/resources',
};

const iconOutputRoot = joinPosix(paths.staticRoot, 'resources', 'icons');

const banner = `/*!
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 * - http://www.intranda.com
 * - http://digiverso.com
 * GPLv2 or later. NO WARRANTY.
 */\n`;

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Feature-Toggles                                                     ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/** Toggle Autoprefixer warnings (set GV_APWARN=0 to hide locally). */
const SHOW_AP_WARNINGS = process.env.GV_APWARN !== '0';

/** Optional: completely disable Autoprefixer (set GV_AUTOPREFIX=0). */
const ENABLE_AUTOPREFIX = process.env.GV_AUTOPREFIX !== '0';

/** Optional: verbose file list for sync-all (set GV_SYNC_VERBOSE=1). */
const VERBOSE_SYNC = process.env.GV_SYNC_VERBOSE === '1';

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Resolve deployment/Core/Theme directories                            ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Computes `DEPLOYMENT_DIR` (running webapp root) and local repo roots.
 * Honors environment overrides and falls back to a standard ~/git layout.
 *
 * Env overrides (all optional):
 *   GV_CORE_DIR   : absolute path to the core repo
 *   GV_THEME_DIR  : absolute path to the theme repo
 *   GV_VIEWER_CFG : absolute path to config_viewer.xml
 *   GV_GULP_CFG   : absolute path to ~/.config/gulp_userconfig.json
 *
 * @returns {{DEPLOYMENT_DIR:string, CORE_DIR:string, THEME_DIR:string}}
 * @throws {Error} If the user config or viewer XML cannot be read/parsed.
 */
function resolveDirs() {
    const home = os.homedir();

    const gulpCfgPath =
        process.env.GV_GULP_CFG || path.join(home, '.config', 'gulp_userconfig.json');
    const viewerCfgPath =
        process.env.GV_VIEWER_CFG ||
        (isWin
            ? path.join('C:', 'opt', 'digiverso', 'viewer', 'config', 'config_viewer.xml')
            : path.join('/', 'opt', 'digiverso', 'viewer', 'config', 'config_viewer.xml'));

    // Read gulp user config (JSON)
    let cfg;
    try {
        cfg = JSON.parse(fs.readFileSync(path.resolve(gulpCfgPath), 'utf-8'));
    } catch (e) {
        throw new Error(`Cannot parse ${gulpCfgPath}: ${e.message}`);
    }

    // Read viewer XML config
    let viewerConfig;
    try {
        viewerConfig = XML.parse(fs.readFileSync(path.resolve(viewerCfgPath), 'utf-8'));
    } catch (e) {
        throw new Error(`Cannot parse ${viewerCfgPath}: ${e.message}`);
    }

    const theme = viewerConfig?.viewer?.theme || {};
    const special = theme.specialName && String(theme.specialName);
    const mainTheme = String(theme.mainTheme || 'reference');

    // Deployment target
    let deployDir;

    if (special && special.length) {
        deployDir = path.join(cfg.tomcat_dir, `goobi-viewer-theme-${special}`);
    } else {
        const candidates = [
            path.join(cfg.tomcat_dir, `goobi-viewer-theme-${mainTheme}`),

            path.join(
                home,
                'git',
                'goobi-viewer',
                `goobi-viewer-theme-${mainTheme}`,
                `goobi-viewer-theme-${mainTheme}`,
                'target',
                'viewer'
            ),
        ];

        deployDir = candidates.find((c) => fs.existsSync(c)) || candidates[0];
    }

    // Repo roots
    const coreDir = process.env.GV_CORE_DIR || __dirname;

    let themeDir = process.env.GV_THEME_DIR || null;
    if (!themeDir) {
        themeDir = path.join(
            home,
            'git',
            'goobi-viewer',
            `goobi-viewer-theme-${mainTheme}`,
            `goobi-viewer-theme-${mainTheme}`
        );
    }

    return {DEPLOYMENT_DIR: deployDir, CORE_DIR: coreDir, THEME_DIR: themeDir};
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Helpers: Gulp-related helpers                                        ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Validates that a directory exists and is a directory.
 *
 * @param {string} label Short name used in error messages.
 * @param {string} dir Absolute path to check.
 * @throws {Error} If the path does not exist or is not a directory.
 */
function assertDirExists(label, dir) {
    if (!dir) throw new Error(`${label} not resolved`);
    try {
        const st = fs.statSync(dir);
        if (!st.isDirectory()) throw new Error(`${label} is not a directory: ${dir}`);
    } catch {
        throw new Error(`${label} does not exist: ${dir}`);
    }
}

/* Resolve once on load, assert lazily for tasks that require deployment */
const {DEPLOYMENT_DIR, CORE_DIR, THEME_DIR} = (() => {
    const dirs = resolveDirs();
    return dirs;
})();

let deploymentDirChecked = false;
function requireDeploymentDir() {
    if (!deploymentDirChecked) {
        assertDirExists('DEPLOYMENT_DIR', DEPLOYMENT_DIR);
        deploymentDirChecked = true;
    }
}

const homeDir = os.homedir();
const prettyPath = (p) => (p ? toPosix(String(p).replace(homeDir, '~')) : '');

/**
 * Returns a safe destination stream into a deployment subfolder.
 * If the target subfolder does not exist, emits a warning and returns a no-op
 * passthrough (so nothing is written and the watcher stays alive).
 *
 * @param {string} subPath Subdirectory inside `DEPLOYMENT_DIR`.
 * @returns {NodeJS.ReadWriteStream} A gulp destination or a no-op stream.
 */
function safeDest(subPath) {
    const full = path.join(DEPLOYMENT_DIR, subPath);
    fs.mkdirSync(full, { recursive: true });
    if (!fs.existsSync(full)) {
        log(colors.yellow(`[deploy] target does not exist, skipping: ${prettyPath(full)}`));
        return through.obj((f, _e, cb) => cb(null, f));
    }
    return gulp.dest(full);
}

/**
 * Optional gulp-plumber integration.
 * If installed, prevents pipe breaking on plugin errors and logs them.
 * If not installed, returns a no-op passthrough so pipelines keep working.
 *
 * @returns {NodeJS.ReadWriteStream} A transform stream usable in Gulp pipes.
 */
let plumber = null;
try {
    plumber = require('gulp-plumber');
} catch {
}
const noopThrough = () => through.obj((f, _e, cb) => cb(null, f));

function guard() {
    return plumber
        ? plumber({
            errorHandler(err) {
                log(colors.red(err && err.message ? err.message : String(err)));
                if (this && typeof this.emit === 'function') this.emit('end');
            },
        })
        : noopThrough();
}

/**
 * Formats a high-resolution timer difference into milliseconds string.
 *
 * @param {bigint} t0 A monotonic timestamp from `process.hrtime.bigint()`.
 * @returns {string} e.g. "123 ms".
 */
function elapsedMs(t0) {
    return ((Number(process.hrtime.bigint() - t0) / 1e6) || 0).toFixed(0) + ' ms';
}

/**
 * Prints a titled, indented block to the console.
 *
 * @param {string} title Section title.
 * @param {string[]} lines Lines to print under the title (already colorized if desired).
 */
function logBlock(title, lines) {
    console.log(colors.white(`\n[${title}]`));
    lines.forEach((l) => console.log('  ' + l));
}

/**
 * Creates a tap transform that calls `push(file)` for every file flowing through.
 *
 * @param {(file: import('vinyl')) => void} push Callback to observe files.
 * @returns {NodeJS.ReadWriteStream} Through2 passthrough.
 */
function collectFiles(push) {
    return through.obj(function (file, _, cb) {
        try {
            push(file);
        } catch {
        }
        cb(null, file);
    });
}

/**
 * Generates gulp streams that process Tabler SVG icons into the viewer resources.
 *
 * @param {Function} [onCopy] Optional callback invoked for every processed file.
 * @returns {NodeJS.ReadWriteStream[]} Streams (one per icon variant).
 */
function createIconStreams(onCopy) {
    if (!Array.isArray(tablerIconSources) || tablerIconSources.length === 0) {
        return [];
    }

    return tablerIconSources
        .map(({variant, src, base}) => {
            if (!src) return null;

            const srcOpts = {allowEmpty: true};
            if (base) srcOpts.base = base;

            const destDir = variant ? joinPosix(iconOutputRoot, variant) : iconOutputRoot;
            let stream = gulp
                .src(src, srcOpts)
                .pipe(guard())
                .pipe(
                    svgmin({
                        plugins: [{removeViewBox: false}],
                    })
                )
                .pipe(
                    through.obj((file, _enc, cb) => {
                        if (file.isBuffer()) {
                            const $ = cheerio.load(file.contents.toString(), {xmlMode: true});
                            const svg = $('svg');
                            if (svg.length) {
                                if (!svg.attr('id')) svg.attr('id', 'icon');
                                svg.removeAttr('width');
                                svg.removeAttr('height');
                                file.contents = Buffer.from($.xml());
                            }
                        }
                        cb(null, file);
                    })
                );

            if (typeof onCopy === 'function') {
                stream = stream.pipe(collectFiles(onCopy));
            }

            return stream.pipe(gulp.dest(destDir));
        })
        .filter(Boolean);
}

/**
 * Processes Tabler SVG icons and writes them to `resources/icons`.
 *
 * @returns {Promise<void>|NodeJS.ReadWriteStream}
 */
function buildIcons() {
    const streams = createIconStreams();
    if (streams.length === 0) {
        return Promise.resolve();
    }

    return streams.length === 1 ? streams[0] : merge(streams);
}

/** Unified footer line for task summaries. */
function taskFooter(generated, copied, errors, started) {
    return (
        colors.green('✓ ') +
        colors.white(`${generated} generated · ${copied} copied · ${errors} errors · `) +
        colors.magenta(elapsedMs(started))
    );
}

/**
 * Compact task logger to avoid boilerplate in tasks.
 *
 * @param {Object} opts
 * @param {string}  opts.name        Task name for the section header.
 * @param {bigint}  opts.started     Monotonic start timestamp (from process.hrtime.bigint()).
 * @param {string=} opts.changed     Optional path of the file that triggered the run.
 * @param {string=} opts.src         Optional source glob/path to display.
 * @param {string[]=} opts.projOut   Project output file paths.
 * @param {string[]=} opts.deployOut Deployment output file paths.
 * @param {number=}  opts.genCount   Override for “generated” count (defaults to projOut.length).
 * @param {number=}  opts.copyCount  Override for “copied” count (defaults to deployOut.length).
 * @param {number=}  opts.errors     Number of errors (default: 0).
 * @param {string[]=} opts.extra     Extra lines to append to the log block.
 */
function logTask({
                     name,
                     started,
                     changed,
                     src,
                     projOut = [],
                     deployOut = [],
                     genCount,
                     copyCount,
                     errors = 0,
                     extra = [],
                 }) {
    const changedPath = (typeof changed === 'string') ? changed : undefined;

    const lines = [`time: ${colors.gray(new Date().toLocaleTimeString('de-DE', {hour12: false}))}`];
    if (changedPath) {
        lines.push(`changed: ${colors.green(prettyPath(changedPath))}`);
    } else if (src) {
        lines.push(`src: ${colors.green(src)}`);
    }
    if (projOut.length) lines.push('→ project:', ...projOut.map((p) => '  • ' + colors.blue(prettyPath(p))));
    if (deployOut.length) lines.push('→ deploy:', ...deployOut.map((p) => '  • ' + colors.blue(prettyPath(p))));
    if (extra.length) lines.push(...extra);
    const gen = (typeof genCount === 'number') ? genCount : projOut.length;
    const copy = (typeof copyCount === 'number') ? copyCount : deployOut.length;
    lines.push(taskFooter(gen, copy, errors, started));
    logBlock(name, lines);
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Styles: LESS → CSS + (sourcemaps & autoprefixer)                     ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Compiles the main LESS entrypoint into a minified CSS bundle with sourcemaps.
 * Runs PostCSS Autoprefixer (toggle via GV_AUTOPREFIX / GV_APWARN).
 * Writes to project dist and mirrors into deployment (if target exists).
 *
 * @param {?string=} changedFilePath Optional path that triggered rebuild (for logging).
 * @returns {NodeJS.ReadWriteStream} Gulp pipeline.
 */
function buildStyles(changedFilePath = null) {
    requireDeploymentDir();
    const started = process.hrtime.bigint();
    const lessEntryFile = path.join(paths.lessRoot, 'constructor.less');

    const projectOutputs = [];
    const deployOutputs = [];

    const collectProjectOutputs = collectFiles((file) => {
        const base = path.resolve(paths.cssDistRoot);
        projectOutputs.push(path.join(base, path.basename(file.path)));
    });
    const collectDeployOutputs = collectFiles((file) => {
        const rel = toPosix(path.relative(paths.staticRoot, file.path));
        deployOutputs.push(path.join(DEPLOYMENT_DIR, rel));
    });

    return gulp
        .src(lessEntryFile, {allowEmpty: true})
        .pipe(guard())
        .pipe(sourcemaps.init())
        .pipe(less({compress: true}))
        .pipe(
            postcss([
                ...(ENABLE_AUTOPREFIX ? [autoprefixer()] : []),
                ...(SHOW_AP_WARNINGS
                    ? [reporter({clearReportedMessages: true, throwError: false})]
                    : []),
            ])
        )
        .pipe(header(banner))
        .pipe(rename('viewer.min.css'))
        .pipe(sourcemaps.write('.', {
            includeContent: true,
            sourceRoot: toPosix(path.relative(paths.cssDistRoot, paths.lessRoot)) || '.'
        }))

        .pipe(collectProjectOutputs)
        .pipe(gulp.dest(paths.cssDistRoot))

        .pipe(collectDeployOutputs)
        .pipe(through.obj(function(file, enc, cb) {
            if (file.stat) {
                file.stat.mtime = new Date();
                file.stat.atime = new Date();
            }
            cb(null, file);
        }))
        .pipe(safeDest('resources/css/dist'))

        .on('finish', () => {
            logTask({
                name: 'styles',
                started,
                changed: changedFilePath,
                src: lessEntryFile,
                projOut: projectOutputs,
                deployOut: deployOutputs,
            });
        });
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ JavaScript bundles                                                   ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 *  Additional Tasks to bundle es6 modules 
 */
async function bundleModules() {
  const bundle = await rollup({
    input: paths.jsModulesRoot + 'modules.mjs'
  });

  await bundle.write({
    file: paths.jsModulesRoot + 'modules.js',
    format: 'iife',
  });
}



/**
 * Bundles both iife and es6 modules in javascript/dev/modules into viewer.min.js
 *
 * @param {?string=} changedFilePath Optional path that triggered rebuild (for logging).
 * @returns {NodeJS.ReadWriteStream} Gulp pipeline.
 */
function bundleViewerJS(changedFilePath = null) {
    requireDeploymentDir();
    const started = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'viewer.min.js');
    const outDeploy = path.join(DEPLOYMENT_DIR, 'resources/javascript/dist/viewer.min.js');

    return gulp
        .src(
            [
                joinPosix(paths.jsModulesRoot, 'viewer', 'viewerJS.js'),
                joinPosix(paths.jsModulesRoot, 'viewer', 'viewerJS.helper.js'),
                joinPosix(paths.jsModulesRoot, 'viewer', 'viewerJS.*.js'),
                joinPosix(paths.jsModulesRoot, 'modules.js'),
                joinPosix(paths.jsModulesRoot, 'viewer', 'geoMap', 'viewerJS.geoMap.js'),
                joinPosix(paths.jsModulesRoot, 'viewer', 'geoMap', '*.js'),
                joinPosix(paths.jsModulesRoot, 'cms', 'cmsJS.js'),
                joinPosix(paths.jsModulesRoot, 'cms', 'cmsJS.*.js'),
                joinPosix(paths.jsModulesRoot, 'admin', 'adminJS.js'),
                joinPosix(paths.jsModulesRoot, 'admin', 'adminJS.*.js'),
                joinPosix(paths.jsModulesRoot, 'crowdsourcing', 'Crowdsourcing.js'),
                joinPosix(paths.jsModulesRoot, 'crowdsourcing', 'Crowdsourcing.Annotation.js'),
                joinPosix(paths.jsModulesRoot, 'crowdsourcing', 'Crowdsourcing.*.js'),
            ],
           {allowEmpty: true}
        )
        .pipe(guard())
        .pipe(concat('viewer.min.js'))
        .pipe(header(banner))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(safeDest('resources/javascript/dist'))
        .on('finish', () => {
            const deployOutputs = fs.existsSync(outDeploy) ? [outDeploy] : [];
            logTask({
                name: 'js_viewer',
                started,
                changed: changedFilePath,
                src: joinPosix(paths.jsModulesRoot, '{viewer,cms,admin,crowdsourcing}', '**', '*.js'),
                projOut: [outProj],
                deployOut: deployOutputs,
            });
        });
}

/**
 * Bundles statistics module into `statistics.min.js`.
 *
 * @param {?string=} changedFilePath Optional path that triggered rebuild (for logging).
 * @returns {NodeJS.ReadWriteStream} Gulp pipeline.
 */
function bundleStatisticsJS(changedFilePath = null) {
    requireDeploymentDir();
    const started = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'statistics.min.js');
    const outDeploy = path.join(DEPLOYMENT_DIR, 'resources/javascript/dist/statistics.min.js');

    return gulp
        .src(joinPosix(paths.jsModulesRoot, 'statistics', 'statistics.js'), {allowEmpty: true})
        .pipe(guard())
        .pipe(concat('statistics.min.js'))
        .pipe(header(banner))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(safeDest('resources/javascript/dist'))
        .on('finish', () => {
            const deployOutputs = fs.existsSync(outDeploy) ? [outDeploy] : [];
            logTask({
                name: 'js_statistics',
                started,
                changed: changedFilePath,
                src: joinPosix(paths.jsModulesRoot, 'statistics', 'statistics.js'),
                projOut: [outProj],
                deployOut: deployOutputs,
            });
        });
}

/**
 * Bundles browser support module into `browsersupport.min.js`.
 *
 * @param {?string=} changedFilePath Optional path that triggered rebuild (for logging).
 * @returns {NodeJS.ReadWriteStream} Gulp pipeline.
 */
function bundleBrowserSupportJS(changedFilePath = null) {
    requireDeploymentDir();
    const started = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'browsersupport.min.js');
    const outDeploy = path.join(DEPLOYMENT_DIR, 'resources/javascript/dist/browsersupport.min.js');

    return gulp
        .src(joinPosix(paths.jsModulesRoot, 'browsersupport', 'browsersupport.js'), {allowEmpty: true})
        .pipe(guard())
        .pipe(concat('browsersupport.min.js'))
        .pipe(header(banner))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(safeDest('resources/javascript/dist'))
        .on('finish', () => {
            const deployOutputs = fs.existsSync(outDeploy) ? [outDeploy] : [];
            logTask({
                name: 'js_browser',
                started,
                changed: changedFilePath,
                src: joinPosix(paths.jsModulesRoot, 'browsersupport', 'browsersupport.js'),
                projOut: [outProj],
                deployOut: deployOutputs,
            });
        });
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Riot tags                                                            ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Compiles Riot `.tag` files into `riot-tags.js` and mirrors to deployment.
 *
 * @param {?string=} changedFilePath Optional path that triggered rebuild (for logging).
 * @returns {NodeJS.ReadWriteStream} Gulp pipeline.
 */
function compileRiotTags(changedFilePath = null) {
    requireDeploymentDir();
    const started = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'riot-tags.js');
    const outDeploy = path.join(DEPLOYMENT_DIR, 'resources/javascript/dist/riot-tags.js');

    return gulp
        .src(joinPosix(paths.jsDevRoot, 'tags', '**', '*.tag'), {allowEmpty: true})
        .pipe(guard())
        .pipe(riot({compact: true}))
        .pipe(concat('riot-tags.js'))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(safeDest('resources/javascript/dist'))
        .on('finish', () => {
            const deployOutputs = fs.existsSync(outDeploy) ? [outDeploy] : [];
            logTask({
                name: 'riotTags',
                started,
                changed: changedFilePath,
                src: joinPosix(paths.jsDevRoot, 'tags', '**', '*.tag'),
                projOut: [outProj],
                deployOut: deployOutputs,
            });
        });
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Copy third-party dependencies                                        ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Copies declared third-party JS/CSS assets defined in `depsPathsJS/CSS`.
 * Supports optional flattening and special rename for Masonry bundles.
 *
 * @returns {Promise<void>|NodeJS.ReadWriteStream} Merged stream or resolved promise.
 */
function copyDependencies() {
    const started = process.hrtime.bigint();
    const streams = [];
    let copied = 0;

    // JS
    depsPathsJS.forEach((def) => {
        const srcOpts = {cwd: def.cwd, allowEmpty: true};
        const resolvedCwd = def.cwd ? path.resolve(def.cwd) : null;
        if (def.base) {
            srcOpts.base = path.isAbsolute(def.base)
                ? def.base
                : (resolvedCwd ? path.resolve(resolvedCwd, def.base) : path.resolve(def.base));
        } else if (resolvedCwd) {
            srcOpts.base = resolvedCwd;
        }

        let s = gulp.src(def.src, srcOpts).pipe(guard());
        if (def.flatten) s = s.pipe(rename({dirname: ''}));

        const destNorm = toPosix(def.dest);
        if (destNorm && /(^|\/)masonry\/?$/.test(destNorm)) {
            s = s.pipe(
                rename((p) => {
                    if (p.basename === 'masonry.pkgd.min' && p.extname === '.js') {
                        p.basename = 'masonry.min';
                    }
                })
            );
        }

        streams.push(
            s
                .pipe(
                    collectFiles(() => {
                        copied++;
                    })
                )
                .pipe(gulp.dest(def.dest))
        );
    });

    // CSS
    depsPathsCSS.forEach((def) => {
        const srcOpts = {cwd: def.cwd, allowEmpty: true};
        const resolvedCwd = def.cwd ? path.resolve(def.cwd) : null;
        if (def.base) {
            srcOpts.base = path.isAbsolute(def.base)
                ? def.base
                : (resolvedCwd ? path.resolve(resolvedCwd, def.base) : path.resolve(def.base));
        } else if (resolvedCwd) {
            srcOpts.base = resolvedCwd;
        }

        let s = gulp.src(def.src, srcOpts).pipe(guard());
        if (def.flatten) s = s.pipe(rename({dirname: ''}));
        streams.push(
            s
                .pipe(
                    collectFiles(() => {
                        copied++;
                    })
                )
                .pipe(gulp.dest(def.dest))
        );
    });

    if (streams.length === 0) return Promise.resolve();

    return merge(streams).on('finish', () => {
        logTask({
            name: 'copy-deps',
            started,
            genCount: 0,
            copyCount: copied,
        });
    });
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Full project → deployment mirror (one-shot)                          ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Mirrors the entire `staticRoot` tree into the deployment directory (one-shot).
 * Intended for initial syncs; subsequent changes are handled by the watcher.
 *
 * @returns {NodeJS.ReadWriteStream} Gulp pipeline.
 */
function fullSync() {
    requireDeploymentDir();
    const started = process.hrtime.bigint();
    let copied = 0;
    const copiedEntries = VERBOSE_SYNC ? [] : null;

    return gulp
        .src(joinPosix(paths.staticRoot, '**', '*'), { dot: true, allowEmpty: true })
        .pipe(guard())
        .pipe(
            collectFiles((file) => {
                copied++;
                if (copiedEntries && file && typeof file.path === 'string' && !file.isDirectory()) {
                    const rel = toPosix(path.relative(paths.staticRoot, file.path));
                    const dst = joinPosix(DEPLOYMENT_DIR, rel);
                    copiedEntries.push({src: rel, dst});
                }
            })
        )
        .pipe(gulp.dest(DEPLOYMENT_DIR))
        .on('finish', () => {
            logTask({
                name: 'sync-all',
                started,
                src: toPosix(path.resolve(paths.staticRoot)) + '/**/*',
                genCount: 0,
                copyCount: copied,
                extra: [
                    `dst: ${colors.blue(prettyPath(DEPLOYMENT_DIR))}`,
                    ...(copiedEntries
                        ? copiedEntries.map(({src, dst}) =>
                              `  • ${colors.green(src)} → ${colors.blue(dst)}`
                          )
                        : []),
                ],
            });
        });
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Java (Maven) helpers                                                 ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Resolves the Maven executable for a given project folder.
 * Prefers project-local wrapper (`mvnw`/`mvnw.cmd`) and falls back to system `mvn`.
 *
 * @param {string} cwd Project folder.
 * @returns {string} Executable path or command.
 */
function getMavenCmd(cwd) {
    const isWin = process.platform === 'win32';
    const wrapper = path.join(cwd, isWin ? 'mvnw.cmd' : 'mvnw');
    return fs.existsSync(wrapper) ? wrapper : (isWin ? 'mvn.cmd' : 'mvn');
}

/**
 * Formats milliseconds into a compact human string.
 *
 * @param {number} ms Milliseconds.
 * @returns {string} e.g. "850 ms", "3.5 s", "2m 05s", "1h 03m".
 */
function fmt(ms) {
    if (ms < 1000) return `${Math.round(ms)} ms`;
    const s = ms / 1000;
    if (s < 60) return `${s.toFixed(s < 10 ? 2 : 1)} s`;
    const m = Math.floor(s / 60);
    const rs = Math.round(s - m * 60);
    if (m < 60) return `${m}m ${String(rs).padStart(2, '0')}s`;
    const h = Math.floor(m / 60);
    const mm = m % 60;
    return `${h}h ${String(mm).padStart(2, '0')}m`;
}

/**
 * Runs Maven in `cwd` with the given goals (e.g., `['install']`) and resolves
 * with the elapsed time in milliseconds. Maven's own output streams to the console.
 *
 * @param {string} cwd Working directory for Maven.
 * @param {string[]=} goals Maven goals (default: []).
 * @returns {Promise<number>} Elapsed time in milliseconds.
 */
function runMaven(cwd, goals = []) {
    return new Promise((resolve, reject) => {
        if (!cwd || !fs.existsSync(cwd)) return reject(new Error(`Directory does not exist: ${cwd}`));
        const cmd = getMavenCmd(cwd);
        const t0 = process.hrtime.bigint();
        const child = spawn(cmd, goals, {cwd, stdio: 'inherit'});
        child.on('close', (code) => {
            const ms = Number(process.hrtime.bigint() - t0) / 1e6;
            if (code === 0) resolve(ms);
            else reject(new Error(`maven exited with code ${code}`));
        });
    });
}

/**
 * Builds core (`install`), then packages theme (`package`) and prints a compact summary.
 *
 * @returns {Promise<void>} Resolves when both Maven invocations complete.
 */
async function java() {
    assertDirExists('CORE_DIR', CORE_DIR);
    assertDirExists('THEME_DIR', THEME_DIR);
    const tAll = process.hrtime.bigint();
    const coreMs = await runMaven(CORE_DIR, ['install']);
    const themeMs = await runMaven(THEME_DIR, ['package']);
    const totalMs = Number(process.hrtime.bigint() - tAll) / 1e6;
    log(
        colors.bold(
            `[java] core [install] ${colors.cyan(fmt(coreMs))} · theme [package] ${colors.cyan(
                fmt(themeMs)
            )} · total ${colors.magenta(fmt(totalMs))}`
        )
    );
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Watch mode                                                           ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Starts watchers for JS bundles, styles, Riot tags, and static assets.
 * Changes are rebuilt and mirrored into the deployment folder where applicable.
 */
function watchMode() {
    requireDeploymentDir();
    // JS bundles
    gulp
        .watch([
            joinPosix(paths.jsModulesRoot, '{viewer,cms,admin,crowdsourcing}', '**', '*.js'),
            joinPosix(paths.jsModulesRoot, '**', '*.mjs')
        ])
        .on('change', (p) => {
            bundleModules();
            bundleViewerJS(p);
        });

    gulp.watch(joinPosix(paths.jsModulesRoot, 'statistics', '**', '*.js')).on('change', (p) =>
        bundleStatisticsJS(p)
    );
    gulp.watch(joinPosix(paths.jsModulesRoot, 'browsersupport', '**', '*.js')).on('change', (p) =>
        bundleBrowserSupportJS(p)
    );

    // Styles & tags
    gulp.watch(joinPosix(paths.lessRoot, '**', '*.less')).on('change', (p) => buildStyles(p));
    gulp.watch(joinPosix(paths.jsDevRoot, 'tags', '**', '*.tag')).on('change', (p) => compileRiotTags(p));

    // Static assets (no dist CSS/JS)
    const staticGlobs = [
        joinPosix(paths.staticRoot, '*.xhtml'),
        joinPosix(paths.staticRoot, '*.xml'),
        joinPosix(paths.staticRoot, '*.xls'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.xhtml'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.html'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.jpg'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.jpeg'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.png'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.svg'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.gif'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.ico'),
        joinPosix(paths.staticRoot, 'resources', '**', '*.css'),
        '!' + joinPosix(paths.staticRoot, 'resources', 'css', 'dist', '**', '*.css'),
    ];

    const staticWatcher = gulp.watch(staticGlobs, {ignoreInitial: true});

    function mirrorStatic(filePath) {
        const started = process.hrtime.bigint();
        const rel = toPosix(path.relative(paths.staticRoot, filePath));
        const dst = path.join(DEPLOYMENT_DIR, rel);
        return gulp
            .src(filePath, {base: paths.staticRoot})
            .pipe(guard())
            .pipe(gulp.dest(DEPLOYMENT_DIR))
            .on('finish', () => {
                logTask({
                    name: 'static',
                    started,
                    changed: filePath,
                    src: rel,
                    projOut: [],
                    deployOut: [dst],
                    genCount: 0,
                    copyCount: 1,
                });
            });
    }

    staticWatcher.on('add', mirrorStatic);
    staticWatcher.on('change', mirrorStatic);
    staticWatcher.on('unlink', async (filePath) => {
        const rel = toPosix(path.relative(paths.staticRoot, filePath));
        const targetPath = path.join(DEPLOYMENT_DIR, rel);
        try {
            const {default: del} = await import('del');
            await del(targetPath, {force: true});
            logBlock('static', [
                `deleted: ${colors.green(filePath)}`,
                `dst: ${colors.blue(targetPath)}`,
                colors.yellow('• removed'),
            ]);
        } catch (err) {
            console.warn('[WARN] Could not delete file from deploy dir:', err.message);
        }
    });
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Debug helper                                                         ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

/**
 * Prints resolved directories and environment override hints.
 *
 * @param {Function} cb Gulp callback.
 */
function printTargets(cb) {
    const home = os.homedir();
    const exists = (p) => p && fs.existsSync(p);
    const pretty = (p) => (p ? toPosix(p.replace(home, '~')) : '(none)');
    const mark = (p) => (exists(p) ? colors.green('✓') : colors.red('✗'));
    const row = (label, p) => `${colors.white(label.padEnd(14))} ${mark(p)}  ${colors.blue(pretty(p))}`;

    logBlock('targets', [
        `platform: ${colors.cyan(process.platform)}  node: ${colors.cyan(process.version)}`,
        row('DEPLOYMENT_DIR', DEPLOYMENT_DIR),
        row('CORE_DIR', CORE_DIR),
        row('THEME_DIR', THEME_DIR),
        colors.gray('hint: GV_CORE_DIR / GV_THEME_DIR override repo detection'),
        colors.gray('      GV_VIEWER_CFG / GV_GULP_CFG override config paths'),
    ]);
    cb();
}

/* ╔══════════════════════════════════════════════════════════════════════╗
   ║ Task composition & exports                                           ║
   ╚══════════════════════════════════════════════════════════════════════╝ */

const buildJS = gulp.series(bundleModules, bundleViewerJS, bundleStatisticsJS, bundleBrowserSupportJS);
const buildAll = gulp.series(gulp.parallel(buildStyles, buildJS, compileRiotTags));

exports.build = buildAll;
exports.dev = gulp.series(fullSync, watchMode);
exports['copy-deps'] = gulp.series(buildIcons, copyDependencies);
exports['sync-all'] = fullSync;
exports.target = printTargets;
exports.java = java;
exports.icons = buildIcons;

/* ── Task exports ────────────────────────────────────────────────────────────────────────────
   - npm run build      → builds styles, JS bundles, riot tags, icons
   - npm run dev        → builds icons, full project → deploy mirror once, then starts watchers
   - npm run copyDeps   → copies declared 3rd-party assets
   - npm run icons      → rebuilds Tabler SVG sprite assets
   - npm run sync       → one-shot full project → deploy mirror
   - npm run target     → prints resolved paths / env overrides
   - npm run java       → maven: core [install] + theme [package]
──────────────────────────────────────────────────────────────────────────────────────────────── */
