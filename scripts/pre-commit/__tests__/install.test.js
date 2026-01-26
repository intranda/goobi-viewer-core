const {
  HOOK_TEMPLATE,
  HOOK_TEMPLATE_WIN,
  getHookPaths,
  ensureHooksDir,
  writeHook,
  installHooks,
} = require("../install");

describe("Hook Installation", () => {
  // ============================================================
  // HOOK TEMPLATE
  // The Node script that Git executes on commit
  // Works on all platforms including Windows/JGit
  // ============================================================

  describe("Hook Template (Unix)", () => {
    test("is a valid Node script with shebang", () => {
      expect(HOOK_TEMPLATE).toMatch(/^#!\/usr\/bin\/env node/);
    });

    test("references the formatter script", () => {
      expect(HOOK_TEMPLATE).toContain("formatter.js");
    });

    test("uses path.join for cross-platform compatibility", () => {
      expect(HOOK_TEMPLATE).toContain("path.join");
    });

    test("calls runPreCommit function", () => {
      expect(HOOK_TEMPLATE).toContain("runPreCommit()");
    });
  });

  // ============================================================
  // HOOK TEMPLATE (Windows)
  // The CMD script for Windows/JGit compatibility
  // ============================================================

  describe("Hook Template (Windows)", () => {
    test("is a valid CMD script", () => {
      expect(HOOK_TEMPLATE_WIN).toMatch(/^@echo off/);
    });

    test("references the formatter script", () => {
      expect(HOOK_TEMPLATE_WIN).toContain("formatter.js");
    });

    test("uses node to execute", () => {
      expect(HOOK_TEMPLATE_WIN).toContain("node");
    });
  });

  // ============================================================
  // PATH CALCULATION
  // Determines where to install the hook
  // ============================================================

  describe("Path Calculation", () => {
    test("calculates correct hook locations", () => {
      const result = getHookPaths("/project/scripts/pre-commit");

      expect(result.hooksDir).toContain(".git");
      expect(result.hooksDir).toContain("hooks");
      expect(result.preCommitHook).toContain("pre-commit");
      expect(result.preCommitHookWin).toContain("pre-commit.cmd");
    });
  });

  // ============================================================
  // DIRECTORY CREATION
  // Creates .git/hooks if it doesn't exist
  // ============================================================

  describe("Directory Creation", () => {
    test("creates directory if missing", () => {
      const mockFs = {
        existsSync: jest.fn().mockReturnValue(false),
        mkdirSync: jest.fn(),
      };

      ensureHooksDir("/path/to/hooks", mockFs);

      expect(mockFs.mkdirSync).toHaveBeenCalledWith("/path/to/hooks", {
        recursive: true,
      });
    });

    test("skips creation if directory exists", () => {
      const mockFs = {
        existsSync: jest.fn().mockReturnValue(true),
        mkdirSync: jest.fn(),
      };

      ensureHooksDir("/path/to/hooks", mockFs);

      expect(mockFs.mkdirSync).not.toHaveBeenCalled();
    });
  });

  // ============================================================
  // HOOK FILE WRITING
  // Writes the hook with executable permissions
  // ============================================================

  describe("Hook Writing", () => {
    test("writes with executable permissions (0755)", () => {
      const mockFs = { writeFileSync: jest.fn() };

      writeHook("/path/to/hook", "content", mockFs);

      expect(mockFs.writeFileSync).toHaveBeenCalledWith(
        "/path/to/hook",
        "content",
        { mode: 0o755 },
      );
    });
  });

  // ============================================================
  // FULL INSTALLATION
  // The complete installation process
  // ============================================================

  describe("Full Installation", () => {
    test("installs both Unix and Windows hooks", () => {
      const mockFs = {
        existsSync: jest.fn().mockReturnValue(true),
        writeFileSync: jest.fn(),
      };

      const result = installHooks({
        baseDir: "/project/scripts",
        fsModule: mockFs,
        log: jest.fn(),
      });

      expect(result.success).toBe(true);
      expect(result.hookPath).toContain("pre-commit");
      expect(result.hookPathWin).toContain("pre-commit.cmd");
      // Should write both hooks
      expect(mockFs.writeFileSync).toHaveBeenCalledTimes(2);
    });

    test("creates hooks directory if needed", () => {
      const mockFs = {
        existsSync: jest.fn().mockReturnValue(false),
        mkdirSync: jest.fn(),
        writeFileSync: jest.fn(),
      };

      installHooks({
        baseDir: "/project/scripts",
        fsModule: mockFs,
        log: jest.fn(),
      });

      expect(mockFs.mkdirSync).toHaveBeenCalled();
    });

    test("handles write errors gracefully", () => {
      const mockFs = {
        existsSync: jest.fn().mockReturnValue(true),
        writeFileSync: jest.fn().mockImplementation(() => {
          throw new Error("Permission denied");
        }),
      };

      const result = installHooks({
        baseDir: "/project/scripts",
        fsModule: mockFs,
        log: jest.fn(),
      });

      expect(result.success).toBe(false);
      expect(result.error).toBe("Permission denied");
    });
  });
});
