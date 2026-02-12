# How to Run Google Docs MCP with MasterControl

**Quick Answer:** MasterControl and Google Docs MCP work together in a **dual-server architecture**. Both servers run simultaneously, and MCP clients (like Claude Desktop or custom scripts) connect to both.

## TL;DR - Quick Setup

```bash
# 1. Start MasterControl on Android
# - Install and launch the app
# - Tap "Start MCP Server"
# - Run: adb forward tcp:8080 tcp:8080

# 2. Start Google Docs MCP on your computer
cd google-docs-mcp
npm install
npm start

# 3. Use both servers with Claude Desktop or custom client
# See examples/multi_server_client.py or examples/multi_server_client.js
```

## Why This Works

The Google Docs MCP server is a **Node.js application** that:
- Cannot run on Android (requires Node.js runtime)
- Connects to Google Workspace APIs
- Runs on your computer or in the cloud

MasterControl is an **Android application** that:
- Runs natively on Android devices
- Provides device-level tools
- Runs a WebSocket MCP server

Together, they give AI agents access to **both** platforms!

## Architecture

```
         ┌─────────────────────┐
         │    AI Agent         │
         │  (Claude/Custom)    │
         └──────┬──────┬───────┘
                │      │
        ┌───────┘      └────────┐
        │                       │
  ┌─────▼─────┐          ┌──────▼──────┐
  │ Android   │          │  Google     │
  │ MCP       │          │  Docs MCP   │
  │ (Device)  │          │  (Computer) │
  └───────────┘          └─────────────┘
```

## Complete Setup Guide

### Step 1: Install Google Docs MCP (on your computer)

```bash
# Clone the repository
git clone https://github.com/a-bonus/google-docs-mcp.git
cd google-docs-mcp

# Install dependencies
npm install

# Set up Google OAuth credentials
# 1. Go to https://console.cloud.google.com
# 2. Create a new project
# 3. Enable Google Docs, Drive, and Sheets APIs
# 4. Create OAuth 2.0 credentials
# 5. Download credentials.json to this directory

# Build and start
npm run build
npm start
```

### Step 2: Install MasterControl (on Android)

```bash
# Clone and build
git clone https://github.com/Kaleaon/MasterControl.git
cd MasterControl

# Open in Android Studio and build
# Or use Gradle:
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Set up port forwarding
adb forward tcp:8080 tcp:8080
```

### Step 3: Use Both Servers

#### Option A: Claude Desktop

Edit `~/.config/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "google-docs": {
      "command": "node",
      "args": ["/path/to/google-docs-mcp/build/index.js"]
    },
    "android": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/client-websocket", "ws://localhost:8080/mcp"]
    }
  }
}
```

#### Option B: Python Client

```python
# See examples/multi_server_client.py
python examples/multi_server_client.py
```

#### Option C: JavaScript Client

```javascript
// See examples/multi_server_client.js
node examples/multi_server_client.js
```

## Example Workflows

### 1. Create Document with Device Info

```python
# Get device info from Android
device = await android_client.call_tool("get_device_info", {})

# Create Google Doc with device details
doc = await google_client.call_tool("create_document", {
    "title": f"Device Report - {device['model']}",
    "content": device_info
})

# Notify on Android
await android_client.call_tool("send_notification", {
    "title": "Document Created",
    "message": f"Created: {doc['title']}"
})
```

### 2. Comment Notifications

```python
# Check for comments in a Google Doc
comments = await google_client.call_tool("list_comments", {
    "document_id": "YOUR_DOC_ID"
})

# Send Android notification if there are unresolved comments
if comments['unresolved_count'] > 0:
    await android_client.call_tool("send_notification", {
        "title": "Google Docs Alert",
        "message": f"{comments['unresolved_count']} comments need review"
    })
```

## Available Tools

### From MasterControl (Android)
- `get_device_info` - Device details (model, manufacturer, Android version)
- `send_notification` - Send notifications to the device
- `echo` - Test connectivity

### From Google Docs MCP (Computer)
- `read_document` - Read Google Doc content
- `create_document` - Create new documents
- `list_files` - List Google Drive files
- `create_spreadsheet` - Create Google Sheets
- `append_to_document` - Add content to documents
- `manage_comments` - Handle document comments
- And 20+ more...

## Troubleshooting

### "Cannot connect to Android server"
- Is MasterControl app running?
- Is the MCP server started in the app?
- Did you run `adb forward tcp:8080 tcp:8080`?

### "Google Docs MCP won't start"
- Is Node.js installed? (`node --version`)
- Are Google OAuth credentials configured?
- Check the error logs in the terminal

### "Client connects to only one server"
- Test each server independently first
- Check your client configuration
- Review the example code in `examples/`

## Documentation

- **[GOOGLE_DOCS_INTEGRATION.md](GOOGLE_DOCS_INTEGRATION.md)** - Complete integration guide
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture and diagrams
- **[examples/README.md](examples/README.md)** - Client examples documentation
- **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** - MasterControl development guide

## Examples

All examples are in the `examples/` directory:

1. **mcp_client_example.py** - Basic Android-only Python client
2. **mcp_client_example.js** - Basic Android-only JavaScript client
3. **multi_server_client.py** - Multi-server Python client ⭐ NEW
4. **multi_server_client.js** - Multi-server JavaScript client ⭐ NEW

## Benefits of This Setup

✅ **One AI Agent, Two Platforms** - Control both Android and Google Workspace  
✅ **Standardized Protocol** - MCP makes integration seamless  
✅ **Extensible** - Add more MCP servers as needed  
✅ **Production Ready** - Full error handling and logging  
✅ **Well Documented** - Comprehensive guides and examples  

## Next Steps

1. **Try the basic examples** - Get familiar with each server individually
2. **Run multi-server examples** - See the integration in action
3. **Build your workflow** - Combine tools from both servers
4. **Extend as needed** - Add more tools or servers

## Support

- **MasterControl Issues**: https://github.com/Kaleaon/MasterControl/issues
- **Google Docs MCP Issues**: https://github.com/a-bonus/google-docs-mcp/issues
- **MCP Protocol**: https://modelcontextprotocol.io/

---

**Bottom Line**: Run both servers side-by-side and connect your AI agent to both. It's that simple! 🚀
