import requests

PROXY_BASE_URL = "https://mag.upxuu.com"
NOTICE_URL = f"{PROXY_BASE_URL}/notice.json"
APK_URL = f"{PROXY_BASE_URL}/MagicWordLatest.apk"

def verify_notice():
    print(f"Checking Notice URL: {NOTICE_URL}")
    try:
        response = requests.get(NOTICE_URL)
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            print("Notice Content:")
            print(response.text)
        else:
            print("Failed to fetch notice.")
    except Exception as e:
        print(f"Error: {e}")

def verify_apk():
    print(f"Checking APK URL: {APK_URL}")
    try:
        response = requests.head(APK_URL) # Use HEAD to avoid downloading large file
        print(f"Status Code: {response.status_code}")
        print("Headers:")
        for k, v in response.headers.items():
            print(f"{k}: {v}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    verify_notice()
    print("-" * 20)
    verify_apk()
