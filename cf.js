
// 目标域名配置
const TARGET_DOMAIN = 'api.github.com'; // 默认代理 GitHub API
const RAW_DOMAIN = 'raw.githubusercontent.com'; // Raw 文件域名
const WORKER_DOMAIN = 'mag.upxuu.com'; // 您的 Worker 域名

// APK 直链配置
const RAW_APK_URL = "https://raw.githubusercontent.com/lijiaxu2021/MagicWord/main/MagicWordLatest.apk";
// 通知文件直链配置
const NOTICE_JSON_URL = "https://raw.githubusercontent.com/lijiaxu2021/MagicWord/main/notice.json";
// 在线词库索引直链
const LIBRARY_INDEX_URL = "https://raw.githubusercontent.com/lijiaxu2021/magicwordfile/main/index.json";

// GitHub Token (Replace with your actual token when deploying)
const GH_TOKEN = "YOUR_GITHUB_TOKEN_HERE";
const REPO_OWNER = "lijiaxu2021";
const REPO_NAME = "magicwordfile";

addEventListener('fetch', event => { 
    event.respondWith(handleRequest(event.request, event)); 
}); 

async function handleRequest(request, event) { 
    const url = new URL(request.url); 
    
    // 1. APK 直链下载 (最优先)
    if (url.pathname === `/MagicWordLatest.apk` || url.pathname === "/latest/MagicWord.apk") { 
        return fetchRawApk();
    }

    // 2. 通知文件代理
    if (url.pathname === '/notice.json') {
        return fetchNoticeJson();
    }
    
    // 3. 在线词库索引代理
    if (url.pathname === '/library/index.json') {
        return fetchLibraryIndex();
    }

    // 4. 词库上传接口
    if (url.pathname === '/library/upload' && request.method === 'POST') {
        return handleLibraryUpload(request);
    }
     
    // 5. API 代理逻辑
    if (url.pathname.startsWith('/api/')) {
        const newPath = url.pathname.replace(/^\/api/, '');
        const targetUrl = `https://${TARGET_DOMAIN}${newPath}${url.search}`;
        return proxyRequest(request, targetUrl, TARGET_DOMAIN);
    }

    // 6. 兜底/默认页面
    return new Response("MagicWord Proxy Service is Running.", { status: 200 });
} 

async function fetchRawApk() {
    return proxyRawFile(RAW_APK_URL, "application/vnd.android.package-archive", 'attachment; filename="MagicWordLatest.apk"');
}

async function fetchNoticeJson() {
    return proxyRawFile(NOTICE_JSON_URL, "application/json; charset=utf-8");
}

async function fetchLibraryIndex() {
    return proxyRawFile(LIBRARY_INDEX_URL, "application/json; charset=utf-8");
}

async function proxyRawFile(targetUrl, contentType, contentDisposition) {
    try {
        const response = await fetch(targetUrl, {
            headers: { "User-Agent": "MagicWord-Updater" }
        });

        if (response.ok) {
            const newHeaders = new Headers(response.headers);
            if (contentType) newHeaders.set("Content-Type", contentType);
            if (contentDisposition) newHeaders.set("Content-Disposition", contentDisposition);
            newHeaders.set("Access-Control-Allow-Origin", "*");
            return new Response(response.body, { status: 200, headers: newHeaders });
        } else {
            return new Response(`File not found (Status: ${response.status})`, { status: 404 });
        }
    } catch (error) {
        return new Response(`Proxy Error: ${error.message}`, { status: 502 });
    }
}

async function handleLibraryUpload(request) {
    try {
        const body = await request.json();
        const { name, description, contentBase64 } = body;

        if (!name || !contentBase64) {
            return new Response(JSON.stringify({ error: "Missing name or content" }), { status: 400 });
        }

        const timestamp = Date.now().toString();
        
        // 1. Upload info.json
        const infoJson = JSON.stringify({
            id: timestamp,
            name: name,
            description: description || "",
            timestamp: parseInt(timestamp),
            author: "User" // Could be enhanced
        });
        const infoUpload = await uploadToGithub(`${timestamp}/info.json`, btoa(unescape(encodeURIComponent(infoJson))));
        if (!infoUpload.ok) throw new Error("Failed to upload info.json");

        // 2. Upload library.json
        const libUpload = await uploadToGithub(`${timestamp}/library.json`, contentBase64);
        if (!libUpload.ok) throw new Error("Failed to upload library.json");

        return new Response(JSON.stringify({ success: true, id: timestamp }), { 
            status: 200,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
        });

    } catch (error) {
        return new Response(JSON.stringify({ error: error.message }), { 
            status: 500,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
        });
    }
}

async function uploadToGithub(path, contentBase64) {
    const url = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/contents/${path}`;
    return await fetch(url, {
        method: "PUT",
        headers: {
            "Authorization": `Bearer ${GH_TOKEN}`,
            "User-Agent": "MagicWord-Proxy",
            "Content-Type": "application/json",
            "Accept": "application/vnd.github.v3+json"
        },
        body: JSON.stringify({
            message: `Upload ${path}`,
            content: contentBase64
        })
    });
}

async function proxyRequest(request, targetUrl, targetHost) {
    const requestHeaders = new Headers(request.headers); 
    requestHeaders.set('Host', targetHost); 
    requestHeaders.set('User-Agent', 'MagicWord-Proxy'); 
    requestHeaders.set('X-Forwarded-Host', targetHost); 
    requestHeaders.set('X-Forwarded-Proto', 'https'); 
    
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
        return new Response(`Proxy Error: ${error.message}`, { status: 502 }); 
    } 
} 

async function processResponse(response) { 
    const responseHeaders = new Headers(response.headers); 
    responseHeaders.set('Access-Control-Allow-Origin', '*'); 
    
    const problematicHeaders = [ 
        'content-security-policy', 'content-security-policy-report-only', 
        'x-frame-options', 'strict-transport-security', 
        'x-content-type-options', 'permissions-policy' 
    ]; 
    problematicHeaders.forEach(header => responseHeaders.delete(header)); 
     
    return new Response(response.body, { 
        status: response.status, 
        headers: responseHeaders 
    }); 
}
