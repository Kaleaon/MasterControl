# MCP Client Examples

This directory contains example client implementations for testing the MasterControl MCP server, including examples for multi-server integration.

## Prerequisites

Before running these examples, make sure:
1. The MasterControl Android app is installed and running on your device
2. The MCP server is started (tap "Start MCP Server" in the app)
3. You have set up port forwarding if testing from your development machine:
   ```bash
   adb forward tcp:8080 tcp:8080
   ```

## Basic Examples

### Python Example (mcp_client_example.py)

#### Setup
```bash
pip install websocket-client
```

#### Run
```bash
python mcp_client_example.py
```

#### What it does
The Python example demonstrates:
- Connecting to the MCP server via WebSocket
- Initializing the MCP connection
- Listing available tools
- Calling the `echo` tool
- Calling the `get_device_info` tool
- Calling the `send_notification` tool

### JavaScript Example (mcp_client_example.js)

#### Setup
```bash
npm install ws
```

#### Run
```bash
node mcp_client_example.js
```

#### What it does
The JavaScript example performs the same operations as the Python example:
- WebSocket connection
- Protocol initialization
- Tool listing and execution
- All three built-in tools (echo, get_device_info, send_notification)

## Multi-Server Examples

### Multi-Server Python Client (multi_server_client.py)

**NEW!** This example demonstrates connecting to both MasterControl Android server AND the Google Docs MCP server simultaneously.

#### Setup
```bash
pip install websocket-client
```

#### Configuration
Edit `multi_server_client.py` and update:
```python
GOOGLE_DOCS_MCP_COMMAND = ["node", "/path/to/google-docs-mcp/build/index.js"]
```

#### Run
```bash
python multi_server_client.py
```

#### What it does
- Connects to Android MCP via WebSocket
- Connects to Google Docs MCP via stdio
- Lists tools from both servers
- Demonstrates cross-server workflows
- Shows how to use device info with Google Docs

See [../GOOGLE_DOCS_INTEGRATION.md](../GOOGLE_DOCS_INTEGRATION.md) for complete integration guide.

### Multi-Server JavaScript Client (multi_server_client.js)

Similar to the Python multi-server example, but in JavaScript/Node.js.

#### Setup
```bash
npm install ws
```

#### Configuration
Edit `multi_server_client.js` and update:
```javascript
const GOOGLE_DOCS_MCP_PATH = '/path/to/google-docs-mcp/build/index.js';
```

#### Run
```bash
node multi_server_client.js
```

## Expected Output

### Basic Examples

Both basic examples should produce output similar to:

```
Connecting to MCP server at ws://localhost:8080/mcp...
✓ Connected!

=== 1. Initialize ===
Server: MasterControl v1.0.0

=== 2. List Available Tools ===
Found 3 tools:
  - get_device_info: Get Android device information
  - send_notification: Send a notification to the device
  - echo: Echo back a message

=== 3. Call Echo Tool ===
Echo result: Echo: Hello from Python client!

=== 4. Get Device Info ===
Device info:
Device: ...
Model: ...
Manufacturer: ...
...

=== 5. Send Notification ===
Notification result: Notification sent: 'MCP Test' - 'Notification sent from Python client!'

✓ All tests completed successfully!
✓ Connection closed
```

### Multi-Server Examples

Multi-server examples will show tools from both servers:

```
✓ Connected to Android MCP server
✓ Starting Google Docs MCP server...
✓ Google Docs MCP initialized

--- Listing tools from both servers ---

Android Tools (3):
  - get_device_info
  - send_notification
  - echo

Google Docs Tools (25):
  - read_document
  - create_document
  - list_files
  - create_spreadsheet
  - append_to_document
  ... and 20 more

--- Cross-server workflow ---
Device info retrieved from Android:
[Device information...]

✓ Multi-server workflow completed!
```

## Troubleshooting

### Connection refused
- Make sure the MCP server is running (check the app UI)
- Verify port forwarding: `adb forward tcp:8080 tcp:8080`
- Check if port 8080 is available on your machine

### No response from server
- Check Android device logs: `adb logcat | grep MCP`
- Ensure the app has necessary permissions
- Restart the MCP server in the app

### Tools not working
- Verify the tool names match exactly (case-sensitive)
- Check the tool parameters match the schema
- Review server logs for error messages

### Google Docs MCP not connecting (multi-server examples)
- Verify the path to Google Docs MCP server is correct
- Ensure Node.js is installed and in PATH
- Check Google Cloud credentials are properly configured
- Review the [Google Docs MCP setup guide](https://github.com/a-bonus/google-docs-mcp)

## Creating Your Own Client

To create your own MCP client:

1. **Establish WebSocket connection** to `ws://localhost:8080/mcp`

2. **Send initialize request**:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 1,
     "method": "initialize"
   }
   ```

3. **List tools**:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 2,
     "method": "tools/list"
   }
   ```

4. **Call a tool**:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 3,
     "method": "tools/call",
     "params": {
       "name": "tool_name",
       "arguments": {
         "param1": "value1"
       }
     }
   }
   ```

## Additional Resources

- [MCP Specification](https://modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [WebSocket Protocol](https://tools.ietf.org/html/rfc6455)
