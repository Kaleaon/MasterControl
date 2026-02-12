#!/usr/bin/env python3
"""
Simple MCP client example for testing the MasterControl Android app.
This script demonstrates how to connect to the MCP server and call tools.

Requirements:
    pip install websocket-client

Usage:
    python mcp_client_example.py
"""

import json
import websocket

def send_request(ws, method, params=None, request_id=1):
    """Send an MCP request and return the response."""
    request = {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": method
    }
    if params:
        request["params"] = params
    
    print(f"\n→ Sending: {json.dumps(request, indent=2)}")
    ws.send(json.dumps(request))
    
    response = ws.recv()
    print(f"← Received: {response}")
    return json.loads(response)

def main():
    # Connect to MCP server
    print("Connecting to MCP server at ws://localhost:8080/mcp...")
    ws = websocket.create_connection("ws://localhost:8080/mcp")
    print("✓ Connected!")
    
    try:
        # 1. Initialize
        print("\n=== 1. Initialize ===")
        response = send_request(ws, "initialize", request_id=1)
        print(f"Server: {response['result']['serverInfo']['name']} v{response['result']['serverInfo']['version']}")
        
        # 2. List tools
        print("\n=== 2. List Available Tools ===")
        response = send_request(ws, "tools/list", request_id=2)
        tools = response['result']['tools']
        print(f"Found {len(tools)} tools:")
        for tool in tools:
            print(f"  - {tool['name']}: {tool['description']}")
        
        # 3. Call echo tool
        print("\n=== 3. Call Echo Tool ===")
        response = send_request(
            ws,
            "tools/call",
            params={
                "name": "echo",
                "arguments": {
                    "message": "Hello from Python client!"
                }
            },
            request_id=3
        )
        result = response['result']['content'][0]['text']
        print(f"Echo result: {result}")
        
        # 4. Get device info
        print("\n=== 4. Get Device Info ===")
        response = send_request(
            ws,
            "tools/call",
            params={
                "name": "get_device_info",
                "arguments": {}
            },
            request_id=4
        )
        device_info = response['result']['content'][0]['text']
        print(f"Device info:\n{device_info}")
        
        # 5. Send notification
        print("\n=== 5. Send Notification ===")
        response = send_request(
            ws,
            "tools/call",
            params={
                "name": "send_notification",
                "arguments": {
                    "title": "MCP Test",
                    "message": "Notification sent from Python client!"
                }
            },
            request_id=5
        )
        result = response['result']['content'][0]['text']
        print(f"Notification result: {result}")
        
        print("\n✓ All tests completed successfully!")
        
    finally:
        ws.close()
        print("\n✓ Connection closed")

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"\n✗ Error: {e}")
        print("\nMake sure:")
        print("  1. The MasterControl app is running on your device")
        print("  2. The MCP server is started (tap 'Start MCP Server' button)")
        print("  3. You're connected to the same network or using adb port forwarding:")
        print("     adb forward tcp:8080 tcp:8080")
