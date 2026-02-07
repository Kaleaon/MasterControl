package com.kaleaon.mastercontrol

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var btnStartServer: Button
    private lateinit var btnStopServer: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvServerInfo: TextView
    
    private var mcpService: MCPServerService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MCPServerService.LocalBinder
            mcpService = binder.getService()
            isBound = true
            updateUI()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            mcpService = null
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        btnStartServer = findViewById(R.id.btn_start_server)
        btnStopServer = findViewById(R.id.btn_stop_server)
        tvStatus = findViewById(R.id.tv_status)
        tvServerInfo = findViewById(R.id.tv_server_info)
        
        btnStartServer.setOnClickListener {
            startMCPServer()
        }
        
        btnStopServer.setOnClickListener {
            stopMCPServer()
        }
        
        // Start periodic UI updates
        startUIUpdates()
    }
    
    override fun onStart() {
        super.onStart()
        // Bind to MCPServerService
        Intent(this, MCPServerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    private fun startMCPServer() {
        val intent = Intent(this, MCPServerService::class.java)
        startForegroundService(intent)
        updateUI()
    }
    
    private fun stopMCPServer() {
        val intent = Intent(this, MCPServerService::class.java)
        stopService(intent)
        updateUI()
    }
    
    private fun startUIUpdates() {
        lifecycleScope.launch {
            while (true) {
                updateUI()
                delay(1000) // Update every second
            }
        }
    }
    
    private fun updateUI() {
        val isRunning = mcpService?.isServerRunning() ?: false
        
        btnStartServer.isEnabled = !isRunning
        btnStopServer.isEnabled = isRunning
        
        tvStatus.text = if (isRunning) {
            "Status: Running"
        } else {
            "Status: Stopped"
        }
        
        tvServerInfo.text = if (isRunning) {
            """
            Server Information:
            - Port: ${mcpService?.getServerPort() ?: "N/A"}
            - Protocol: MCP 1.0
            - Transport: WebSocket
            - Tools Available: ${mcpService?.getToolCount() ?: 0}
            """.trimIndent()
        } else {
            "Server is not running"
        }
    }
}
