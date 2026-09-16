# Copilot Instructions

Last reviewed: 2026-08-12

## Agent Instructions

These instructions are loaded automatically by GitHub Copilot in all chat modes. When operating as an agent:

- **Use alongside `AGENT.md`** for repo folder structure, placement rules, and development workflow, subject to Instruction Authority precedence below.

### AI Operation Mode

Use these modes to keep prompts concise and predictable across the team:

- **Discovery mode:** Read-only exploration. Fallbacks are allowed, but must be explicitly labeled as non-authoritative.
- **Source-strict mode:** Single authoritative source only. If unavailable, fail fast and report the blocker. No fallback.
- **Change mode:** Draft/preview first, then require explicit user confirmation before any write operation.

Default mode by request type:

- Dataverse entity/record questions default to **Source-strict mode**.
- Repo codebase analysis defaults to **Discovery mode** unless the user asks for source-strict behavior.
- Any create/update/delete request defaults to **Change mode**.

## Instruction Authority

When guidance conflicts, use this precedence order:

1. `.github/copilot-instructions.md`
2. `AGENTS.md`
3. `README.md`

If a conflict materially affects implementation, follow the highest-precedence source and call out the conflict in your response.

## Prompt Overlay And Context Budget

To reduce context bloat and improve instruction retention:

- Keep this file focused on policy, routing, and non-negotiable local exceptions.
- Prefer linking to canonical documentation rather than restating long standards or architecture summaries.
- If multiple AI prompt layers are active (for example, review prompts or task-specific templates), resolve differences using Instruction Authority precedence.
- If two sources at the same precedence conflict, apply the stricter safety/validation rule and report the conflict in your response.

## Required Post-Change Checklist

After edits, agents must:

1. Update documentation with changes.
2. Summarize impacted objects.
3. Report validation performed and remaining validation gaps.

**Keeping docs current:** When logic or features are added, removed, or substantially changed, update the README to reflect it. 

## Agent Response Expectations

For substantive code changes, responses should include:

1. Files changed.
2. Behavioral impact summary.
3. Validation completed.
4. Validation not completed (and why).
5. Documentation updates made (or why none were needed).

## Maintenance Cadence For This File

- Review quarterly.
- Review after major architecture, deployment workflow, or standards changes.
- Keep this file concise; move deep implementation detail into `_Resources/Documentation/` and link to it.