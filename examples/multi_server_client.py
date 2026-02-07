#!/usr/bin/env python3
"""
Multi-Server MCP Client Example
Demonstrates connecting to both MasterControl Android server and Google Docs MCP server

Prerequisites:
    pip install websocket-client

Setup:
    1. Start MasterControl app on Android device
    2. Run: adb forward tcp:8080 tcp:8080
    3. Start Google Docs MCP server (see GOOGLE_DOCS_INTEGRATION.md)
    4. Update GOOGLE_DOCS_MCP_COMMAND below

Usage:
    python multi_server_client.py
"""

import json
import subprocess
import websocket
import threading
import time

# Configuration
ANDROID_WS_URL = "ws://localhost:8080/mcp"
GOOGLE_DOCS_MCP_COMMAND = ["node", "/path/to/google-docs-mcp/build/index.js"]

class StdioMCPClient:
    """Simple MCP client for stdio-based servers like Google Docs MCP"""
    
    def __init__(self, command):
        self.process = subprocess.Popen(
            command,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1
        )
        self.request_id = 0
    
    def send_request(self, method, params=None):
        self.request_id += 1
        request = {
            "jsonrpc": "2.0",
            "id": self.request_id,
            "method": method
        }
        if params:
            request["params"] = params
        
        request_json = json.dumps(request)
        print(f"→ Google Docs MCP: {request_json}")
        self.process.stdin.write(request_json + "\n")
        self.process.stdin.flush()
        
        # Read response
        response_line = self.process.stdout.readline()
        print(f"← Google Docs MCP: {response_line.strip()}")
        return json.loads(response_line) if response_line else None
    
    def close(self):
        self.process.terminate()
        self.process.wait()

def send_android_request(ws, method, params=None, request_id=1):
    """Send request to Android MCP server"""
    request = {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": method
    }
    if params:
        request["params"] = params
    
    print(f"\n→ Android MCP: {json.dumps(request, indent=2)}")
    ws.send(json.dumps(request))
    
    response = ws.recv()
    print(f"← Android MCP: {response}")
    return json.loads(response)

def example_workflow_android_only():
    """Example using just the Android server"""
    print("=" * 60)
    print("Example 1: Android MCP Server Only")
    print("=" * 60)
    
    ws = websocket.create_connection(ANDROID_WS_URL)
    print("✓ Connected to Android MCP server")
    
    try:
        # Initialize
        response = send_android_request(ws, "initialize", request_id=1)
        print(f"✓ Server: {response['result']['serverInfo']['name']}")
        
        # List tools
        response = send_android_request(ws, "tools/list", request_id=2)
        tools = response['result']['tools']
        print(f"\n✓ Found {len(tools)} Android tools:")
        for tool in tools:
            print(f"  - {tool['name']}: {tool['description']}")
        
        # Get device info
        response = send_android_request(
            ws,
            "tools/call",
            params={"name": "get_device_info", "arguments": {}},
            request_id=3
        )
        device_info = response['result']['content'][0]['text']
        print(f"\n✓ Device Information:\n{device_info}")
        
        # Send notification
        response = send_android_request(
            ws,
            "tools/call",
            params={
                "name": "send_notification",
                "arguments": {
                    "title": "MCP Test",
                    "message": "Successfully connected to Android MCP!"
                }
            },
            request_id=4
        )
        print(f"\n✓ {response['result']['content'][0]['text']}")
        
    finally:
        ws.close()
        print("\n✓ Disconnected from Android MCP")

def example_workflow_both_servers():
    """Example using both Android and Google Docs MCP servers"""
    print("\n" + "=" * 60)
    print("Example 2: Both Android MCP and Google Docs MCP")
    print("=" * 60)
    
    # Connect to Android
    android_ws = websocket.create_connection(ANDROID_WS_URL)
    print("✓ Connected to Android MCP server")
    
    # Initialize Android connection
    send_android_request(android_ws, "initialize", request_id=1)
    
    try:
        # Start Google Docs MCP client
        print("\n✓ Starting Google Docs MCP server...")
        google_client = StdioMCPClient(GOOGLE_DOCS_MCP_COMMAND)
        
        # Initialize Google Docs MCP
        google_client.send_request("initialize")
        print("✓ Google Docs MCP initialized")
        
        # List tools from both servers
        print("\n--- Listing tools from both servers ---")
        
        android_response = send_android_request(android_ws, "tools/list", request_id=2)
        android_tools = android_response['result']['tools']
        print(f"\nAndroid Tools ({len(android_tools)}):")
        for tool in android_tools:
            print(f"  - {tool['name']}")
        
        google_response = google_client.send_request("tools/list")
        if google_response and 'result' in google_response:
            google_tools = google_response['result'].get('tools', [])
            print(f"\nGoogle Docs Tools ({len(google_tools)}):")
            for tool in google_tools[:5]:  # Show first 5
                print(f"  - {tool.get('name', 'unknown')}")
            if len(google_tools) > 5:
                print(f"  ... and {len(google_tools) - 5} more")
        
        # Example: Get device info and notify
        print("\n--- Cross-server workflow ---")
        device_response = send_android_request(
            android_ws,
            "tools/call",
            params={"name": "get_device_info", "arguments": {}},
            request_id=3
        )
        device_info = device_response['result']['content'][0]['text']
        print(f"Device info retrieved from Android:\n{device_info}")
        
        # You could use device_info with Google Docs here, for example:
        # google_client.send_request("create_document", {
        #     "title": f"Device Report",
        #     "content": device_info
        # })
        
        print("\n✓ Multi-server workflow completed!")
        
        google_client.close()
        
    except FileNotFoundError:
        print("\n⚠ Google Docs MCP not found. Running Android-only example.")
        print("   Update GOOGLE_DOCS_MCP_COMMAND in this script to enable.")
    except Exception as e:
        print(f"\n✗ Error with Google Docs MCP: {e}")
        print("   Continuing with Android-only...")
    finally:
        android_ws.close()
        print("\n✓ Disconnected from all servers")

def main():
    print("Multi-Server MCP Client Example")
    print("Connecting to MasterControl Android + Google Docs MCP")
    print()
    
    try:
        # Run Android-only example
        example_workflow_android_only()
        
        # Run both servers example (comment out if Google Docs MCP not available)
        # example_workflow_both_servers()
        
        print("\n" + "=" * 60)
        print("✓ All examples completed successfully!")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        print("\nTroubleshooting:")
        print("  1. Is MasterControl app running on Android?")
        print("  2. Did you run: adb forward tcp:8080 tcp:8080")
        print("  3. Is the MCP server started in the app?")
        print("  4. Is Google Docs MCP server path correct? (if using)")

if __name__ == "__main__":
    main()
