# Integrating MasterControl with Google Docs MCP Server

This guide explains how to use the MasterControl Android MCP server together with the [Google Docs MCP server](https://github.com/a-bonus/google-docs-mcp) to enable AI agents to interact with both your Android device and Google Workspace.

## Architecture Overview

The integration uses a **dual-server architecture** where:
1. **MasterControl** runs on your Android device, providing device-level tools
2. **Google Docs MCP** runs on your computer (Node.js), providing Google Workspace tools
3. **MCP Client** (like Claude Desktop) connects to both servers simultaneously

```
┌─────────────────────────────────────────────┐
│           MCP Client (Claude)               │
│                                             │
│  Discovers and uses tools from both:        │
│  - Android device (MasterControl)           │
│  - Google Workspace (Google Docs MCP)       │
└─────────────────┬───────────────┬───────────┘
                  │               │
                  │               │
        ┌─────────▼─────┐   ┌────▼──────────────────┐
        │ MasterControl  │   │ Google Docs MCP       │
        │ (Android)      │   │ (Node.js on PC)       │
        │                │   │                       │
        │ Tools:         │   │ Tools:                │
        │ - Device Info  │   │ - Read/Write Docs     │
        │ - Notifications│   │ - Manage Drive        │
        │ - Echo         │   │ - Spreadsheets        │
        └────────────────┘   └───────────────────────┘
```

## Setup Instructions

### 1. Set Up Google Docs MCP Server (on your computer)

First, set up the Google Docs MCP server on your development machine:

```bash
# Clone the Google Docs MCP repository
git clone https://github.com/a-bonus/google-docs-mcp.git
cd google-docs-mcp

# Install dependencies
npm install

# Set up Google OAuth credentials
# Follow the instructions in the repository's README to:
# 1. Create a Google Cloud project
# 2. Enable Google Docs, Drive, and Sheets APIs
# 3. Create OAuth 2.0 credentials
# 4. Download credentials.json

# Build and start the server
npm run build
npm start
```

The Google Docs MCP server will typically run using stdio transport (standard input/output).

### 2. Set Up MasterControl (on your Android device)

1. Install the MasterControl app on your Android device
2. Launch the app and tap "Start MCP Server"
3. The server will start on `ws://localhost:8080/mcp`

### 3. Configure Port Forwarding (if needed)

If you want to connect from your computer to the Android MCP server:

```bash
# Forward Android port to your computer
adb forward tcp:8080 tcp:8080
```

Now the Android server is accessible at `ws://localhost:8080/mcp` from your computer.

## Connection Methods

### Method 1: Claude Desktop with Multiple Servers

Configure Claude Desktop to connect to both servers by editing your Claude Desktop configuration file.

**Location:**
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

**Configuration:**

```json
{
  "mcpServers": {
    "google-docs": {
      "command": "node",
      "args": ["/path/to/google-docs-mcp/build/index.js"],
      "env": {
        "GOOGLE_APPLICATION_CREDENTIALS": "/path/to/credentials.json"
      }
    },
    "android-device": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/client-websocket", "ws://localhost:8080/mcp"]
    }
  }
}
```

**Note:** The `@modelcontextprotocol/client-websocket` package provides WebSocket client capabilities for MCP.

### Method 2: Custom MCP Client Script

Create a custom client that connects to both servers:

```python
#!/usr/bin/env python3
"""
Multi-server MCP client that connects to both Android and Google Docs servers
"""

import asyncio
import json
from mcp import ClientSession, StdioServerParameters
from mcp.client.websocket import websocket_client

async def main():
    # Connect to Google Docs MCP (stdio)
    google_docs_params = StdioServerParameters(
        command="node",
        args=["/path/to/google-docs-mcp/build/index.js"],
        env={"GOOGLE_APPLICATION_CREDENTIALS": "/path/to/credentials.json"}
    )
    
    async with ClientSession() as google_session:
        await google_session.initialize(google_docs_params)
        
        # Connect to Android MCP (WebSocket)
        async with websocket_client("ws://localhost:8080/mcp") as android_session:
            await android_session.initialize()
            
            # List tools from both servers
            google_tools = await google_session.list_tools()
            android_tools = await android_session.list_tools()
            
            print("Google Docs Tools:", google_tools)
            print("Android Tools:", android_tools)
            
            # Example: Use a tool from each server
            # Get device info from Android
            device_info = await android_session.call_tool(
                "get_device_info",
                arguments={}
            )
            print("Device Info:", device_info)
            
            # Read a Google Doc
            doc_content = await google_session.call_tool(
                "read_document",
                arguments={"document_id": "YOUR_DOC_ID"}
            )
            print("Doc Content:", doc_content)

if __name__ == "__main__":
    asyncio.run(main())
```

### Method 3: JavaScript/Node.js Multi-Server Client

```javascript
const { Client } = require('@modelcontextprotocol/sdk/client/index.js');
const { StdioClientTransport } = require('@modelcontextprotocol/sdk/client/stdio.js');
const WebSocket = require('ws');

async function main() {
    // Connect to Google Docs MCP
    const googleTransport = new StdioClientTransport({
        command: 'node',
        args: ['/path/to/google-docs-mcp/build/index.js'],
        env: {
            GOOGLE_APPLICATION_CREDENTIALS: '/path/to/credentials.json'
        }
    });
    
    const googleClient = new Client({
        name: 'multi-client',
        version: '1.0.0'
    }, {
        capabilities: {}
    });
    
    await googleClient.connect(googleTransport);
    
    // Connect to Android MCP
    const androidWs = new WebSocket('ws://localhost:8080/mcp');
    
    androidWs.on('open', async () => {
        // Initialize
        androidWs.send(JSON.stringify({
            jsonrpc: "2.0",
            id: 1,
            method: "initialize"
        }));
    });
    
    androidWs.on('message', (data) => {
        const response = JSON.parse(data);
        console.log('Android Response:', response);
    });
    
    // List tools from both
    const googleTools = await googleClient.listTools();
    console.log('Google Tools:', googleTools);
    
    // Use tools from both servers as needed
}

main();
```

## Use Cases

### 1. Document Automation with Device Context

Use device information to personalize Google Docs:

```python
# Get device info from Android
device_info = await android_client.call_tool("get_device_info", {})

# Create a document with device information
await google_client.call_tool("create_document", {
    "title": f"Device Report - {device_info['model']}",
    "content": f"Device: {device_info['device']}\nManufacturer: {device_info['manufacturer']}"
})
```

### 2. Notification-Driven Workflows

Send Android notifications based on Google Docs events:

```python
# Check for comments in a document
comments = await google_client.call_tool("list_comments", {
    "document_id": "YOUR_DOC_ID"
})

# Notify on Android if there are unresolved comments
if comments['unresolved_count'] > 0:
    await android_client.call_tool("send_notification", {
        "title": "Google Docs Alert",
        "message": f"You have {comments['unresolved_count']} unresolved comments"
    })
```

### 3. Mobile Document Creation

Create documents from your Android device through the proxy:

```python
# Android app triggers document creation via MCP
await google_client.call_tool("create_document", {
    "title": "Mobile Note",
    "content": "Created from my Android device via MasterControl"
})
```

## Available Tools

### MasterControl (Android) Tools

| Tool | Description | Parameters |
|------|-------------|------------|
| `get_device_info` | Get Android device information | None |
| `send_notification` | Send a notification to the device | `title`, `message` |
| `echo` | Echo back a message | `message` |

### Google Docs MCP Tools (examples)

| Tool | Description | Key Parameters |
|------|-------------|----------------|
| `read_document` | Read a Google Doc | `document_id` |
| `create_document` | Create a new Google Doc | `title`, `content` |
| `append_to_document` | Append text to a document | `document_id`, `text` |
| `list_files` | List files in Google Drive | `query`, `page_size` |
| `create_spreadsheet` | Create a new Google Sheet | `title` |
| `read_spreadsheet` | Read spreadsheet data | `spreadsheet_id` |

For a complete list of Google Docs MCP tools, see the [Google Docs MCP documentation](https://github.com/a-bonus/google-docs-mcp).

## Troubleshooting

### Can't Connect to Android Server

1. Ensure MasterControl app is running and server is started
2. Check port forwarding: `adb forward tcp:8080 tcp:8080`
3. Verify firewall settings allow WebSocket connections

### Google Docs MCP Authentication Issues

1. Verify Google Cloud credentials are correct
2. Ensure all required APIs are enabled in Google Cloud Console
3. Check OAuth consent screen configuration
4. Review the Google Docs MCP server logs

### Both Servers Not Responding

1. Test each server individually first
2. Check for port conflicts
3. Verify MCP client configuration
4. Review logs from both servers

## Advanced: MCP Proxy Pattern

For more advanced use cases, you can create a proxy server that:
1. Accepts MCP connections from clients
2. Forwards requests to the appropriate backend (Android or Google Docs)
3. Aggregates responses

This allows you to expose a single MCP endpoint that routes to multiple backends.

## Best Practices

1. **Security**: Use secure connections (WSS) for production
2. **Error Handling**: Implement retry logic for network failures
3. **Rate Limiting**: Respect Google API rate limits
4. **Credentials**: Never commit credentials to version control
5. **Logging**: Enable detailed logging for debugging
6. **Testing**: Test each server independently before integrating

## Example Workflow

Here's a complete workflow example:

```python
async def document_workflow():
    # 1. Get device information
    device = await android_client.call_tool("get_device_info", {})
    
    # 2. Create a document with device info
    doc = await google_client.call_tool("create_document", {
        "title": f"Report from {device['model']}",
        "content": f"Generated on: {device['device']}"
    })
    
    # 3. Add more content
    await google_client.call_tool("append_to_document", {
        "document_id": doc['id'],
        "text": "\n\nThis document was created automatically."
    })
    
    # 4. Notify on Android
    await android_client.call_tool("send_notification", {
        "title": "Document Created",
        "message": f"Created: {doc['title']}"
    })
    
    return doc
```

## Resources

- [MasterControl Documentation](../README.md)
- [Google Docs MCP Repository](https://github.com/a-bonus/google-docs-mcp)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [MCP SDK Documentation](https://github.com/modelcontextprotocol/typescript-sdk)

## Contributing

Found a better way to integrate these servers? Please contribute to the documentation or create example scripts!

## Support

For issues related to:
- **MasterControl**: [GitHub Issues](https://github.com/Kaleaon/MasterControl/issues)
- **Google Docs MCP**: [Google Docs MCP Issues](https://github.com/a-bonus/google-docs-mcp/issues)
