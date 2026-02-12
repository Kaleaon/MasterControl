# MasterControl - Developer Guide

## Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK (API 24+)
- Git

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/Kaleaon/MasterControl.git
   cd MasterControl
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository directory
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew build
   ```
   Or use Android Studio: Build → Make Project

4. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Run → Run 'app'

## Project Structure

```
MasterControl/
├── app/
│   ├── src/main/
│   │   ├── java/com/kaleaon/mastercontrol/
│   │   │   ├── MainActivity.kt          # Main UI and controls
│   │   │   └── MCPServerService.kt      # MCP server implementation
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml # UI layout
│   │   │   └── values/                  # Resources
│   │   └── AndroidManifest.xml          # App configuration
│   ├── build.gradle.kts                 # App-level build config
│   └── proguard-rules.pro               # ProGuard rules
├── build.gradle.kts                     # Project-level build config
├── settings.gradle.kts                  # Project settings
└── gradle.properties                    # Gradle properties
```

## Key Components

### MainActivity
The main activity provides:
- UI for starting/stopping the MCP server
- Real-time status display
- Server information (port, tools count)
- Lifecycle-aware UI updates

Key features:
- Binds to MCPServerService for communication
- Uses coroutines with lifecycle awareness
- Updates UI only when activity is in started state

### MCPServerService
A foreground service that implements:
- Ktor-based WebSocket server on port 8080
- MCP 1.0 protocol with JSON-RPC 2.0
- Tool registry and execution
- Notification management

Key features:
- Runs as foreground service (persists in background)
- Client-initiated protocol handshake
- Type-safe JSON serialization
- Error handling and logging

## MCP Protocol Flow

1. **Client connects** via WebSocket to `ws://localhost:8080/mcp`
2. **Client sends** initialize request:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 1,
     "method": "initialize"
   }
   ```
3. **Server responds** with capabilities:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 1,
     "result": {
       "protocolVersion": "1.0",
       "serverInfo": {
         "name": "MasterControl",
         "version": "1.0.0"
       },
       "capabilities": {
         "tools": {
           "listChanged": false
         }
       }
     }
   }
   ```
4. **Client requests** available tools:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 2,
     "method": "tools/list"
   }
   ```
5. **Client calls** a tool:
   ```json
   {
     "jsonrpc": "2.0",
     "id": 3,
     "method": "tools/call",
     "params": {
       "name": "echo",
       "arguments": {
         "message": "Hello, MCP!"
       }
     }
   }
   ```

## Available Tools

### 1. get_device_info
Returns Android device information including:
- Device name and model
- Manufacturer
- Android version
- SDK level
- Brand and product

**Parameters:** None

**Example:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "get_device_info"
  }
}
```

### 2. send_notification
Sends a notification to the Android device.

**Parameters:**
- `title` (string, required): Notification title
- `message` (string, required): Notification message

**Example:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "send_notification",
    "arguments": {
      "title": "Test Notification",
      "message": "This is a test message"
    }
  }
}
```

### 3. echo
Echoes back the provided message.

**Parameters:**
- `message` (string, required): Message to echo

**Example:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "echo",
    "arguments": {
      "message": "Hello, World!"
    }
  }
}
```

## Adding New Tools

To add a new tool:

1. **Register the tool** in `registerDefaultTools()`:
   ```kotlin
   tools.add(
       MCPTool(
           name = "my_tool",
           description = "Description of what the tool does",
           inputSchema = ToolInputSchema(
               type = "object",
               properties = mapOf(
                   "param1" to mapOf("type" to "string", "description" to "Parameter description")
               ),
               required = listOf("param1")
           )
       )
   )
   ```

2. **Implement the tool** in `executeTool()`:
   ```kotlin
   "my_tool" -> {
       val param1 = arguments["param1"] ?: ""
       // Your tool logic here
       ToolCallResult(
           content = listOf(
               TextContent(
                   type = "text",
                   text = "Tool executed successfully!"
               )
           )
       )
   }
   ```

## Testing

### Manual Testing
1. Build and install the app on a device/emulator
2. Start the MCP server using the app UI
3. Connect using a WebSocket client:
   ```bash
   # Using wscat
   npm install -g wscat
   wscat -c ws://localhost:8080/mcp
   ```
4. Send MCP protocol messages and verify responses

### Debugging
- Enable verbose logging in `MCPServerService` by adding `println()` statements
- Use Android Studio's Logcat to view service logs
- Use Android Studio's Network Inspector for WebSocket traffic

## Build Configuration

### Gradle Properties
Centralized version management in `gradle.properties`:
- `kotlinVersion`: Kotlin version (1.9.20)
- `androidGradlePluginVersion`: Android Gradle Plugin version (8.2.0)

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with ProGuard optimization

### Dependencies
Key dependencies:
- AndroidX Core and AppCompat
- Material Components
- Kotlin Coroutines
- Ktor Server (CIO, WebSockets)
- Kotlinx Serialization

## Troubleshooting

### Build Issues
- **Gradle sync fails**: Check internet connection and repository accessibility
- **Dependency resolution errors**: Update Gradle wrapper or clear cache
  ```bash
  ./gradlew clean
  rm -rf ~/.gradle/caches
  ```

### Runtime Issues
- **Server won't start**: Check permissions in AndroidManifest.xml
- **WebSocket connection fails**: Ensure port 8080 is not in use
- **Tools not executing**: Check Logcat for exceptions in `executeTool()`

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit: `git commit -m "Add my feature"`
6. Push: `git push origin feature/my-feature`
7. Create a Pull Request

## License

This project is licensed under the MIT License.

## Resources

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Ktor Documentation](https://ktor.io/)
- [Android Developer Guide](https://developer.android.com/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## Support

For issues and questions:
- GitHub Issues: https://github.com/Kaleaon/MasterControl/issues
- Discussions: https://github.com/Kaleaon/MasterControl/discussions
