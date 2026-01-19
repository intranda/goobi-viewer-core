#!/usr/bin/env node

const { execSync } = require("child_process");

// Alle geänderten Dateien (staged oder unstaged)
const changedFiles = execSync(
  "git diff --name-only --diff-filter=ACMR",
  { encoding: "utf8" }
)
  .split(/\r?\n/)
  .map(f => f.trim())
  .filter(f => f);

if (changedFiles.length === 0) {
  console.log("No changed files detected.");
  process.exit(0);
}

console.log("Changed files detected:", changedFiles);

// Prettier auf relevante Dateitypen ausführen
const prettierFiles = changedFiles.filter(f =>
  /\.(js|ts|jsx|tsx|html|xhtml|css|json|md)$/.test(f)
);

if (prettierFiles.length > 0) {
  console.log("Running Prettier on changed files:");
  prettierFiles.forEach(f => console.log("  ", f));

  execSync(`npx prettier --write ${prettierFiles.map(f => `"${f}"`).join(" ")}`, {
    stdio: "inherit",
  });

  // Dateien wieder zum Commit hinzufügen
  execSync(`git add ${prettierFiles.map(f => `"${f}"`).join(" ")}`);
}

// Optional: ESLint nur auf gestagte JS/TS-Dateien
const stagedJsFiles = execSync("git diff --cached --name-only", { encoding: "utf8" })
  .split(/\r?\n/)
  .filter(f => /\.(js|ts|jsx|tsx)$/.test(f))
  .filter(f => f);

if (stagedJsFiles.length > 0) {
  console.log("Running ESLint on staged JS/TS files:");
  stagedJsFiles.forEach(f => console.log("  ", f));

  execSync(`npx eslint --fix ${stagedJsFiles.map(f => `"${f}"`).join(" ")}`, {
    stdio: "inherit",
  });

  // Gestagte Dateien erneut adden
  execSync(`git add ${stagedJsFiles.map(f => `"${f}"`).join(" ")}`);
}

console.log("✅ Pre-commit hook finished successfully.");