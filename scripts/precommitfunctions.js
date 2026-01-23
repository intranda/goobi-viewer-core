#!/usr/bin/env node

const { execSync } = require("child_process");
const path = require("path");

// Pfad zum lokalen Prettier (OS-unabhängig)
const isWindows = process.platform === "win32";
const prettierBin = path.join(
  __dirname,
  "../goobi-viewer-core/node_modules/.bin",
  isWindows ? "prettier.cmd" : "prettier",
);

// Nur staged Dateien (die tatsächlich committet werden)
const stagedFiles = execSync(
  "git diff --cached --name-only --diff-filter=ACMR",
  { encoding: "utf8" },
)
  .split(/\r?\n/)
  .map((f) => f.trim())
  .filter((f) => f);

if (stagedFiles.length === 0) {
  console.log("No staged files detected.");
  process.exit(0);
}

// Prettier auf relevante Dateitypen ausführen
const prettierFiles = stagedFiles.filter((f) =>
  /\.(js|ts|jsx|tsx|html|xhtml|css|less|json|md)$/.test(f),
);

if (prettierFiles.length > 0) {
  console.log("Running Prettier on staged files:");
  prettierFiles.forEach((f) => console.log("  ", f));

  execSync(
    `"${prettierBin}" --write ${prettierFiles.map((f) => `"${f}"`).join(" ")}`,
    {
      stdio: "inherit",
    },
  );

  // Formatierte Dateien wieder stagen
  execSync(`git add ${prettierFiles.map((f) => `"${f}"`).join(" ")}`);
}

console.log("✅ Pre-commit hook finished successfully.");
