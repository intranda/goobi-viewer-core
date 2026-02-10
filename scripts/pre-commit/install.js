#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

// Hook template
const HOOK_TEMPLATE = `#!/bin/sh
# Pre-commit hook - runs Prettier on staged files

# Get the git root directory
GIT_ROOT="$(git rev-parse --show-toplevel)"

# Run the pre-commit script with Node
node "$GIT_ROOT/scripts/pre-commit/formatter.js"
`;

/**
 * Gets the paths for hook installation
 * @param {string} baseDir - Base directory (usually __dirname)
 * @returns {Object} Object with gitRoot, hooksDir and preCommitHook paths
 */
function getHookPaths(baseDir) {
  const gitRoot = path.join(baseDir, "../..");
  const hooksDir = path.join(gitRoot, ".git", "hooks");
  const preCommitHook = path.join(hooksDir, "pre-commit");
  return { gitRoot, hooksDir, preCommitHook };
}

/**
 * Ensures the hooks directory exists
 * @param {string} hooksDir - Path to hooks directory
 * @param {Object} fsModule - Filesystem module (mockable for tests)
 */
function ensureHooksDir(hooksDir, fsModule = fs) {
  if (!fsModule.existsSync(hooksDir)) {
    fsModule.mkdirSync(hooksDir, { recursive: true });
  }
}

/**
 * Writes the hook to file
 * @param {string} hookPath - Path to hook file
 * @param {string} content - Hook content
 * @param {Object} fsModule - Filesystem module (mockable for tests)
 */
function writeHook(hookPath, content, fsModule = fs) {
  fsModule.writeFileSync(hookPath, content, { mode: 0o755 });
}

/**
 * Checks if a hook is already installed
 * @param {string} hookPath - Path to hook file
 * @param {Object} fsModule - Filesystem module (mockable for tests)
 * @returns {boolean} true if hook exists
 */
function hookExists(hookPath, fsModule = fs) {
  return fsModule.existsSync(hookPath);
}

/**
 * Reads the content of an existing hook
 * @param {string} hookPath - Path to hook file
 * @param {Object} fsModule - Filesystem module (mockable for tests)
 * @returns {string} Hook content
 */
function readHook(hookPath, fsModule = fs) {
  return fsModule.readFileSync(hookPath, "utf8");
}

/**
 * Installs the pre-commit hook
 * @param {Object} options - Options
 * @param {string} options.baseDir - Base directory
 * @param {Object} options.fsModule - Filesystem module (mockable for tests)
 * @param {Function} options.log - Logging function
 * @returns {Object} Installation result
 */
function installHooks(options = {}) {
  const { baseDir = __dirname, fsModule = fs, log = console.log } = options;

  const { hooksDir, preCommitHook } = getHookPaths(baseDir);

  try {
    log("Installing git hooks...");

    ensureHooksDir(hooksDir, fsModule);
    writeHook(preCommitHook, HOOK_TEMPLATE, fsModule);

    log("Git hooks installed successfully.");
    log("Hook location:", preCommitHook);

    return { success: true, hookPath: preCommitHook };
  } catch (error) {
    log("Failed to install git hooks:", error.message);
    return { success: false, error: error.message };
  }
}

// Export for tests
module.exports = {
  HOOK_TEMPLATE,
  getHookPaths,
  ensureHooksDir,
  writeHook,
  hookExists,
  readHook,
  installHooks,
};

// Only execute when called directly
if (require.main === module) {
  const result = installHooks();
  if (!result.success) {
    process.exit(0); // Don't fail npm install
  }
}
