#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

// Pfad zum Git-Root
const gitRoot = path.join(__dirname, "..");
const hooksDir = path.join(gitRoot, ".git", "hooks");
const preCommitHook = path.join(hooksDir, "pre-commit");

// Einfacher Hook - ruft Node direkt auf (funktioniert auf allen OS)
const hookContent = `#!/bin/sh
# Pre-commit hook - runs Prettier on staged files

# Get the git root directory
GIT_ROOT="$(git rev-parse --show-toplevel)"

# Run the pre-commit script with Node
node "\$GIT_ROOT/scripts/precommitfunctions.js"
`;

try {
  console.log("Installing git hooks...");

  // Ensure hooks directory exists
  if (!fs.existsSync(hooksDir)) {
    fs.mkdirSync(hooksDir, { recursive: true });
  }

  // Write the hook file
  fs.writeFileSync(preCommitHook, hookContent, { mode: 0o755 });

  console.log("Git hooks installed successfully.");
  console.log("Hook location:", preCommitHook);
} catch (error) {
  console.error("Failed to install git hooks:", error.message);
  // Don't fail npm install if hooks can't be installed
  process.exit(0);
}
