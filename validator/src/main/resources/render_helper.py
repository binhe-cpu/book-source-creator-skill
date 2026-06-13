"""
Playwright 渲染助手 — 供 Kotlin validator 通过子进程调用。
协议：stdin 接收 JSON，stdout 输出 JSON。

请求格式：
  {"url": "https://...", "timeout": 30000, "headless": true}

响应格式：
  {"ok": true, "finalUrl": "...", "title": "...", "html": "...(截取前 50000 字符)",
   "screenshot": "...(base64 png)", "bodyLength": 12345, "error": null}

错误响应：
  {"ok": false, "error": "...", "finalUrl": null, ...}
"""

import sys
import json
import base64
import io

def render(url: str, timeout: int = 30000, headless: bool = True) -> dict:
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=headless)
        context = browser.new_context(
            viewport={"width": 1280, "height": 800},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        page = context.new_page()

        try:
            resp = page.goto(url, timeout=timeout, wait_until="domcontentloaded")
            # 等待额外渲染时间
            page.wait_for_timeout(2000)

            final_url = page.url
            title = page.title()
            html = page.content()
            body_length = len(html)

            # 截图
            screenshot_bytes = page.screenshot(type="png", full_page=False)
            screenshot_b64 = base64.b64encode(screenshot_bytes).decode("ascii")

            # 截取 HTML 预览
            html_preview = html[:50000]

            # 检测是否为 Cloudflare/验证码页面
            needs_app_review = False
            review_reason = None
            lower_html = html[:5000].lower()
            if "challenges.cloudflare.com" in lower_html or "turnstile" in lower_html:
                needs_app_review = True
                review_reason = "Cloudflare Turnstile 验证页"
            elif "just a moment" in lower_html:
                needs_app_review = True
                review_reason = "Cloudflare challenge 页面"
            elif "captcha" in lower_html or "验证码" in html[:2000]:
                needs_app_review = True
                review_reason = "验证码页面"

            return {
                "ok": True,
                "finalUrl": final_url,
                "title": title,
                "html": html_preview,
                "screenshot": screenshot_b64,
                "bodyLength": body_length,
                "httpCode": resp.status if resp else None,
                "needsAppReview": needs_app_review,
                "reviewReason": review_reason,
                "error": None
            }

        except Exception as e:
            return {
                "ok": False,
                "finalUrl": page.url if page else None,
                "title": None,
                "html": None,
                "screenshot": None,
                "bodyLength": 0,
                "httpCode": None,
                "needsAppReview": False,
                "reviewReason": None,
                "error": f"{type(e).__name__}: {str(e)}"
            }
        finally:
            browser.close()


def main():
    # 强制 UTF-8 输出
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

    try:
        _main()
    except Exception as e:
        import traceback
        try:
            print(json.dumps({"ok": False, "error": f"{type(e).__name__}: {str(e)}\n{traceback.format_exc()}"}))
        except Exception:
            pass

def _main():
    # 从 stdin 读取请求
    raw = sys.stdin.read()
    try:
        req = json.loads(raw)
    except json.JSONDecodeError as e:
        print(json.dumps({"ok": False, "error": f"Invalid JSON: {e}"}))
        return

    url = req.get("url")
    if not url:
        print(json.dumps({"ok": False, "error": "Missing url"}))
        return

    # 如果提供了 searchKeyword，用 Python 的 quote() 安全编码后拼入 URL
    search_keyword = req.get("searchKeyword")
    search_template = req.get("searchUrlTemplate")
    if search_keyword and search_template:
        from urllib.parse import quote
        encoded = quote(search_keyword, safe="")
        url = search_template.replace("{{key}}", encoded)

    timeout = req.get("timeout", 30000)
    headless = req.get("headless", True)

    result = render(url, timeout, headless)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
