
// ============================================================
// MagicWord Cloudflare Worker Script
// ============================================================

// --- 配置区域 (Configuration) ---

// 1. GitHub 仓库信息
const GH_USERNAME = "lijiaxu2021";
const GH_REPO_APP = "MagicWord";       // 主项目仓库 (存放 APK, notice.json)
const GH_REPO_DATA = "magicwordfile";  // 数据仓库 (存放词库文件)
const GH_BRANCH = "main";

// 2. 关键文件直链 (自动生成)
// 注意：GitHub Raw 可能会有缓存，建议请求时带上时间戳
const RAW_BASE = "https://raw.githubusercontent.com";
const URL_APK = `${RAW_BASE}/${GH_USERNAME}/${GH_REPO_APP}/${GH_BRANCH}/MagicWordLatest.apk`;
const URL_NOTICE = `${RAW_BASE}/${GH_USERNAME}/${GH_REPO_APP}/${GH_BRANCH}/notice.json`;
const URL_LIB_INDEX = `${RAW_BASE}/${GH_USERNAME}/${GH_REPO_DATA}/${GH_BRANCH}/index.json`;

// 3. GitHub Token (用于上传词库)
// 建议在 Cloudflare Worker 的 Settings -> Variables 中配置 GITHUB_TOKEN，不要直接硬编码在这里。
// 如果必须硬编码，请替换下方字符串。
const GITHUB_TOKEN = typeof GH_TOKEN !== 'undefined' ? GH_TOKEN : "YOUR_GITHUB_TOKEN_HERE";

// 4. 其他配置
const TARGET_API_DOMAIN = 'api.github.com';

// --- 事件监听 ---

addEventListener('fetch', event => { 
    event.respondWith(handleRequest(event.request).catch(err => {
        return new Response(`Server Error: ${err.message}`, { status: 500 });
    })); 
}); 

// --- 主处理逻辑 ---

async function handleRequest(request) { 
    const url = new URL(request.url); 
    const path = url.pathname;

    // Route 1: APK 下载 (支持 /MagicWordLatest.apk 和 /latest/MagicWord.apk)
    if (path === `/MagicWordLatest.apk` || path === "/latest/MagicWord.apk") { 
        return fetchRawFile(URL_APK, "application/vnd.android.package-archive", 'attachment; filename="MagicWordLatest.apk"');
    }

    // Route 2: 通知文件 (notice.json)
    if (path === '/notice.json') {
        return fetchRawFile(URL_NOTICE, "application/json; charset=utf-8");
    }
    
    // Route 3: 在线词库索引 (index.json / index_X.json / num.json / tags.json)
    // 兼容 /library/index.json 和 /library/file/index.json
    if (path === '/library/index.json') {
         // 指向 magicwordfile/index.json (虽然现在似乎是用 index_0.json 分页了，保留此兼容)
        return fetchRawFile(URL_LIB_INDEX, "application/json; charset=utf-8");
    }

    // Route 4: 词库文件通用代理 (/library/file/...)
    // 映射到 magicwordfile 仓库的对应文件
    if (path.startsWith('/library/file/')) {
        const filePath = path.replace('/library/file/', '');
        const targetUrl = `${RAW_BASE}/${GH_USERNAME}/${GH_REPO_DATA}/${GH_BRANCH}/${filePath}`;
        return fetchRawFile(targetUrl, "application/json; charset=utf-8");
    }

    // Route 5: 词库上传接口 (POST /library/upload)
    if (path === '/library/upload' && request.method === 'POST') {
        return handleLibraryUpload(request);
    }
     
    // Route 6: GitHub API 代理 (用于其他 API 调用)
    if (path.startsWith('/api/')) {
        const newPath = path.replace(/^\/api/, '');
        const targetUrl = `https://${TARGET_API_DOMAIN}${newPath}${url.search}`;
        return proxyRequest(request, targetUrl, TARGET_API_DOMAIN);
    }

    // Default: 欢迎页面
    return new Response(`MagicWord Server is Running.\nVersion: 1.0.1\nRepo: ${GH_USERNAME}/${GH_REPO_APP}`, { 
        status: 200,
        headers: { "Content-Type": "text/plain" }
    });
} 

// --- 辅助函数 ---

/**
 * 代理 GitHub Raw 文件
 * @param {string} targetUrl 目标 Raw URL
 * @param {string} contentType 响应 Content-Type
 * @param {string} contentDisposition 响应 Content-Disposition (可选)
 */
async function fetchRawFile(targetUrl, contentType, contentDisposition) {
    try {
        // 添加时间戳防止 CF 边缘缓存过久
        const urlWithTs = `${targetUrl}?t=${Date.now()}`;
        
        const response = await fetch(urlWithTs, {
            headers: { 
                "User-Agent": "MagicWord-Proxy-Worker" 
            }
        });

        if (response.ok) {
            const newHeaders = new Headers(response.headers);
            if (contentType) newHeaders.set("Content-Type", contentType);
            if (contentDisposition) newHeaders.set("Content-Disposition", contentDisposition);
            
            // 允许跨域
            newHeaders.set("Access-Control-Allow-Origin", "*");
            
            return new Response(response.body, { status: 200, headers: newHeaders });
        } else {
            return new Response(`File not found on GitHub Raw.\nTarget: ${targetUrl}\nStatus: ${response.status}`, { status: 404 });
        }
    } catch (error) {
        return new Response(`Proxy Error: ${error.message}`, { status: 502 });
    }
}

/**
 * 处理词库上传
 */
async function handleLibraryUpload(request) {
    try {
        const body = await request.json();
        const { name, description, contentBase64, tags } = body;

        if (!name || !contentBase64) {
            return new Response(JSON.stringify({ error: "Missing name or content" }), { status: 400 });
        }

        const timestamp = Date.now().toString();
        
        // 1. Upload info.json
        const infoJson = JSON.stringify({
            id: timestamp,
            name: name,
            description: description || "",
            tags: tags || [],
            timestamp: parseInt(timestamp),
            author: "User"
        }, null, 2);
        
        // Base64 encode info.json (handling utf-8)
        function toBase64(str) {
            return btoa(unescape(encodeURIComponent(str)));
        }

        const infoUpload = await uploadToGithub(`${timestamp}/info.json`, toBase64(infoJson));
        if (!infoUpload.ok) {
            const err = await infoUpload.text();
            throw new Error(`Failed to upload info.json: ${err}`);
        }

        // 2. Upload library.json
        const libUpload = await uploadToGithub(`${timestamp}/library.json`, contentBase64);
        if (!libUpload.ok) {
             const err = await libUpload.text();
             throw new Error(`Failed to upload library.json: ${err}`);
        }

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

/**
 * 上传文件到 magicwordfile 仓库
 */
async function uploadToGithub(path, contentBase64) {
    // 使用 GH_REPO_DATA (magicwordfile)
    const url = `https://api.github.com/repos/${GH_USERNAME}/${GH_REPO_DATA}/contents/${path}`;
    
    // 检查 Token
    if (GITHUB_TOKEN === "YOUR_GITHUB_TOKEN_HERE") {
        return { ok: false, text: () => Promise.resolve("GITHUB_TOKEN not configured in Worker") };
    }

    return await fetch(url, {
        method: "PUT",
        headers: {
            "Authorization": `Bearer ${GITHUB_TOKEN}`,
            "User-Agent": "MagicWord-Proxy-Worker",
            "Content-Type": "application/json",
            "Accept": "application/vnd.github.v3+json"
        },
        body: JSON.stringify({
            message: `Upload ${path} (via App)`,
            content: contentBase64
        })
    });
}

/**
 * 通用请求代理
 */
async function proxyRequest(request, targetUrl, targetHost) {
    const requestHeaders = new Headers(request.headers); 
    requestHeaders.set('Host', targetHost); 
    requestHeaders.set('User-Agent', 'MagicWord-Proxy-Worker'); 
    
    // 清理 Cloudflare 特有头和敏感头
    ['cf-connecting-ip', 'cf-ray', 'cf-visitor', 'Authorization', 'Cookie'].forEach(h => requestHeaders.delete(h));
     
    try { 
        const response = await fetch(targetUrl, { 
            method: request.method, 
            headers: requestHeaders, 
            body: request.method !== 'GET' && request.method !== 'HEAD' ? await request.clone().arrayBuffer() : undefined, 
            redirect: 'follow' 
        }); 
        
        // 处理响应头
        const responseHeaders = new Headers(response.headers); 
        responseHeaders.set('Access-Control-Allow-Origin', '*'); 
        
        // 移除安全限制头以便前端使用
        ['content-security-policy', 'x-frame-options'].forEach(h => responseHeaders.delete(h));
         
        return new Response(response.body, { 
            status: response.status, 
            headers: responseHeaders 
        }); 
    } catch (error) { 
        return new Response(`Proxy Error: ${error.message}`, { status: 502 }); 
    } 
}
