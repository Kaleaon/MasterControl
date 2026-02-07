# Multi-Server MCP Architecture

## Overview Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MCP Client (AI Agent)                        │
│                      (Claude, ChatGPT, Custom)                      │
│                                                                     │
│  Capabilities from both servers:                                   │
│  ✓ Android device control (MasterControl)                          │
│  ✓ Google Workspace automation (Google Docs MCP)                   │
└────────────────┬──────────────────────────┬─────────────────────────┘
                 │                          │
                 │                          │
    ┌────────────▼───────────┐    ┌────────▼──────────────────────┐
    │  MasterControl Server  │    │  Google Docs MCP Server       │
    │  (Android Device)      │    │  (Computer/Cloud)             │
    │                        │    │                               │
    │  Port: 8080            │    │  Transport: stdio/WebSocket   │
    │  Protocol: WebSocket   │    │  Language: Node.js/TypeScript │
    │  Language: Kotlin      │    │                               │
    └────────────┬───────────┘    └────────┬──────────────────────┘
                 │                          │
                 │                          │
    ┌────────────▼───────────┐    ┌────────▼──────────────────────┐
    │  Android Device Tools  │    │  Google Workspace APIs        │
    │                        │    │                               │
    │  • Device Info         │    │  • Google Docs                │
    │  • Notifications       │    │  • Google Drive               │
    │  • Echo/Testing        │    │  • Google Sheets              │
    │  • [Add more tools]    │    │  • Comments & Collaboration   │
    └────────────────────────┘    └───────────────────────────────┘
```

## Connection Flow

### 1. Direct WebSocket Connection (MasterControl)

```
┌─────────┐                                    ┌──────────────────┐
│  Client │ ────WebSocket ws://localhost:8080/mcp──→ │ MasterControl  │
└─────────┘                                    │  (Android)       │
                                               └──────────────────┘
```

### 2. Stdio Connection (Google Docs MCP)

```
┌─────────┐                                    ┌──────────────────┐
│  Client │ ────stdin/stdout──────────────────→ │  Google Docs MCP │
└─────────┘                                    │  (Node.js)       │
                                               └──────────────────┘
```

### 3. Multi-Server Setup

```
                        ┌───────────────┐
                        │  MCP Client   │
                        │  (Python/JS)  │
                        └───────┬───────┘
                                │
                ┌───────────────┴───────────────┐
                │                               │
    ┌───────────▼──────────┐       ┌───────────▼──────────┐
    │  WebSocket Client    │       │   Stdio Client       │
    │  → Android:8080      │       │   → node process     │
    └──────────────────────┘       └──────────────────────┘
```

## Data Flow Example

### Workflow: Create Google Doc with Device Info

```
1. Client                                                    
   │
   ├─→ MasterControl.get_device_info()
   │   └─→ Returns: {device: "Pixel", model: "...", ...}
   │
   ├─→ GoogleDocsMCP.create_document()
   │   └─→ Parameters: {
   │           title: "Device Report",
   │           content: "Device: Pixel\nModel: ..."
   │       }
   │
   └─→ MasterControl.send_notification()
       └─→ Parameters: {
               title: "Document Created",
               message: "Report generated successfully"
           }
```

### Sequence Diagram

```
Client          Android MCP      Google Docs MCP     Google APIs
  │                  │                  │                  │
  ├─ initialize ────→│                  │                  │
  │←─ capabilities ──┤                  │                  │
  │                  │                  │                  │
  ├─ initialize ─────┼─────────────────→│                  │
  │←─ capabilities ──┼──────────────────┤                  │
  │                  │                  │                  │
  ├─ get_device_info→│                  │                  │
  │←─ device data ───┤                  │                  │
  │                  │                  │                  │
  ├─ create_document─┼─────────────────→│                  │
  │                  │                  ├─ API call ──────→│
  │                  │                  │←─ doc created ───┤
  │←─ success ───────┼──────────────────┤                  │
  │                  │                  │                  │
  ├─ send_notification→                 │                  │
  │←─ notification sent─                │                  │
  │                  │                  │                  │
```

## Deployment Scenarios

### Scenario 1: Local Development

```
┌────────────────────────────────────────┐
│         Developer Machine              │
│                                        │
│  ┌──────────────┐  ┌────────────────┐ │
│  │ Claude       │  │ Google Docs    │ │
│  │ Desktop      │  │ MCP Server     │ │
│  │              │  │ (Node.js)      │ │
│  └──────┬───────┘  └────────┬───────┘ │
│         │                   │         │
│         │         ┌─────────▼───────┐ │
│         │         │ Google Cloud    │ │
│         │         │ APIs            │ │
│         │         └─────────────────┘ │
└─────────┼─────────────────────────────┘
          │
          │ ADB Forward
          │ tcp:8080→8080
          │
┌─────────▼─────────────┐
│  Android Device       │
│                       │
│  ┌─────────────────┐  │
│  │ MasterControl   │  │
│  │ MCP Server      │  │
│  └─────────────────┘  │
└───────────────────────┘
```

### Scenario 2: Cloud + Mobile

```
┌─────────────────────┐
│   Client (Anywhere) │
└──────────┬──────────┘
           │
     ┌─────┴──────┐
     │            │
┌────▼────┐  ┌────▼─────────────┐
│ Cloud   │  │ Android Device   │
│ Google  │  │ (via VPN/Tunnel) │
│ Docs    │  │ MasterControl    │
│ MCP     │  └──────────────────┘
└─────────┘
```

## Tool Categories

### Android Tools (MasterControl)
```
┌─────────────────────────────┐
│     Device Control          │
├─────────────────────────────┤
│ ✓ get_device_info           │
│ ✓ send_notification         │
│ ✓ echo                      │
│                             │
│ Potential additions:        │
│ • camera_capture            │
│ • screen_capture            │
│ • location_info             │
│ • battery_status            │
│ • network_info              │
└─────────────────────────────┘
```

### Google Workspace Tools (Google Docs MCP)
```
┌─────────────────────────────┐
│   Document Management       │
├─────────────────────────────┤
│ • read_document             │
│ • create_document           │
│ • append_to_document        │
│ • format_text               │
│ • insert_table              │
│ • manage_comments           │
│                             │
│   Drive Operations          │
├─────────────────────────────┤
│ • list_files                │
│ • create_folder             │
│ • move_file                 │
│ • delete_file               │
│                             │
│   Spreadsheet Tools         │
├─────────────────────────────┤
│ • create_spreadsheet        │
│ • read_sheet                │
│ • write_data                │
│ • append_row                │
└─────────────────────────────┘
```

## Communication Patterns

### Pattern 1: Sequential Operations
```
Client → Android (get info) → Google Docs (create) → Android (notify)
```

### Pattern 2: Parallel Operations
```
        ┌→ Android (get info)
Client ─┤
        └→ Google Docs (list files)
```

### Pattern 3: Conditional Workflow
```
Client → Google Docs (check document)
   │
   ├─ If comments exist
   │    └→ Android (send notification)
   │
   └─ If no comments
        └→ (do nothing)
```

## Security Considerations

```
┌──────────────────────────────────────────┐
│          Security Layers                 │
├──────────────────────────────────────────┤
│  1. Network Security                     │
│     • WebSocket over localhost           │
│     • Optional: WSS for remote           │
│     • ADB port forwarding                │
│                                          │
│  2. Authentication                       │
│     • Google OAuth 2.0 (Google Docs)     │
│     • Android permissions (MasterControl)│
│                                          │
│  3. Authorization                        │
│     • Scoped API access (Google)         │
│     • Android runtime permissions        │
│                                          │
│  4. Data Protection                      │
│     • Encrypted credentials              │
│     • Secure token storage               │
│     • No credential logging              │
└──────────────────────────────────────────┘
```

## Performance Characteristics

```
┌──────────────────────┬─────────────┬──────────────┐
│      Operation       │   Latency   │   Bandwidth  │
├──────────────────────┼─────────────┼──────────────┤
│ Android local call   │   < 10ms    │   Low        │
│ Google API call      │   100-500ms │   Medium     │
│ Document creation    │   500-2000ms│   High       │
│ Notification         │   < 50ms    │   Very Low   │
└──────────────────────┴─────────────┴──────────────┘
```

## Best Practices

1. **Error Handling**
   - Implement retry logic for network failures
   - Graceful degradation when one server is unavailable
   - Clear error messages to users

2. **Resource Management**
   - Close connections when done
   - Respect API rate limits
   - Clean up temporary resources

3. **Testing**
   - Test each server independently
   - Test multi-server workflows
   - Include timeout scenarios

4. **Monitoring**
   - Log all server interactions
   - Track success/failure rates
   - Monitor response times

## References

- [MasterControl Documentation](README.md)
- [Google Docs MCP Integration Guide](GOOGLE_DOCS_INTEGRATION.md)
- [Model Context Protocol Spec](https://modelcontextprotocol.io/)
- [Google Docs MCP Repository](https://github.com/a-bonus/google-docs-mcp)
