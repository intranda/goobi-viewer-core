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
const colors = require('ansi-colors');
const log = require('fancy-log');

const {depsPathsJS, depsPathsCSS} = require('./gulp/depsPaths');

/* ========== Paths ==========
 * Note: jsLibsFolder / cssLibsFolder are not defined here,
 * because those are already handled via depsPathsJS / depsPathsCSS
 * (see ./gulp/depsPaths.js). No direct references in the gulpfile needed.
 */
const paths = {
    jsDevRoot: 'src/main/resources/META-INF/resources/resources/javascript/dev/',
    jsModulesRoot: 'src/main/resources/META-INF/resources/resources/javascript/dev/modules/',
    jsDistRoot: 'src/main/resources/META-INF/resources/resources/javascript/dist/',
    cssRoot: 'src/main/resources/META-INF/resources/resources/css/',
    cssDistRoot: 'src/main/resources/META-INF/resources/resources/css/dist/',
    lessRoot: 'src/main/resources/META-INF/resources/resources/css/less/',
    staticRoot: 'src/main/resources/META-INF/resources'
};

/* ========== Banner ========== */
const banner = `/*!
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 * - http://www.intranda.com
 * - http://digiverso.com
 * GPLv2 or later. NO WARRANTY.
 */\n`;

/* ========== Tomcat target detection ========== */
function resolveTomcatDir() {
    try {
        const home = os.homedir();
        const raw = fs.readFileSync(path.join(home, '.config', 'grunt_userconfig.json'), 'utf-8');
        const cfg = JSON.parse(raw);

        const isWin = process.platform.toLowerCase().startsWith('win');
        const viewerCfgPath = isWin
            ? 'c:/opt/digiverso/viewer/config/config_viewer.xml'
            : '/opt/digiverso/viewer/config/config_viewer.xml';

        const xmlString = fs.readFileSync(viewerCfgPath, 'utf-8');
        const viewerConfig = XML.parse(xmlString);
        const theme = viewerConfig.viewer.theme;

        if (theme.specialName && theme.specialName.length) {
            return path.join(cfg.tomcat_dir, `goobi-viewer-theme-${theme.specialName}`);
        } else if (theme.deployedTargetFolder === 'true') {
            return path.join(
                home, 'git', 'goobi-viewer',
                'goobi-viewer-theme-reference', 'goobi-viewer-theme-reference',
                'target', 'viewer'
            );
        } else {
            return path.join(cfg.tomcat_dir, `goobi-viewer-theme-${theme.mainTheme}`);
        }
    } catch (err) {
        console.error('[ERROR] Could not resolve Tomcat directory: ' + err.message);
        process.exit(1);
    }
}

const TOMCAT_DIR = resolveTomcatDir();

// This copies generated files (e.g. CSS/JS bundles) directly into the
// running Tomcat webapp so changes are immediately available without redeploy.
const writeToTomcat = (subPath) => gulp.dest(path.join(TOMCAT_DIR, subPath));

/* ========== helpers ========== */
const elapsedMs = (t0) => ((Number(process.hrtime.bigint() - t0) / 1e6) || 0).toFixed(0) + ' ms';

function logBlock(title, lines) {
    console.log(colors.white(`\n[${title}]`));
    lines.forEach(l => console.log('  ' + l));
}

function collectFiles(push) {
    return through.obj(function (file, _, cb) {
        try {
            push(file);
        } catch {
        }
        cb(null, file);
    });
}

function logTaskSummary(task, {changedPath = null, srcPath, projectOutputs = [], tomcatOutputs = [], startedAt}) {
    logBlock(task, [
        ...(changedPath ? [`changed: ${colors.green(changedPath)}`] : []),
        `src: ${colors.green(srcPath)}`,
        `→ project:`,
        ...projectOutputs.map(p => '  • ' + colors.blue(p)),
        ...(tomcatOutputs.length
            ? [`→ tomcat:`, ...tomcatOutputs.map(p => '  • ' + colors.blue(p))]
            : []),
        colors.green('✓ ') +
        colors.white(`${projectOutputs.length} generated · ${tomcatOutputs.length} copied · 0 errors · `) +
        colors.magenta(elapsedMs(startedAt))
    ]);
}

/* ========== Styles (LESS → CSS + map) ========== */
function buildStyles(changedFilePath = null) {
    const startTimeStamp = process.hrtime.bigint();
    const lessEntryFile = path.join(paths.lessRoot, 'constructor.less');

    const projectOutputs = [];
    const tomcatOutputs = [];

    // Collect generated output paths for logging
    const collectProjectOutputs = collectFiles((file) => {
        const base = path.resolve(paths.cssDistRoot);
        const fname = path.basename(file.path);
        projectOutputs.push(path.join(base, fname));
    });
    // Collect corresponding paths when copying into Tomcat webapp
    const collectTomcatOutputs = collectFiles((file) => {
        const rel = path.relative(paths.staticRoot, file.path).replace(/\\/g, '/');
        tomcatOutputs.push(path.join(TOMCAT_DIR, rel));
    });

    return gulp.src(lessEntryFile)
        .pipe(sourcemaps.init())
        .pipe(less({compress: true}))
        .pipe(header(banner))
        .pipe(rename('viewer.min.css'))
        .pipe(sourcemaps.write('.', {includeContent: true, sourceRoot: '/viewer/resources/css/less'}))

        .pipe(collectProjectOutputs)
        .pipe(gulp.dest(paths.cssDistRoot))

        .pipe(collectTomcatOutputs)
        .pipe(writeToTomcat('resources/css/dist'))

        .on('finish', () => {
            logTaskSummary('styles', {
                changedPath: changedFilePath,
                srcPath: lessEntryFile,
                projectOutputs,
                tomcatOutputs,
                startedAt: startTimeStamp
            });
        });
}

/* ========== JavaScript bundles ========== */
function bundleViewerJS(changedFilePath = null) {
    const startTimeStamp = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'viewer.min.js');
    const outTomcat = TOMCAT_DIR
        ? path.join(TOMCAT_DIR, 'resources/javascript/dist/viewer.min.js')
        : null;

    // These files are concatenated into viewer.min.js when the task runs.
    return gulp.src([
        path.join(paths.jsModulesRoot, 'viewer/viewerJS.js'),
        path.join(paths.jsModulesRoot, 'viewer/viewerJS.helper.js'),
        path.join(paths.jsModulesRoot, 'viewer/viewerJS.*.js'),
        path.join(paths.jsModulesRoot, 'viewer/geoMap/viewerJS.geoMap.js'),
        path.join(paths.jsModulesRoot, 'viewer/geoMap/*.js'),
        path.join(paths.jsModulesRoot, 'cms/cmsJS.js'),
        path.join(paths.jsModulesRoot, 'cms/cmsJS.*.js'),
        path.join(paths.jsModulesRoot, 'admin/adminJS.js'),
        path.join(paths.jsModulesRoot, 'admin/adminJS.*.js'),
        path.join(paths.jsModulesRoot, 'crowdsourcing/Crowdsourcing.js'),
        path.join(paths.jsModulesRoot, 'crowdsourcing/Crowdsourcing.*.js')
    ], {allowEmpty: true})
        .pipe(concat('viewer.min.js'))
        .pipe(header(banner))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(writeToTomcat('resources/javascript/dist'))
        .on('finish', () => {
            logTaskSummary('js_viewer', {
                changedPath: changedFilePath,
                srcPath: path.join(paths.jsModulesRoot, '{viewer,cms,admin,crowdsourcing}/**/*.js'),
                projectOutputs: [outProj],
                tomcatOutputs: outTomcat ? [outTomcat] : [],
                startedAt: startTimeStamp
            });
        });
}

function bundleStatisticsJS(changedFilePath = null) {
    const startTimeStamp = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'statistics.min.js');
    const outTomcat = TOMCAT_DIR
        ? path.join(TOMCAT_DIR, 'resources/javascript/dist/statistics.min.js')
        : null;

    return gulp.src(path.join(paths.jsModulesRoot, 'statistics/statistics.js'), {allowEmpty: true})
        .pipe(concat('statistics.min.js'))
        .pipe(header(banner))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(writeToTomcat('resources/javascript/dist'))
        .on('finish', () => {
            logTaskSummary('js_statistics', {
                changedPath: changedFilePath,
                srcPath: path.join(paths.jsModulesRoot, 'statistics/statistics.js'),
                projectOutputs: [outProj],
                tomcatOutputs: outTomcat ? [outTomcat] : [],
                startedAt: startTimeStamp
            });
        });
}

function bundleBrowserSupportJS(changedFilePath = null) {
    const startTimeStamp = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'browsersupport.min.js');
    const outTomcat = TOMCAT_DIR
        ? path.join(TOMCAT_DIR, 'resources/javascript/dist/browsersupport.min.js')
        : null;

    return gulp.src(path.join(paths.jsModulesRoot, 'browsersupport/browsersupport.js'), {allowEmpty: true})
        .pipe(concat('browsersupport.min.js'))
        .pipe(header(banner))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(writeToTomcat('resources/javascript/dist'))
        .on('finish', () => {
            logTaskSummary('js_browser', {
                changedPath: changedFilePath,
                srcPath: path.join(paths.jsModulesRoot, 'browsersupport/browsersupport.js'),
                projectOutputs: [outProj],
                tomcatOutputs: outTomcat ? [outTomcat] : [],
                startedAt: startTimeStamp
            });
        });
}

/* ========== Riot tags ========== */
function compileRiotTags(changedFilePath = null) {
    const startTimeStamp = process.hrtime.bigint();
    const outProj = path.resolve(paths.jsDistRoot, 'riot-tags.js');
    const outTomcat = TOMCAT_DIR
        ? path.join(TOMCAT_DIR, 'resources/javascript/dist/riot-tags.js')
        : null;

    return gulp.src(path.join(paths.jsDevRoot, 'tags/**/*.tag'), {allowEmpty: true})
        .pipe(riot({compact: true}))
        .pipe(concat('riot-tags.js'))
        .pipe(gulp.dest(paths.jsDistRoot))
        .pipe(writeToTomcat('resources/javascript/dist'))
        .on('finish', () => {
            logTaskSummary('riotTags', {
                changedPath: changedFilePath,
                srcPath: path.join(paths.jsDevRoot, 'tags/**/*.tag'),
                projectOutputs: [outProj],
                tomcatOutputs: outTomcat ? [outTomcat] : [],
                startedAt: startTimeStamp
            });
        });
}

/* ========== Full project → Tomcat sync ========== */
function fullSync() {
    const startTimeStamp = process.hrtime.bigint();
    let copied = 0;

    return gulp.src(path.join(paths.staticRoot, '**/*'), {dot: true, allowEmpty: true})
        .pipe(collectFiles(() => {
            copied++;
        }))
        .pipe(gulp.dest(TOMCAT_DIR))
        .on('finish', () => {
            logBlock('sync-all', [
                `src: ${colors.green(path.resolve(paths.staticRoot)) + '/**/*'}`,
                `dst: ${colors.blue(TOMCAT_DIR)}`,
                colors.green('✓ ') + colors.white(`${copied} copied · 0 errors · `) + colors.magenta(elapsedMs(startTimeStamp))
            ]);
        });
}

/* ========== Copy third-party deps ========== */
function copyDependencies() {
    const startTimeStamp = process.hrtime.bigint();
    const streams = [];
    let copied = 0;

    // JS
    depsPathsJS.forEach(def => {
        let s = gulp.src(def.src, {cwd: def.cwd, allowEmpty: true});
        if (def.flatten) s = s.pipe(rename({dirname: ''}));
        if (def.dest && /\/masonry\/$/.test(def.dest)) {
            s = s.pipe(rename(p => {
                if (p.basename === 'masonry.pkgd.min' && p.extname === '.js') p.basename = 'masonry.min';
            }));
        }
        streams.push(s.pipe(collectFiles(() => {
            copied++;
        })).pipe(gulp.dest(def.dest)));
    });

    // CSS
    depsPathsCSS.forEach(def => {
        let s = gulp.src(def.src, {cwd: def.cwd, allowEmpty: true});
        if (def.flatten) s = s.pipe(rename({dirname: ''}));
        streams.push(s.pipe(collectFiles(() => {
            copied++;
        })).pipe(gulp.dest(def.dest)));
    });

    if (streams.length === 0) return Promise.resolve();

    return merge(streams).on('finish', () => {
        logBlock('copy-deps', [
            colors.green('✓ ') + colors.white(`${copied} files copied · 0 errors · `) + colors.magenta(elapsedMs(startTimeStamp))
        ]);
    });
}

/* ========== Watch mode ========== */
function watchMode() {
    // Viewer bundle reacts to multiple module folders
    const viewerGlob = path.join(paths.jsModulesRoot, '{viewer,cms,admin,crowdsourcing}/**/*.js');
    gulp.watch(viewerGlob).on('change', (p) => bundleViewerJS(p));

    // Separate bundles
    gulp.watch(path.join(paths.jsModulesRoot, 'statistics/**/*.js')).on('change', (p) => bundleStatisticsJS(p));
    gulp.watch(path.join(paths.jsModulesRoot, 'browsersupport/**/*.js')).on('change', (p) => bundleBrowserSupportJS(p));

    // LESS → CSS (+map) + Tomcat
    gulp.watch(path.join(paths.lessRoot, '**/*.less')).on('change', (p) => buildStyles(p));

    // Riot tags
    gulp.watch(path.join(paths.jsDevRoot, 'tags/**/*.tag')).on('change', (p) => compileRiotTags(p));

    // Static assets only (no CSS/JS from dist)
    const staticGlobs = [
        path.join(paths.staticRoot, '*.xhtml'),
        path.join(paths.staticRoot, '*.xml'),
        path.join(paths.staticRoot, '*.xls'),
        path.join(paths.staticRoot, 'resources/**/*.xhtml'),
        path.join(paths.staticRoot, 'resources/**/*.html'),
        path.join(paths.staticRoot, 'resources/**/*.jpg'),
        path.join(paths.staticRoot, 'resources/**/*.jpeg'),
        path.join(paths.staticRoot, 'resources/**/*.png'),
        path.join(paths.staticRoot, 'resources/**/*.svg'),
        path.join(paths.staticRoot, 'resources/**/*.gif'),
        path.join(paths.staticRoot, 'resources/**/*.ico')
    ];

    const staticWatcher = gulp.watch(staticGlobs, {ignoreInitial: true});

    function mirrorStatic(filePath) {
        const rel = path.relative(paths.staticRoot, filePath).replace(/\\/g, '/');
        const dst = path.join(TOMCAT_DIR, rel);
        return gulp.src(filePath, {base: paths.staticRoot})
            .pipe(gulp.dest(TOMCAT_DIR))
            .on('finish', () => {
                logBlock('static', [
                    `changed: ${colors.green(filePath)}`,
                    `dst: ${colors.blue(dst)}`,
                    colors.green('✓ copied')
                ]);
            });
    }

    staticWatcher.on('add', mirrorStatic);
    staticWatcher.on('change', mirrorStatic);

    staticWatcher.on('unlink', async (filePath) => {
        const rel = path.relative(paths.staticRoot, filePath).replace(/\\/g, '/');
        const targetPath = path.join(TOMCAT_DIR, rel);
        try {
            const {default: del} = await import('del');
            await del(targetPath, {force: true});
            logBlock('static', [
                `deleted: ${colors.green(filePath)}`,
                `dst: ${colors.blue(targetPath)}`,
                colors.yellow('• removed')
            ]);
        } catch (err) {
            console.warn('[WARN] Could not delete file from Tomcat:', err.message);
        }
    });
}

/* ========== Debug helper ========== */
function printTargetDir(cb) {
    log('[gulp] TOMCAT_DIR =', TOMCAT_DIR || '(none)');
    cb();
}

/* ========== Task composition & exports ========== */
const buildJS = gulp.series(bundleViewerJS, bundleStatisticsJS, bundleBrowserSupportJS);
const buildAll = gulp.series(gulp.parallel(buildStyles, buildJS, compileRiotTags));

// npm run build
// Builds all assets (CSS, JS bundles, Riot tags)
exports.build = buildAll;

// npm run dev
// Starts watch mode (equivalent to "grunt watch")
// Watches .less, .js, .tag and static files, rebuilds and copies into Tomcat automatically
exports.dev = watchMode;

// npm run devsync
// Runs a full project → Tomcat sync first, then starts watch mode
exports.devsync = gulp.series(fullSync, watchMode);

// npm run copy-deps
// Copies external dependencies (JS/CSS libs) into the project folders
exports['copy-deps'] = copyDependencies;

// npm run sync-all
// Copies the entire staticRoot into the Tomcat target directory
exports['sync-all'] = fullSync;

// npm run target
// Prints the resolved Tomcat target directory in the console
exports.target = printTargetDir;