
// 目标域名配置
const TARGET_DOMAIN = 'api.github.com'; // 默认代理 GitHub API
const RAW_DOMAIN = 'raw.githubusercontent.com'; // Raw 文件域名
const WORKER_DOMAIN = 'mag.upxuu.com'; // 您的 Worker 域名

// APK 直链配置
const RAW_APK_URL = "https://raw.githubusercontent.com/lijiaxu2021/MagicWord/main/MagicWordLatest.apk";
// 通知文件直链配置
const NOTICE_JSON_URL = "https://raw.githubusercontent.com/lijiaxu2021/MagicWord/main/notice.json";

addEventListener('fetch', event => { 
    event.respondWith(handleRequest(event.request, event)); 
}); 

async function handleRequest(request, event) { 
    const url = new URL(request.url); 
    const userAgent = request.headers.get('User-Agent') || ''; 
     
    // 1. APK 直链下载 (最优先)
    if (url.pathname === `/MagicWordLatest.apk` || url.pathname === "/latest/MagicWord.apk") { 
        return fetchRawApk();
    }

    // 2. 通知文件代理
    if (url.pathname === '/notice.json') {
        return fetchNoticeJson();
    }
     
    // 3. API 代理逻辑
    // 只有路径以 /api/ 开头的才被认为是 API 请求 (客户端约定)
    if (url.pathname.startsWith('/api/')) {
        // 去掉 /api 前缀，代理到 api.github.com
        const newPath = url.pathname.replace(/^\/api/, '');
        const targetUrl = `https://${TARGET_DOMAIN}${newPath}${url.search}`;
        
        return proxyRequest(request, targetUrl, TARGET_DOMAIN);
    }

    // 4. 兜底/默认页面
    return new Response("MagicWord Proxy Service is Running.", { status: 200 });
} 

async function fetchRawApk() {
    try {
        const response = await fetch(RAW_APK_URL, {
            headers: { 
                "User-Agent": "MagicWord-Updater" // 防止 GitHub 拦截
            }
        });

        if (response.ok) {
            const newHeaders = new Headers(response.headers);
            newHeaders.set("Content-Type", "application/vnd.android.package-archive");
            newHeaders.set("Content-Disposition", 'attachment; filename="MagicWordLatest.apk"');
            newHeaders.set("Access-Control-Allow-Origin", "*");
            return new Response(response.body, { status: 200, headers: newHeaders });
        } else {
            return new Response(`File not found on GitHub Raw (Status: ${response.status})`, { status: 404 });
        }
    } catch (error) {
        return new Response(`Proxy Error: ${error.message}`, { status: 502 });
    }
}

async function fetchNoticeJson() {
    try {
        const response = await fetch(NOTICE_JSON_URL, {
            headers: { 
                "User-Agent": "MagicWord-Updater" 
            }
        });

        if (response.ok) {
            const newHeaders = new Headers(response.headers);
            newHeaders.set("Content-Type", "application/json; charset=utf-8");
            newHeaders.set("Access-Control-Allow-Origin", "*");
            return new Response(response.body, { status: 200, headers: newHeaders });
        } else {
            return new Response(`Notice file not found (Status: ${response.status})`, { status: 404 });
        }
    } catch (error) {
        return new Response(`Proxy Error: ${error.message}`, { status: 502 });
    }
}

async function proxyRequest(request, targetUrl, targetHost) {
    const requestHeaders = new Headers(request.headers); 
    requestHeaders.set('Host', targetHost); 
    requestHeaders.set('User-Agent', 'MagicWord-Proxy'); // 统一 UA
    requestHeaders.set('X-Forwarded-Host', targetHost); 
    requestHeaders.set('X-Forwarded-Proto', 'https'); 
    
    // 移除敏感/干扰头
    requestHeaders.delete('cf-connecting-ip'); 
    requestHeaders.delete('cf-ray'); 
    requestHeaders.delete('cf-visitor'); 
    requestHeaders.delete('Authorization'); 
    requestHeaders.delete('Cookie');
     
    try { 
        const response = await fetch(targetUrl, { 
            method: request.method, 
            headers: requestHeaders, 
            body: request.method !== 'GET' && request.method !== 'HEAD' ? await request.clone().arrayBuffer() : undefined, 
            redirect: 'follow' 
        }); 
         
        return processResponse(response); 
         
    } catch (error) { 
        console.error('Proxy error:', error); 
        return new Response(`Proxy Error: ${error.message}`, { 
            status: 502, 
            headers: { 'Content-Type': 'text/plain' } 
        }); 
    } 
} 

async function processResponse(response) { 
    const responseHeaders = new Headers(response.headers); 
     
    // 允许跨域
    responseHeaders.set('Access-Control-Allow-Origin', '*'); 
     
    // 移除可能导致问题的安全头
    const problematicHeaders = [ 
        'content-security-policy', 
        'content-security-policy-report-only', 
        'x-frame-options', 
        'strict-transport-security', 
        'x-content-type-options', 
        'permissions-policy' 
    ]; 
     
    problematicHeaders.forEach(header => responseHeaders.delete(header)); 
     
    return new Response(response.body, { 
        status: response.status, 
        headers: responseHeaders 
    }); 
}
