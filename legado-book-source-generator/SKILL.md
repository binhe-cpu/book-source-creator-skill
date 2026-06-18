---
name: legado-book-source-generator
description: Use when 用户要求为任意网站生成书源、生成阅读书源、分析小说站点、生成 Legado/阅读规则。强制触发词：书源、生成书源、帮我生成、book source、legado、阅读书源、小说站点分析。如果用户给出了一个 URL 并要求生成或分析，必须加载此 skill。
---

# Legado 书源生成

Run exactly:

```bash
node "<skill-dir>/scripts/bsg.mjs" init <site-url> [--fast] [--cwd <输出目录>]
```

Follow the `nextAction` returned by each command. The workflow is: `probe → assess → analyze → generate → validate → deliver`. Only the `deliver` command produces the final "passed" certification.

AI analyzes websites and writes rules. `bsg.mjs` enforces phase order, structural checks, and deliverable integrity.

If skill is on C: drive and project on D: drive, use `--cwd`:
```bash
node "<skill-dir>/scripts/bsg.mjs" init <url> --cwd "D:/my-project"
```

## Output

- `outputs/<site-slug>/book-source.json` — sole user deliverable
- `runs/<site-slug>/` — process records (assessment.md, analysis.md, validator-report.json, etc.)

## Fast path

Add `--fast` to init only when ALL four conditions are met:

1. Anonymous HTTP fetch returns visible text for search/detail/toc/content (not CSR shell)
2. No Cloudflare / captcha
3. No login required (or login is optional)
4. No `webView` / `webJs` / CSR dependency

If any condition is violated, use the default full path with Browser MCP.

## Stop points

Stop and wait for the user when `requiredUserAction` is non-null:

| Trigger | Action |
|---------|--------|
| Assessment rating: 不建议生成 | Ask user to decide |
| WebView/CSR detected but no Android device | Ask user to connect phone/emulator (setup guide: `docs/SETUP.md`) |
| Login credentials needed | User completes login, then continue |
| Android Probe needs adb authorization | User confirms USB debugging on phone |
| Same error 5 consecutive times (convergence failure) | Ask user to decide |

Login credential channels: phone scan / token input / Browser MCP cookie extraction → see `references/policies.md`

## Principles

1. **Observed behavior over model inference.** If Browser MCP contradicts model assumptions, trust Browser MCP. Document the correction.
2. **Legado official rules first.** Follow `references/legado-official-rule-notes.md` over experiential patterns.
3. **Candidate sources are reference only.** `candidates/` content is not production-ready.
4. **Browser MCP ≠ Android WebView.** Write "content visible in desktop browser" not "Legado WebView can render."
5. **WebView renders, it doesn't decrypt.** If encrypted content renders in browser, use `webView: true` + `webJs` for DOM extraction. Do not analyze encryption algorithms.
6. **Cover only search / detail / toc / content.** Do not enable explore unless explicitly requested.
7. **Reference examples, don't copy.** Compare against the closest example in `examples/` for structure, but rules must match actual site behavior.

## Reference documents

Load by phase. One level deep from SKILL.md:

| Phase | Required | On-demand |
|-------|----------|-----------|
| probe / assess | `references/policies.md`, `references/assessment-template.md` | |
| analyze | `references/analysis-workflow.md` | `references/webview-behavior-matrix.md` (CSR/WebView), `examples/README.md` |
| generate | `references/legado-official-rule-notes.md`, `references/legado-json-structure.md` | `examples/README.md`, `examples/<site>/book-source.json` |
| validate | `references/validator-integration.md`, `references/validation-policy.md` | `references/failure-diagnosis.md`, `references/validation-checklist.md` |
| debug/review | `references/debugging-collaboration.md` | `references/failure-diagnosis.md` |

## Android WebView Probe

When loginFeatures has webView or webJs, use mode=android for content validation. Do not use mode=http to pass CSR content.

Setup sequence:
1. `bsg.mjs validator-start`
2. `validator/setup-adb.bat`
3. `validator/setup-android-probe.bat`
4. `validate-with-validator.mjs <source> <keyword> android --output runs/<slug>/`

If Android Probe is unavailable, mark content as `validator_limitation`, not `passed`.

Phone setup guide: `docs/SETUP.md` (includes brand-specific USB debugging steps).
