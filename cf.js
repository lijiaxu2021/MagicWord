
// 配置常量
const GITHUB_RAW_BASE = "https://raw.githubusercontent.com";
const REPO_OWNER = "lijiaxu2011";
const REPO_NAME = "MagicWord";
const BRANCH = "main";
const APK_FILENAME = "MagicWordLatest.apk";

// 需要屏蔽的路径规则 (Release 反代用)
const BLOCKED_PATTERNS = [
  '/login', '/logout', '/signup', '/register', '/sessions', '/session', 
  '/auth', '/oauth', '/authorize', '/token', '/login/oauth', '/settings', 
  '/account', '/password', '/admin', '/dashboard', '/manage', '/organizations', 
  '/orgs', '/teams', '/api/v1/user', '/api/v1/users', '/api/v1/orgs', 
  '/api/v1/teams', '/new', '/create', '/clone', '/fork', '/star', '/watch', 
  '/sponsors', '/marketplace', '/pulls', '/issues/new', '/wiki/_new', 
  '/notifications', '/subscriptions', '/watching', '/profile', '/avatar', 
  '/emails', '/keys', '/security', '/billing', '/invoices', '/payment', 
  '/plans', '/upgrade', '/copilot'
];

// 路径允许列表 (Release 反代用)
const ALLOWED_PATH_PREFIXES = [
  '/releases/download/',
  '/releases/tag/',
  '/releases',
  '/github-production-release-asset-',
  `/${APK_FILENAME}`, // 允许访问根目录的 APK
  '/latest/MagicWord.apk' // 允许访问 latest 别名
];

function isPathBlocked(path) {
  // 1. 白名单优先
  for (const prefix of ALLOWED_PATH_PREFIXES) {
    if (path.startsWith(prefix)) return false;
  }
  
  // 2. 首页屏蔽
  if (path === '/' || path === '') return true;
  
  // 3. 黑名单屏蔽
  for (const pattern of BLOCKED_PATTERNS) {
    if (path === pattern || path.startsWith(pattern + '/') || path.startsWith(pattern + '?')) {
      return true;
    }
    // API 特殊处理
    if (path.includes('/api/') && pattern.includes('/api/')) {
       // 简单包含匹配，实际可更严格
       if (path.includes(pattern)) return true;
    }
  }
  
  // 4. 屏蔽隐藏文件
  if (path.split('/').some(segment => segment.startsWith('.'))) return true;

  // 5. API 和 Releases 放行，其他默认屏蔽
  if (path.includes('/releases/') || path.includes('/api/')) {
      return false;
  }

  return true;
}

function blockResponse(path) {
  return new Response(JSON.stringify({
    error: 'Access Denied',
    message: 'This endpoint is not available via proxy',
    path: path
  }, null, 2), {
    status: 403,
    headers: { 'Content-Type': 'application/json' }
  });
}

async function handleRequest(request) {
  const url = new URL(request.url);
  let path = url.pathname;
  let headers = new Headers(request.headers);

  // ---------------------------------------------------------
  // 1. Raw File Proxy (优先处理 MagicWordLatest.apk)
  // ---------------------------------------------------------
  if (path === `/${APK_FILENAME}` || path === "/latest/MagicWord.apk") {
    const rawUrl = `${GITHUB_RAW_BASE}/${REPO_OWNER}/${REPO_NAME}/${BRANCH}/${APK_FILENAME}`;
    const response = await fetch(rawUrl, {
      headers: { "User-Agent": "MagicWord-Updater" }
    });

    if (response.status === 200) {
      const newHeaders = new Headers(response.headers);
      newHeaders.set("Content-Type", "application/vnd.android.package-archive");
      newHeaders.set("Content-Disposition", `attachment; filename="${APK_FILENAME}"`);
      return new Response(response.body, { status: 200, headers: newHeaders });
    } else {
      return new Response("File not found on GitHub Raw", { status: 404 });
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
