# 样例目录

本目录存放经过真实站点闭环验证的书源样例。

## 样例

| 样例 | 复杂度 | 关键特征 |
|------|--------|----------|
| `pattern-css-pagination/` | 中 | CSS 选择器 + JS 内容处理 + 目录分页 |
| `pattern-post-detail-toc/` | 低 | POST 搜索、目录嵌详情页、纯静态 HTML |
| `pattern-api-webview-auth/` | 高 | JSON API 搜索/详情/目录 + CSR WebView 正文 + enabledCookieJar + loginUrl + header JS |

## 使用规则

- 样例用于说明交付结构与规则组织方式，不构成对目标站点长期可用性的保证。
- 样例不替代目标站点的 Browser MCP 实测。
- 生成复杂站点的书源时，先找最接近的样例对照结构，但规则必须针对目标站点实测调整。
- 不要在 validator 验证前直接复制样例规则到目标站点。

## 典型场景对照

| 站点特征 | 参考样例 |
|---------|---------|
| 搜索/详情/目录用公开 JSON API，正文 CSR 需 WebView | `pattern-api-webview-auth/` |
| 纯静态 HTML，CSS 选择器直提 | `pattern-css-pagination/` |
| POST 搜索，详情页内嵌目录 | `pattern-post-detail-toc/` |
