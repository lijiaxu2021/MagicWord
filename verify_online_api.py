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
    
    # Create dummy library content
    dummy_library = {
        "name": "Python Test Library",
        "words": [
            {"term": "Hello", "definition": "你好"},
            {"term": "World", "definition": "世界"}
        ]
    }
    
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
            print("Response:", response.text)
        else:
            print("Upload Failed!")
            print("Response:", response.text)
    except Exception as e:
        print(f"Request Failed: {e}")
    print("-" * 30)

if __name__ == "__main__":
    print("Starting API Verification...")
    test_get_index()
    test_upload_library()
    print("Verification Complete.")
