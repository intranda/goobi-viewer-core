const {
  SUPPORTED_EXTENSIONS,
  getPrettierBinPath,
  prettierExists,
  getStagedFiles,
  filterPrettierFiles,
  runPrettier,
  stageFiles,
  runPreCommit,
} = require("../formatter");

describe("Pre-Commit Formatter", () => {
  // ============================================================
  // FILE EXTENSION FILTER
  // Which files should Prettier format?
  // ============================================================

  describe("File Extensions", () => {
    const formattable = [
      "app.js",
      "utils.ts",
      "Component.jsx",
      "Page.tsx",
      "index.html",
      "template.xhtml",
      "styles.css",
      "theme.less",
      "config.json",
      "README.md",
    ];

    const notFormattable = [
      "Main.java",
      "App.class",
      "image.png",
      "photo.jpg",
      "config.xml",
      "data.sql",
      "script.sh",
    ];

    test.each(formattable)("formats %s", (file) => {
      expect(SUPPORTED_EXTENSIONS.test(file)).toBe(true);
    });

    test.each(notFormattable)("does NOT format %s", (file) => {
      expect(SUPPORTED_EXTENSIONS.test(file)).toBe(false);
    });
  });

  // ============================================================
  // CROSS-PLATFORM SUPPORT
  // Prettier path differs between Windows and Unix
  // ============================================================

  describe("Cross-Platform Prettier Path", () => {
    const originalPlatform = process.platform;

    afterEach(() => {
      Object.defineProperty(process, "platform", { value: originalPlatform });
    });

    test("Unix: uses 'prettier'", () => {
      Object.defineProperty(process, "platform", { value: "darwin" });
      const result = getPrettierBinPath("/base");

      expect(result).toContain("prettier");
      expect(result).not.toContain(".cmd");
    });

    test("Windows: uses 'prettier.cmd'", () => {
      Object.defineProperty(process, "platform", { value: "win32" });
      const result = getPrettierBinPath("/base");

      expect(result).toContain("prettier.cmd");
    });
  });

  // ============================================================
  // GIT STAGED FILES
  // Parse output from 'git diff --cached'
  // ============================================================

  describe("Git Staged Files", () => {
    test("parses file list correctly", () => {
      const mockExec = jest.fn().mockReturnValue("file1.js\nfile2.ts\n");
      expect(getStagedFiles(mockExec)).toEqual(["file1.js", "file2.ts"]);
    });

    test("handles Windows line endings (CRLF)", () => {
      const mockExec = jest.fn().mockReturnValue("file1.js\r\nfile2.ts\r\n");
      expect(getStagedFiles(mockExec)).toEqual(["file1.js", "file2.ts"]);
    });

    test("returns empty array when no files staged", () => {
      const mockExec = jest.fn().mockReturnValue("");
      expect(getStagedFiles(mockExec)).toEqual([]);
    });
  });

  // ============================================================
  // FILE FILTERING
  // Filter staged files to only Prettier-supported types
  // ============================================================

  describe("File Filtering", () => {
    test("keeps only formattable files", () => {
      const files = ["app.js", "Main.java", "styles.css", "image.png"];
      expect(filterPrettierFiles(files)).toEqual(["app.js", "styles.css"]);
    });

    test("returns empty when no files match", () => {
      const files = ["Main.java", "App.class"];
      expect(filterPrettierFiles(files)).toEqual([]);
    });
  });

  // ============================================================
  // COMMAND EXECUTION
  // Verify correct shell commands are built
  // ============================================================

  describe("Command Building", () => {
    test("Prettier command includes --write and all files", () => {
      const mockExec = jest.fn();
      runPrettier(["a.js", "b.css"], "/bin/prettier", mockExec);

      expect(mockExec).toHaveBeenCalledWith(
        '"/bin/prettier" --write "a.js" "b.css"',
        { stdio: "inherit" },
      );
    });

    test("git add command includes all files", () => {
      const mockExec = jest.fn();
      stageFiles(["a.js", "b.css"], mockExec);

      expect(mockExec).toHaveBeenCalledWith('git add "a.js" "b.css"');
    });

    test("empty file list does nothing", () => {
      const mockExec = jest.fn();
      runPrettier([], "/bin/prettier", mockExec);
      stageFiles([], mockExec);

      expect(mockExec).not.toHaveBeenCalled();
    });
  });

  // ============================================================
  // PRETTIER INSTALLATION CHECK
  // Verify Prettier is installed before running
  // ============================================================

  describe("Prettier Installation Check", () => {
    test("returns true if Prettier exists", () => {
      const mockFs = { existsSync: jest.fn().mockReturnValue(true) };
      expect(prettierExists("/path/to/prettier", mockFs)).toBe(true);
    });

    test("returns false if Prettier is missing", () => {
      const mockFs = { existsSync: jest.fn().mockReturnValue(false) };
      expect(prettierExists("/path/to/prettier", mockFs)).toBe(false);
    });
  });

  // ============================================================
  // MAIN WORKFLOW
  // The complete pre-commit hook flow
  // ============================================================

  describe("Main Workflow", () => {
    test("exits early when no files are staged", () => {
      const mockExec = jest.fn().mockReturnValue("");
      const mockExit = jest.fn();

      const result = runPreCommit({
        execFn: mockExec,
        exit: mockExit,
        log: jest.fn(),
      });

      expect(result.reason).toBe("no-staged-files");
      expect(mockExit).toHaveBeenCalledWith(0);
    });

    test("formats and re-stages JS/CSS files", () => {
      const mockExec = jest.fn().mockReturnValue("app.js\nstyles.css\n");
      const mockFs = { existsSync: jest.fn().mockReturnValue(true) };

      const result = runPreCommit({
        execFn: mockExec,
        exit: jest.fn(),
        log: jest.fn(),
        baseDir: "/scripts",
        fsModule: mockFs,
      });

      expect(result.success).toBe(true);
      expect(result.filesProcessed).toBe(2);
      // Called 3 times: git diff, prettier, git add
      expect(mockExec).toHaveBeenCalledTimes(3);
    });

    test("skips Java files completely", () => {
      const mockExec = jest.fn().mockReturnValue("Main.java\n");

      const result = runPreCommit({
        execFn: mockExec,
        exit: jest.fn(),
        log: jest.fn(),
      });

      expect(result.filesProcessed).toBe(0);
      // Only git diff called, not prettier
      expect(mockExec).toHaveBeenCalledTimes(1);
    });

    test("skips formatting if Prettier is not installed", () => {
      const mockExec = jest.fn().mockReturnValue("app.js\n");
      const mockFs = { existsSync: jest.fn().mockReturnValue(false) };
      const mockLog = jest.fn();

      const result = runPreCommit({
        execFn: mockExec,
        exit: jest.fn(),
        log: mockLog,
        fsModule: mockFs,
      });

      expect(result.reason).toBe("prettier-not-found");
      expect(result.filesProcessed).toBe(0);
      expect(mockLog).toHaveBeenCalledWith(
        "⚠️  Prettier not found. Skipping formatting.",
      );
      // Only git diff called, not prettier
      expect(mockExec).toHaveBeenCalledTimes(1);
    });
  });
});
