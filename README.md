# MasterControl

An Android application for running a Model Context Protocol (MCP) server on your device.

## Overview

MasterControl is an Android app that implements a Model Context Protocol (MCP) server, allowing AI models and agents to interact with your Android device through a standardized protocol. The app provides a WebSocket-based MCP server that exposes various tools and capabilities.

## Features

- **MCP Server on Android**: Run a full-featured MCP server directly on your Android device
- **WebSocket Transport**: Connect to the server via WebSocket at `ws://localhost:8080/mcp`
- **Foreground Service**: Server runs as a foreground service with notification
- **Built-in Tools**: Includes sample tools like device info, notifications, and echo
- **Real-time UI**: Live status updates and server information display

## Technical Stack

- **Language**: Kotlin
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Server Framework**: Ktor (WebSocket + CIO)
- **Serialization**: Kotlinx Serialization
- **Architecture**: MVVM with Lifecycle-aware components

## MCP Protocol Implementation

The app implements the Model Context Protocol v1.0 with the following features:

- Protocol initialization and capability negotiation
- Tools listing and execution
- JSON-RPC 2.0 message format
- WebSocket transport layer

### Available Tools

1. **get_device_info**: Retrieve Android device information
2. **send_notification**: Send notifications to the device
3. **echo**: Echo back messages (useful for testing)

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+
- Gradle 8.0+

### Building the App

1. Clone the repository:
   ```bash
   git clone https://github.com/Kaleaon/MasterControl.git
   cd MasterControl
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run on your device or emulator

### Running the MCP Server

1. Launch the MasterControl app
2. Tap "Start MCP Server" button
3. The server will start on port 8080
4. Connect to it using any MCP client via WebSocket at `ws://localhost:8080/mcp`

### Connecting to the Server

You can connect to the MCP server using any WebSocket client that supports the MCP protocol:

```bash
# Example using wscat
wscat -c ws://localhost:8080/mcp
```

Or use the MCP SDK from your application to connect to the server.

## Project Structure

```
MasterControl/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/kaleaon/mastercontrol/
│   │       │   ├── MainActivity.kt          # Main UI activity
│   │       │   └── MCPServerService.kt      # MCP server service
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml    # Main UI layout
│   │       │   └── values/
│   │       │       ├── strings.xml
│   │       │       ├── colors.xml
│   │       │       └── themes.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Architecture

### MainActivity
- Provides UI for controlling the MCP server
- Displays server status and information
- Binds to MCPServerService for communication

### MCPServerService
- Implements the MCP server using Ktor
- Handles WebSocket connections
- Processes MCP protocol messages
- Manages tool registry and execution
- Runs as a foreground service

## MCP Protocol Flow

1. **Initialize**: Client connects and sends initialization request
2. **Capabilities**: Server responds with available capabilities
3. **Tools List**: Client can request available tools
4. **Tool Execution**: Client calls tools with parameters
5. **Response**: Server executes and returns results

## Permissions

The app requires the following permissions:

- `INTERNET`: For network communication
- `FOREGROUND_SERVICE`: To run the MCP server in background
- `FOREGROUND_SERVICE_DATA_SYNC`: For foreground service type
- `POST_NOTIFICATIONS`: To show service notifications

## Integration with Other MCP Servers

MasterControl can work alongside other MCP servers to create powerful multi-server workflows. For example, you can use it with the [Google Docs MCP server](https://github.com/a-bonus/google-docs-mcp) to enable AI agents to interact with both your Android device and Google Workspace.

**See [GOOGLE_DOCS_INTEGRATION.md](GOOGLE_DOCS_INTEGRATION.md) for:**
- How to connect to multiple MCP servers simultaneously
- Example workflows combining Android and Google Docs tools
- Complete setup instructions and code examples

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the MIT License.

## Acknowledgments

- Based on the Model Context Protocol specification by Anthropic
- Uses Ktor for WebSocket server implementation
- Built with Kotlin and Android Jetpack components

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/Kaleaon/MasterControl).
