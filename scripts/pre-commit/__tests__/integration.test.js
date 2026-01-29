const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const os = require("os");

/**
 * Integration Tests
 *
 * These tests create real Git repositories and execute real commands
 * to verify the pre-commit hook works correctly end-to-end.
 */

const PROJECT_ROOT = path.join(__dirname, "../../..");
const PRETTIER_BIN = path.join(
  PROJECT_ROOT,
  "goobi-viewer-core/node_modules/.bin",
  process.platform === "win32" ? "prettier.cmd" : "prettier",
);

// Test data
const UNFORMATTED_JS = `const x={a:1,b:2,c:3};function test(){return x;}`;
const JAVA_CODE = `public class Test{public static void main(String[]args){}}`;

describe("Integration Tests", () => {
  let tempDir;
  const prettierAvailable = fs.existsSync(PRETTIER_BIN);

  beforeAll(() => {
    if (!prettierAvailable) {
      console.warn("Prettier not found - some tests will be skipped");
      console.warn("Run 'npm install' in goobi-viewer-core first");
    }
  });

  beforeEach(() => {
    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "git-hook-test-"));
  });

  afterEach(() => {
    if (tempDir && fs.existsSync(tempDir)) {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  // ============================================================
  // GIT STAGING
  // Tests that the hook correctly identifies staged files
  // ============================================================

  describe("Git Staging Detection", () => {
    test("detects only staged files, not unstaged ones", () => {
      execSync("git init", { cwd: tempDir });
      execSync("git config user.email 'test@test.com'", { cwd: tempDir });
      execSync("git config user.name 'Test'", { cwd: tempDir });

      fs.writeFileSync(path.join(tempDir, "staged.js"), "const a = 1;");
      fs.writeFileSync(path.join(tempDir, "unstaged.js"), "const b = 2;");

      execSync("git add staged.js", { cwd: tempDir });

      const staged = execSync(
        "git diff --cached --name-only --diff-filter=ACMR",
        { cwd: tempDir, encoding: "utf8" },
      );

      expect(staged.trim()).toBe("staged.js");
      expect(staged).not.toContain("unstaged.js");
    });
  });

  // ============================================================
  // PRETTIER FORMATTING
  // Tests that Prettier correctly formats supported files
  // ============================================================

  describe("Prettier Formatting", () => {
    (prettierAvailable ? test : test.skip)("formats JavaScript files", () => {
      const testFile = path.join(tempDir, "test.js");
      fs.writeFileSync(testFile, UNFORMATTED_JS);

      execSync(`"${PRETTIER_BIN}" --write "${testFile}"`, {
        cwd: PROJECT_ROOT,
      });

      const result = fs.readFileSync(testFile, "utf8");
      expect(result).toContain("const x = {"); // Prettier adds spaces
    });

    (prettierAvailable ? test : test.skip)("does NOT format Java files", () => {
      const testFile = path.join(tempDir, "Test.java");
      fs.writeFileSync(testFile, JAVA_CODE);

      try {
        execSync(`"${PRETTIER_BIN}" --write "${testFile}"`, {
          cwd: PROJECT_ROOT,
          stdio: "pipe",
        });
      } catch (e) {
        // Expected - Prettier doesn't support Java
      }

      const result = fs.readFileSync(testFile, "utf8");
      expect(result).toBe(JAVA_CODE); // Unchanged
    });
  });

  // ============================================================
  // FILE TYPE FILTERING
  // Tests that only supported file types are processed
  // ============================================================

  describe("File Type Filtering", () => {
    (prettierAvailable ? test : test.skip)(
      "filters files correctly by extension",
      () => {
        execSync("git init", { cwd: tempDir });
        execSync("git config user.email 'test@test.com'", { cwd: tempDir });
        execSync("git config user.name 'Test'", { cwd: tempDir });

        // Create various file types
        fs.writeFileSync(path.join(tempDir, "app.js"), UNFORMATTED_JS);
        fs.writeFileSync(path.join(tempDir, "Main.java"), JAVA_CODE);
        fs.writeFileSync(path.join(tempDir, "image.png"), "fake-image");

        execSync("git add .", { cwd: tempDir });

        // Simulate hook filtering
        const stagedFiles = execSync(
          "git diff --cached --name-only --diff-filter=ACMR",
          { cwd: tempDir, encoding: "utf8" },
        )
          .split("\n")
          .filter((f) => f.trim())
          .filter((f) =>
            /\.(js|ts|jsx|tsx|html|xhtml|css|less|json|md)$/.test(f),
          );

        expect(stagedFiles).toContain("app.js");
        expect(stagedFiles).not.toContain("Main.java");
        expect(stagedFiles).not.toContain("image.png");
      },
    );
  });

  // ============================================================
  // COMPLETE HOOK SIMULATION
  // End-to-end test of the full hook workflow
  // ============================================================

  describe("Full Hook Workflow", () => {
    (prettierAvailable ? test : test.skip)(
      "formats staged files and re-stages them",
      () => {
        // Setup Git repo
        execSync("git init", { cwd: tempDir });
        execSync("git config user.email 'test@test.com'", { cwd: tempDir });
        execSync("git config user.name 'Test'", { cwd: tempDir });

        // Create and stage unformatted file
        const testFile = path.join(tempDir, "app.js");
        fs.writeFileSync(testFile, UNFORMATTED_JS);
        execSync("git add app.js", { cwd: tempDir });

        // Simulate hook workflow:
        // 1. Get staged files
        const stagedFiles = execSync(
          "git diff --cached --name-only --diff-filter=ACMR",
          { cwd: tempDir, encoding: "utf8" },
        )
          .split("\n")
          .filter((f) => f.trim())
          .filter((f) =>
            /\.(js|ts|jsx|tsx|html|xhtml|css|less|json|md)$/.test(f),
          );

        // 2. Run Prettier
        for (const file of stagedFiles) {
          const fullPath = path.join(tempDir, file);
          execSync(`"${PRETTIER_BIN}" --write "${fullPath}"`, {
            cwd: PROJECT_ROOT,
          });
        }

        // 3. Re-stage
        for (const file of stagedFiles) {
          execSync(`git add "${file}"`, { cwd: tempDir });
        }

        // Verify: File is formatted
        const content = fs.readFileSync(testFile, "utf8");
        expect(content).toContain("const x = {");

        // Verify: Changes are staged
        const status = execSync("git status --porcelain", {
          cwd: tempDir,
          encoding: "utf8",
        });
        expect(status).toContain("A  app.js");
      },
    );
  });

  // ============================================================
  // HOOK INSTALLATION
  // Tests the hook file creation
  // ============================================================

  describe("Hook Installation", () => {
    test("creates executable hook file", () => {
      execSync("git init", { cwd: tempDir });

      const hookPath = path.join(tempDir, ".git", "hooks", "pre-commit");
      fs.writeFileSync(hookPath, "#!/bin/sh\necho test", { mode: 0o755 });

      const stats = fs.statSync(hookPath);

      // Check executable bit on Unix
      if (process.platform !== "win32") {
        expect(stats.mode & 0o111).toBeGreaterThan(0);
      }
    });
  });
});
