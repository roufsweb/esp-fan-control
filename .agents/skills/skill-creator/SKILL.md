---
name: skill-creator
description: >-
  Standardizes the creation of new Antigravity agent skills on demand. Use whenever
  a new workflow, tool integration, or domain capability is needed in the project.
---

# Skill Creator Specification

Use this skill whenever you identify a recurring technical pattern, protocol, or workflow that should be packaged into an autonomous agent skill.

---

## 1. Skill Placement & Hierarchy

All workspace-level skills must be placed under:
`.agents/skills/<skill-name>/SKILL.md`

### Directory Layout
```
.agents/skills/<skill-name>/
├── SKILL.md             # Required: YAML frontmatter + clear markdown instructions
├── scripts/             # Optional: Helper scripts (PowerShell, Python, Bash)
└── examples/            # Optional: Code templates, test fixtures, or payload examples
```

---

## 2. Skill Authoring Process

1. **Identify the Need**:
   * Determine the specific problem domain (e.g. `esp32-ble-protocol`, `tachometer-calibration`, `android-release-builder`).
   * Verify that an existing skill does not already cover this scope.

2. **Compose Frontmatter**:
   * Must include `name` (kebab-case matching directory name).
   * Must include `description` stating **what** the skill does and **when** it should be triggered.
   * Keep description under 3 lines for token efficiency during progressive disclosure.

```yaml
---
name: your-skill-name
description: >-
  Actionable 1-2 sentence description detailing capabilities and triggering criteria.
---
```

3. **Structure Document Content**:
   * **Context / Purpose**: Concise statement of what this skill achieves.
   * **Core Directives**: Clear bullet points of mandatory rules and constraints.
   * **Implementation Walkthrough**: Step-by-step procedure with code snippets.
   * **Gotchas / Edge Cases**: Common pitfalls to avoid.

4. **Register in Project Files**:
   * Add the new skill entry to `PROJECT_INDEX.md`.
   * Log the skill creation event in `PROJECT_SUMMARY.md`.

---

## 3. Skill Checklist
Before saving a new skill:
- [ ] Is `name` unique and matching the folder name?
- [ ] Is frontmatter valid YAML?
- [ ] Does the skill avoid bloating the context with unnecessary text?
- [ ] Are code examples copy-paste ready and syntactically correct?

