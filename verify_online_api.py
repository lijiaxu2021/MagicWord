import requests
import json
import base64
import time

# Configuration
WORKER_DOMAIN = "mag.upxuu.com"
BASE_URL = f"https://{WORKER_DOMAIN}"

def test_get_index():
    print(f"Testing GET {BASE_URL}/library/index.json...")
    try:
        response = requests.get(f"{BASE_URL}/library/index.json")
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            print("Response Content:")
            try:
                print(json.dumps(response.json(), indent=2))
            except:
                print(response.text)
        else:
            print(f"Error: {response.text}")
    except Exception as e:
        print(f"Request Failed: {e}")
    print("-" * 30)

def test_upload_library():
    print(f"Testing POST {BASE_URL}/library/upload...")
    
    # Create dummy library content (as per ExportPackage structure or legacy list)
    # Using legacy list format for simplicity as supported by importLibraryJson
    dummy_library = [
        {
            "word": "hello_world_test",
            "definitionCn": "你好世界测试",
            "libraryId": 0,
            "createdAt": int(time.time() * 1000),
            "reviewCount": 0,
            "id": 0,
            "definitionEn": "Hello World Test",
            "example": "Hello World",
            "memoryMethod": "",
            "phonetic": "",
            "correctCount": 0,
            "incorrectCount": 0,
            "nextReviewTime": 0,
            "lastReviewTime": 0,
            "repetitions": 0,
            "interval": 0,
            "easinessFactor": 2.5,
            "sortOrder": 0
        }
    ]
    
    # Encode library content to Base64
    library_json = json.dumps(dummy_library)
    content_base64 = base64.b64encode(library_json.encode('utf-8')).decode('utf-8')
    
    payload = {
        "name": "Python Test Library",
        "description": f"Uploaded via Python script at {time.strftime('%Y-%m-%d %H:%M:%S')}",
        "contentBase64": content_base64
    }
    
    try:
        response = requests.post(f"{BASE_URL}/library/upload", json=payload)
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            print("Upload Success!")
            resp_json = response.json()
            print("Response:", resp_json)
            return resp_json.get("id") # Return ID for download test
        else:
            print("Upload Failed!")
            print("Response:", response.text)
            return None
    except Exception as e:
        print(f"Request Failed: {e}")
        return None
    print("-" * 30)

def test_download_library(timestamp_id):
    if not timestamp_id:
        print("Skipping download test (no ID).")
        return

    print(f"Testing GET {BASE_URL}/library/file/{timestamp_id}/library.json...")
    
    # Wait a bit? GitHub API is fast but raw might take a few seconds to propagate?
    # Actually, raw.githubusercontent.com has a cache (approx 5 mins).
    # But since we just uploaded via API, API read is instant, but RAW might lag.
    # Let's try anyway.
    
    try:
        url = f"{BASE_URL}/library/file/{timestamp_id}/library.json"
        print(f"URL: {url}")
        response = requests.get(url)
        print(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            print("Download Success!")
            print("Content Snippet:", response.text[:100])
        elif response.status_code == 404:
            print("Download 404 (Expected if Raw GitHub cache hasn't updated or repo is empty)")
            print("Note: Raw GitHub content may take up to 5 minutes to appear.")
        else:
            print(f"Download Failed: {response.text}")
            
    except Exception as e:
        print(f"Request Failed: {e}")
    print("-" * 30)

if __name__ == "__main__":
    print("Starting API Verification...")
    test_get_index()
    lib_id = test_upload_library()
    
    if lib_id:
        print("Waiting 15 seconds for GitHub Action to process and Raw Cache to update...")
        # Give GitHub Action some time to run and commit index.json
        for i in range(15):
            time.sleep(1)
            print(f"{15-i}...", end=" ", flush=True)
        print("\nChecking download and index...")
        
        test_download_library(lib_id)
        test_get_index() # Check if index updated
        
    print("Verification Complete.")
