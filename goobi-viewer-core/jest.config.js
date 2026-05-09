/**
 * Jest configuration for the goobi-viewer-core JavaScript test suite.
 *
 * Two test "projects" coexist:
 *   - "browser-modules": jsdom environment, for the JS modules that ship in
 *     the WAR (see src/main/resources/.../javascript/dev/modules/...).
 *     These were originally written against a real browser; jsdom gives us
 *     window/document/localStorage without spawning Chrome.
 *   - "node-scripts":    node environment, for the pre-commit helper
 *     scripts under scripts/pre-commit/.
 *
 * Run from this directory (goobi-viewer-core/):
 *   npx jest                      # full suite
 *   npx jest --selectProjects browser-modules
 *   npx jest --coverage           # writes target/jest-coverage/lcov.info
 *
 * Coverage output goes under target/ so `mvn clean` removes it.
 */

const path = require('path');

// Both projects resolve paths against the repository root (one level up from
// this package); that way the test paths look the same as in the file system.
const repoRoot = path.resolve(__dirname, '..');

module.exports = {
    rootDir: repoRoot,

    // Aggregated coverage for both projects in a single Maven-friendly location.
    coverageDirectory: path.join(__dirname, 'target/jest-coverage'),
    coverageReporters: ['text-summary', 'lcov'],
    collectCoverageFrom: [
        'goobi-viewer-core/src/main/resources/META-INF/resources/resources/javascript/dev/modules/**/*.js',
        'scripts/pre-commit/*.js',
        '!**/__tests__/**',
        '!**/node_modules/**',
    ],

    // jest-junit writes a JUnit-format XML next to Java's surefire reports
    // so the existing Jenkinsfile glob (`**/target/surefire-reports/*.xml`)
    // and GitHub Actions "junit" report consumers pick it up automatically.
    // jest-junit option values must be strings (it stringifies booleans).
    reporters: [
        'default',
        [
            // Absolute path because Jest resolves reporters relative to
            // rootDir (the repo root), where there is no node_modules.
            require.resolve('jest-junit'),
            {
                outputDirectory: path.join(__dirname, 'target/surefire-reports'),
                outputName: 'TEST-jest-junit.xml',
                // Suites named by source file (mirrors surefire's class-per-file).
                suiteNameTemplate: '{filepath}',
                classNameTemplate: '{classname}',
                titleTemplate: '{title}',
                ancestorSeparator: ' › ',
                addFileAttribute: 'true',
            },
        ],
        [
            // jest-sonar writes Sonar's "Generic Test Execution" XML format
            // (different from JUnit). Consumed by SonarCloud via
            // sonar.testExecutionReportPaths in pom.xml.
            //
            // NOT under target/sonar/: Sonar's Maven plugin uses that
            // directory as its own working directory and wipes it on start,
            // which would delete our report before it gets read.
            require.resolve('jest-sonar'),
            {
                outputDirectory: path.join(__dirname, 'target/jest-sonar'),
                outputName: 'test-execution-report.xml',
                // Paths in the report point back to absolute file system paths,
                // which is what Sonar matches against to attribute test results.
                reportedFilePath: 'absolute',
            },
        ],
    ],

    projects: [
        {
            displayName: 'browser-modules',
            testEnvironment: 'jsdom',
            rootDir: repoRoot,
            testMatch: [
                '<rootDir>/goobi-viewer-core/src/main/resources/META-INF/resources/resources/javascript/dev/modules/**/__tests__/**/*.test.js',
            ],
            // Binds real jQuery to jsdom's window for every test file in
            // this project. See jest-setup-browser.js for the rationale and
            // a HOWTO for adding new tests.
            setupFiles: [path.join(__dirname, 'jest-setup-browser.js')],
        },
        {
            displayName: 'node-scripts',
            testEnvironment: 'node',
            rootDir: repoRoot,
            testMatch: ['<rootDir>/scripts/pre-commit/__tests__/**/*.test.js'],
        },
    ],
};
