#!/usr/bin/env node

const { execSync } = require("child_process");
const path = require("path");

// Pfad zum Git-Root (eine Ebene höher als goobi-viewer-core)
const gitRoot = path.join(__dirname, "..");

// Pfad zum lokalen lefthook (OS-unabhängig)
const isWindows = process.platform === "win32";
const lefthookBin = path.join(
  __dirname,
  "../goobi-viewer-core/node_modules/.bin",
  isWindows ? "lefthook.cmd" : "lefthook",
);

try {
  console.log("Installing git hooks...");
  execSync(`"${lefthookBin}" install`, {
    cwd: gitRoot,
    stdio: "inherit",
  });
  console.log("Git hooks installed successfully.");
} catch (error) {
  console.error("Failed to install git hooks:", error.message);
  // Don't fail npm install if hooks can't be installed
  process.exit(0);
}
