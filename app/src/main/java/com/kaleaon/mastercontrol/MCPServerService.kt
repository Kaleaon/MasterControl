package com.kaleaon.mastercontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Duration

class MCPServerService : Service() {
    
    private val binder = LocalBinder()
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var isRunning = false
    private val serverPort = 8080
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    // MCP Tools registry
    private val tools = mutableListOf<MCPTool>()
    
    inner class LocalBinder : Binder() {
        fun getService(): MCPServerService = this@MCPServerService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerDefaultTools()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification())
            startServer()
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        serviceScope.cancel()
    }
    
    private fun startServer() {
        serviceScope.launch {
            try {
                server = embeddedServer(CIO, port = serverPort) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    
                    routing {
                        get("/") {
                            call.respondText("MasterControl MCP Server is running")
                        }
                        
                        webSocket("/mcp") {
                            handleMCPConnection(this)
                        }
                    }
                }
                
                server?.start(wait = false)
                isRunning = true
                updateNotification("Server running on port $serverPort")
            } catch (e: Exception) {
                isRunning = false
                e.printStackTrace()
            }
        }
    }
    
    private fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        isRunning = false
    }
    
    private suspend fun handleMCPConnection(session: DefaultWebSocketServerSession) {
        try {
            // Send initialization message
            val initMessage = MCPMessage(
                jsonrpc = "2.0",
                id = 1,
                method = "initialize",
                result = MCPInitResult(
                    protocolVersion = "1.0",
                    serverInfo = ServerInfo(
                        name = "MasterControl",
                        version = "1.0.0"
                    ),
                    capabilities = Capabilities(
                        tools = ToolsCapability(listChanged = false)
                    )
                )
            )
            
            session.send(Frame.Text(json.encodeToString(initMessage)))
            
            // Handle incoming messages
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val receivedText = frame.readText()
                    handleMCPRequest(session, receivedText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun handleMCPRequest(session: DefaultWebSocketServerSession, request: String) {
        try {
            val mcpRequest = json.decodeFromString<MCPRequest>(request)
            
            when (mcpRequest.method) {
                "tools/list" -> {
                    val response = MCPResponse(
                        jsonrpc = "2.0",
                        id = mcpRequest.id,
                        result = ToolsListResult(tools = tools)
                    )
                    session.send(Frame.Text(json.encodeToString(response)))
                }
                
                "tools/call" -> {
                    val toolName = mcpRequest.params?.name ?: ""
                    val tool = tools.find { it.name == toolName }
                    
                    val result = if (tool != null) {
                        ToolCallResult(
                            content = listOf(
                                TextContent(
                                    type = "text",
                                    text = "Tool '${tool.name}' executed successfully!"
                                )
                            )
                        )
                    } else {
                        ToolCallResult(
                            content = listOf(
                                TextContent(
                                    type = "text",
                                    text = "Error: Tool '$toolName' not found"
                                )
                            ),
                            isError = true
                        )
                    }
                    
                    val response = MCPResponse(
                        jsonrpc = "2.0",
                        id = mcpRequest.id,
                        result = result
                    )
                    session.send(Frame.Text(json.encodeToString(response)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun registerDefaultTools() {
        tools.clear()
        tools.add(
            MCPTool(
                name = "get_device_info",
                description = "Get Android device information",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = emptyMap()
                )
            )
        )
        tools.add(
            MCPTool(
                name = "send_notification",
                description = "Send a notification to the device",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "title" to mapOf("type" to "string", "description" to "Notification title"),
                        "message" to mapOf("type" to "string", "description" to "Notification message")
                    ),
                    required = listOf("title", "message")
                )
            )
        )
        tools.add(
            MCPTool(
                name = "echo",
                description = "Echo back a message",
                inputSchema = ToolInputSchema(
                    type = "object",
                    properties = mapOf(
                        "message" to mapOf("type" to "string", "description" to "Message to echo")
                    ),
                    required = listOf("message")
                )
            )
        )
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MCP Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MCP Server running notification"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String = "Starting..."): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MCP Server")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(message))
    }
    
    fun isServerRunning(): Boolean = isRunning
    
    fun getServerPort(): Int = serverPort
    
    fun getToolCount(): Int = tools.size
    
    companion object {
        private const val CHANNEL_ID = "mcp_server_channel"
        private const val NOTIFICATION_ID = 1
    }
}

// MCP Protocol Data Classes
@Serializable
data class MCPMessage(
    val jsonrpc: String,
    val id: Int? = null,
    val method: String? = null,
    val result: MCPInitResult? = null
)

@Serializable
data class MCPInitResult(
    val protocolVersion: String,
    val serverInfo: ServerInfo,
    val capabilities: Capabilities
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class Capabilities(
    val tools: ToolsCapability? = null
)

@Serializable
data class ToolsCapability(
    val listChanged: Boolean = false
)

@Serializable
data class MCPRequest(
    val jsonrpc: String,
    val id: Int? = null,
    val method: String,
    val params: ToolCallParams? = null
)

@Serializable
data class ToolCallParams(
    val name: String? = null,
    val arguments: Map<String, String>? = null
)

@Serializable
data class MCPResponse(
    val jsonrpc: String,
    val id: Int? = null,
    val result: Any? = null
)

@Serializable
data class ToolsListResult(
    val tools: List<MCPTool>
)

@Serializable
data class MCPTool(
    val name: String,
    val description: String,
    val inputSchema: ToolInputSchema
)

@Serializable
data class ToolInputSchema(
    val type: String,
    val properties: Map<String, Map<String, String>> = emptyMap(),
    val required: List<String> = emptyList()
)

@Serializable
data class ToolCallResult(
    val content: List<TextContent>,
    val isError: Boolean = false
)

@Serializable
data class TextContent(
    val type: String,
    val text: String
)
