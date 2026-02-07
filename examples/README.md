# MCP Client Examples

This directory contains example client implementations for testing the MasterControl MCP server.

## Prerequisites

Before running these examples, make sure:
1. The MasterControl Android app is installed and running on your device
2. The MCP server is started (tap "Start MCP Server" in the app)
3. You have set up port forwarding if testing from your development machine:
   ```bash
   adb forward tcp:8080 tcp:8080
   ```

## Python Example

### Setup
```bash
pip install websocket-client
```

### Run
```bash
python mcp_client_example.py
```

### What it does
The Python example demonstrates:
- Connecting to the MCP server via WebSocket
- Initializing the MCP connection
- Listing available tools
- Calling the `echo` tool
- Calling the `get_device_info` tool
- Calling the `send_notification` tool

## JavaScript Example

### Setup
```bash
npm install ws
```

### Run
```bash
node mcp_client_example.js
```

### What it does
The JavaScript example performs the same operations as the Python example:
- WebSocket connection
- Protocol initialization
- Tool listing and execution
- All three built-in tools (echo, get_device_info, send_notification)

## Expected Output

Both examples should produce output similar to:

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
