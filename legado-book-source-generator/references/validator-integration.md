# Validator 集成

## 概述

Validator 是本地书源预验证工具，运行在 `http://localhost:1111`。Skill 生成书源后，先跑 validator 验证，再决定交付或回修。生命周期管理由 `bsg.mjs validator-start/stop` 统一处理。

## 内置运行包

本 skill 自带 validator 前后端运行包：

```text
validator/
  run.bat
  setup-adb.bat
  setup-android-probe.bat
  app/legado-source-validator.jar
  examples/
```

人工启动：双击 `validator/run.bat`。脚本自动启动：`node scripts/bsg.mjs validator-start`。

启动后打开 `http://localhost:1111` 可使用浏览器调试台。

## API 接口

### POST /api/debug/run

单次验证：传入书源 JSON + 关键词，返回完整步骤详情。

```bash
curl -X POST http://localhost:1111/api/debug/run \
  -H "Content-Type: application/json" \
  -d '{"sourceJson": "<书源JSON>", "sourceUrl": "https://...", "keyword": "关键词", "mode": "http"}'
```

参数：
- `sourceJson`：书源 JSON 字符串（数组或单对象均可）
- `sourceUrl`：书源的 bookSourceUrl
- `keyword`：搜索关键词
- `mode`：`http` | `browser` | `android`

返回结构见 `validator-report.json`（包含 phases、steps、ruleHits、bodyPreview）。

### POST /api/debug/smoke

批量验证：跑全部回归 case，返回汇总报告。

```bash
curl -X POST http://localhost:1111/api/debug/smoke \
  -H "Content-Type: application/json" \
  -d '{}'
```

## 状态判定

| 状态 | 含义 | Skill 动作 |
|------|------|-----------|
| `passed` | 全链路 success + 无登录态特征，或已完成登录态验证 | 交付书源 |
| `anonymous_candidate` | 匿名全链路 success，但站点有 loginUrl/enabledCookieJar/Authorization/webJs/webView | 不能标可用，需登录态/App 复核 |
| `failed` | 某阶段 error，有可修证据 | AI 自动回修 |
| `needs_app_review` | needsAppReview=true 或命中 App-only 行为 | 停止自动修，标记需复核 |
| `validator_limitation` | validator 不支持的规则能力 | validator 无法验证该能力；预期需要 App/WebView 复核 |
| `failed_unresolved` | 同一错误连续 5 次未修复（收敛失败） | 标记未解决，需人工检查 |

## 判定逻辑

```
if 全 phases == "success" AND 无登录态特征 (loginUrl/enabledCookieJar/Authorization/webJs/webView):
    status = "passed"
elif 全 phases == "success" AND 有登录态特征:
    status = "anonymous_candidate"
elif step.needsAppReview == true:
    status = "needs_app_review"
elif step.error 含 "Cloudflare|Turnstile|验证码|登录|WebView":
    status = "needs_app_review"
elif step.error 含 "已知限制|不支持|@js 动态 URL":
    status = "validator_limitation"
elif step.ruleHits 有字段失败:
    status = "failed"  // AI 可修
elif step.error 含 URL/编码/规则错误:
    status = "failed"  // AI 可修
else:
    status = "needs_app_review"  // 保守判定
```

## 手动验证（脚本故障时）

如果 `validate-with-validator.mjs` 不可用，直接 curl API：

```bash
# 导入书源
curl -X POST http://localhost:1111/api/source/import \
  -H "Content-Type: application/json" \
  -d @outputs/<slug>/book-source.json

# 运行验证
curl -X POST http://localhost:1111/api/debug/run \
  -H "Content-Type: application/json" \
  -d '{"sourceUrl":"https://...","keyword":"关键词","mode":"http"}'
```

结果判断：`phases` 全部 `success` → 通过。`content` 阶段 `error` + bodyPreview 是 JS 壳 → CSR，需 WebView。

## 前置检查

调用 validator 前，先检查是否运行：

```bash
curl -s http://localhost:1111/api/sources >nul 2>&1 && echo Running || echo Not running
```

**禁止用 `/health` 探测（该端点不存在，返回 404）。只用 `/api/sources`。**

已有服务则复用，不重复启动。

## Android Probe / adb

使用 `mode=android` 处理 `webView:true` / `webJs` 时需要 adb 和已连接 Android 设备/模拟器。

- 缺 adb：运行 `validator/setup-adb.bat`，脚本会从 Google 官方地址下载 Windows Platform-Tools 到 `validator/tools/platform-tools/`
- 安装并启动 Probe：运行 `validator/setup-android-probe.bat`
- 找不到设备：返回 `validator_limitation` / `Android Probe 不可用: No Android devices connected`
