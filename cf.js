
// 允许访问的源文件路径 (Raw File Proxy)
const GITHUB_RAW_BASE = "https://raw.githubusercontent.com";
const REPO_OWNER = "lijiaxu2011";
const REPO_NAME = "MagicWord";
const BRANCH = "main";
const APK_FILENAME = "MagicWordLatest.apk";

async function handleRequest(request) {
  const url = new URL(request.url);
  const path = url.pathname;

  // 1. 处理 APK 下载请求
  // 路径规则: /MagicWordLatest.apk 或 /latest/MagicWord.apk
  if (path === `/${APK_FILENAME}` || path === "/latest/MagicWord.apk") {
    // 构造 GitHub Raw 文件地址
    // https://raw.githubusercontent.com/lijiaxu2011/MagicWord/main/MagicWordLatest.apk
    const rawUrl = `${GITHUB_RAW_BASE}/${REPO_OWNER}/${REPO_NAME}/${BRANCH}/${APK_FILENAME}`;
    
    // 发起请求
    const response = await fetch(rawUrl, {
      headers: {
        "User-Agent": "MagicWord-Updater" // 防止被 GitHub 拦截
      }
    });

    if (response.status === 200) {
      // 转发文件，并设置正确的 Content-Type
      const newHeaders = new Headers(response.headers);
      newHeaders.set("Content-Type", "application/vnd.android.package-archive");
      newHeaders.set("Content-Disposition", `attachment; filename="${APK_FILENAME}"`);
      
      return new Response(response.body, {
        status: 200,
        headers: newHeaders
      });
    } else {
      return new Response("File not found on GitHub Raw", { status: 404 });
    }
  }

  // 2. 原有的 Release API 反代逻辑 (保持不变，用于检查更新信息)
  // ... (保留之前的 API 反代逻辑，如果需要的话。这里为了简洁只展示 Raw Proxy 部分)
  // 如果需要合并之前的逻辑，可以将之前的代码粘贴在这里。
  
  // 为了确保兼容性，我们把之前的逻辑也简单集成一下：
  if (path.startsWith("/api/")) {
     // 简化的 API 转发
     const newUrl = "https://api.github.com" + path.replace("/api", "") + url.search;
     const headers = new Headers(request.headers);
     headers.delete("Authorization");
     return fetch(newUrl, { method: request.method, headers: headers, body: request.body });
  }

  return new Response("MagicWord Proxy Service", { status: 200 });
}

addEventListener("fetch", event => {
  event.respondWith(handleRequest(event.request));
});
