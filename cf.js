
// 配置常量
const GITHUB_RAW_BASE = "https://raw.githubusercontent.com";
const REPO_OWNER = "lijiaxu2021"; // Corrected owner
const REPO_NAME = "MagicWord";
const BRANCH = "main";
const APK_FILENAME = "MagicWordLatest.apk";
const HARDCODED_RAW_URL = "https://raw.githubusercontent.com/lijiaxu2021/MagicWord/refs/heads/main/MagicWordLatest.apk";

// ... (patterns remain the same) ...

async function handleRequest(request) {
  const url = new URL(request.url);
  let path = url.pathname;
  let headers = new Headers(request.headers);

  // ---------------------------------------------------------
  // 1. Raw File Proxy (优先处理 MagicWordLatest.apk)
  // ---------------------------------------------------------
  if (path === `/${APK_FILENAME}` || path === "/latest/MagicWord.apk") {
    // 使用用户指定的硬编码 Raw URL
    const response = await fetch(HARDCODED_RAW_URL, {
      headers: { "User-Agent": "MagicWord-Updater" }
    });

    if (response.status === 200) {
      const newHeaders = new Headers(response.headers);
      newHeaders.set("Content-Type", "application/vnd.android.package-archive");
      newHeaders.set("Content-Disposition", `attachment; filename="${APK_FILENAME}"`);
      return new Response(response.body, { status: 200, headers: newHeaders });
    } else {
      return new Response(`File not found on GitHub Raw. URL: ${HARDCODED_RAW_URL}`, { status: 404 });
    }
  }

  // ---------------------------------------------------------
  // 2. 安全检查
  // ---------------------------------------------------------
  if (isPathBlocked(path)) {
    return blockResponse(path);
  }

  // ---------------------------------------------------------
  // 3. GitHub Release Proxy 逻辑
  // ---------------------------------------------------------
  
  // 处理 objects.githubusercontent.com (实际下载地址)
  if (path.startsWith('/github-production-release-asset-')) {
    const newUrl = 'https://objects.githubusercontent.com' + path + url.search;
    return fetch(newUrl, { headers: headers, method: request.method, body: request.body });
  }

  // 处理 API 请求 (用于检查更新)
  if (path.startsWith('/api/')) {
    if (!path.includes('/repos/') || !path.includes('/releases')) {
      return blockResponse(path);
    }
    
    // 替换 API 路径中的 owner (如果客户端传错) 或者直接转发
    // 这里假设客户端请求的是 /api/repos/{owner}/{repo}/...
    // 我们可以强制修正 owner 为 lijiaxu2021 以防万一
    // 但 UpdateManager.kt 也已经修正，所以直接转发即可
    
    path = path.replace('/api', '');
    const newUrl = 'https://api.github.com' + path + url.search;
    headers.delete('authorization');
    headers.delete('cookie');
    
    return fetch(newUrl, { headers: headers, method: request.method, body: request.body });
  }


  // 处理 Release 页面和下载重定向
  if (path.includes('/releases/')) {
    const githubUrl = 'https://github.com' + path + url.search;
    headers.delete('authorization');
    headers.delete('cookie');
    headers.set('User-Agent', 'Mozilla/5.0 (compatible; GitHubProxy/1.0)');

    const response = await fetch(githubUrl, {
      headers: headers,
      method: request.method,
      body: request.body,
      redirect: 'manual' // 手动处理重定向
    });

    // 处理 302/301 重定向
    if (response.status === 302 || response.status === 301) {
      const location = response.headers.get('Location');
      if (location && location.includes('objects.githubusercontent.com')) {
        const assetUrl = new URL(location);
        // 重写为当前代理域名的路径，以便再次拦截
        const newLocation = url.origin + assetUrl.pathname + assetUrl.search;
        return new Response(null, {
          status: 302,
          headers: { 'Location': newLocation, 'Cache-Control': 'no-cache' }
        });
      }
    }

    // 正常返回
    if (response.status === 200) {
      const responseHeaders = new Headers(response.headers);
      responseHeaders.delete('set-cookie');
      return new Response(response.body, { status: response.status, headers: responseHeaders });
    }
    
    return response;
  }

  return blockResponse(path);
}

addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request));
});
