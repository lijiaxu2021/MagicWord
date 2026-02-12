import requests
import json

PROXY_BASE_URL = "https://mag.upxuu.com"
GITHUB_OWNER = "lijiaxu2021"
GITHUB_REPO = "MagicWord"

def verify_download():
    url = f"{PROXY_BASE_URL}/MagicWordLatest.apk"
    print(f"Testing APK Download: {url}")
    try:
        # Don't download the whole file, just check headers
        response = requests.get(url, stream=True)
        print(f"Status Code: {response.status_code}")
        print(f"Headers: {response.headers}")
        
        if response.status_code == 200:
            print("✅ APK Download URL is working!")
            # Read first 100 bytes to verify it's not an HTML error page
            first_bytes = next(response.iter_content(100))
            print(f"First 100 bytes: {first_bytes}")
        else:
            print("❌ APK Download Failed!")
            print(f"Response Content: {response.text[:500]}")
    except Exception as e:
        print(f"❌ APK Download Error: {e}")

def verify_api():
    url = f"{PROXY_BASE_URL}/api/repos/{GITHUB_OWNER}/{GITHUB_REPO}/releases/latest"
    print(f"\nTesting API Proxy: {url}")
    try:
        response = requests.get(url)
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ API working! Latest tag: {data.get('tag_name')}")
            # Check assets
            assets = data.get('assets', [])
            apk_asset = next((a for a in assets if a['name'].endswith('.apk')), None)
            if apk_asset:
                print(f"✅ Found APK asset: {apk_asset['name']}")
            else:
                print("⚠️ No APK asset found in latest release")
        else:
            print("❌ API Request Failed!")
            print(f"Response Content: {response.text[:500]}")
    except Exception as e:
        print(f"❌ API Error: {e}")

if __name__ == "__main__":
    print("=== MagicWord Proxy Verification ===\n")
    verify_download()
    verify_api()
