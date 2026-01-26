const {
  HOOK_TEMPLATE,
  getHookPaths,
  ensureHooksDir,
  writeHook,
  installHooks,
} = require("../install");

describe("Hook Installation", () => {
  // ============================================================
  // HOOK TEMPLATE
  // The shell script that Git executes on commit
  // ============================================================

  describe("Hook Template", () => {
    test("is a valid shell script", () => {
      expect(HOOK_TEMPLATE).toMatch(/^#!/);
    });

    test("calls the formatter script", () => {
      expect(HOOK_TEMPLATE).toContain("scripts/pre-commit/formatter.js");
    });

    test("uses node to execute", () => {
      expect(HOOK_TEMPLATE).toContain("node ");
    });
  });

  // ============================================================
  // PATH CALCULATION
  // Determines where to install the hook
  // ============================================================

  describe("Path Calculation", () => {
    test("calculates correct hook location", () => {
      const result = getHookPaths("/project/scripts/pre-commit");

      expect(result.hooksDir).toContain(".git");
      expect(result.hooksDir).toContain("hooks");
      expect(result.preCommitHook).toContain("pre-commit");
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
    test("installs hook successfully", () => {
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
