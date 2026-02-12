
// 配置常量
const GITHUB_OWNER = "lijiaxu2021";
const GITHUB_REPO = "MagicWord";
const APK_FILENAME = "MagicWordLatest.apk";

// 两个关键的目标地址
// 1. APK 直链 (Raw)
const RAW_APK_URL = "https://raw.githubusercontent.com/lijiaxu2021/MagicWord/main/MagicWordLatest.apk";
// 2. GitHub API (Release Check)
const GITHUB_API_BASE = "https://api.github.com";

async function handleRequest(request) {
  const url = new URL(request.url);
  const path = url.pathname;
  
  // ---------------------------------------------------------
  // 1. APK 下载请求 (Raw File Proxy)
  // ---------------------------------------------------------
  // 匹配 /MagicWordLatest.apk 或 /latest/MagicWord.apk
  if (path === `/${APK_FILENAME}` || path === "/latest/MagicWord.apk") {
    // 转发请求到 GitHub Raw
    const response = await fetch(RAW_APK_URL, {
      headers: { 
        "User-Agent": "MagicWord-Updater" // 防止 GitHub 拦截
      }
    });

    if (response.status === 200) {
      // 成功获取文件，重写响应头，强制浏览器下载
      const newHeaders = new Headers(response.headers);
      newHeaders.set("Content-Type", "application/vnd.android.package-archive");
      newHeaders.set("Content-Disposition", `attachment; filename="${APK_FILENAME}"`);
      // 允许跨域
      newHeaders.set("Access-Control-Allow-Origin", "*");
      
      return new Response(response.body, { 
        status: 200, 
        headers: newHeaders 
      });
    } else {
      return new Response(`File not found on GitHub Raw.\nURL: ${RAW_APK_URL}\nStatus: ${response.status}`, { status: 404 });
    }
  }

  // ---------------------------------------------------------
  // 2. API 请求转发 (Release Check)
  // ---------------------------------------------------------
  // 客户端请求格式: /api/repos/{owner}/{repo}/releases/latest
  if (path.startsWith("/api/")) {
    // 去掉前缀 /api，还原为 GitHub API 的路径
    // 例如 /api/repos/lijiaxu2021/MagicWord/releases/latest -> /repos/lijiaxu2021/MagicWord/releases/latest
    const apiPath = path.replace(/^\/api/, "");
    const targetUrl = GITHUB_API_BASE + apiPath + url.search;

    // 构造新请求
    const headers = new Headers(request.headers);
    headers.delete("Host");
    headers.delete("Referer");
    // GitHub API 要求 User-Agent
    headers.set("User-Agent", "MagicWord-Proxy");
    // 移除敏感信息
    headers.delete("Authorization");
    headers.delete("Cookie");

    const response = await fetch(targetUrl, {
      method: request.method,
      headers: headers,
      body: request.body
    });

    // 处理响应
    const newHeaders = new Headers(response.headers);
    newHeaders.set("Access-Control-Allow-Origin", "*"); // 允许跨域调用 API

    return new Response(response.body, {
      status: response.status,
      headers: newHeaders
    });
  }

  // ---------------------------------------------------------
  // 3. 兜底响应
  // ---------------------------------------------------------
  return new Response("MagicWord Proxy Service is Running.\nAvailable Endpoints:\n- /MagicWordLatest.apk\n- /api/repos/...", { status: 200 });
}

addEventListener("fetch", event => {
  event.respondWith(handleRequest(event.request));
});
