/**
 * Simple MCP client example for testing the MasterControl Android app.
 * This script demonstrates how to connect to the MCP server and call tools.
 * 
 * Requirements:
 *     npm install ws
 * 
 * Usage:
 *     node mcp_client_example.js
 */

const WebSocket = require('ws');

function sendRequest(ws, method, params = null, requestId = 1) {
    return new Promise((resolve, reject) => {
        const request = {
            jsonrpc: "2.0",
            id: requestId,
            method: method
        };
        
        if (params) {
            request.params = params;
        }
        
        console.log(`\n→ Sending: ${JSON.stringify(request, null, 2)}`);
        
        // Set up one-time listener for response
        const responseHandler = (data) => {
            console.log(`← Received: ${data}`);
            ws.removeListener('message', responseHandler);
            resolve(JSON.parse(data));
        };
        
        ws.on('message', responseHandler);
        ws.send(JSON.stringify(request));
    });
}

async function main() {
    console.log("Connecting to MCP server at ws://localhost:8080/mcp...");
    
    const ws = new WebSocket('ws://localhost:8080/mcp');
    
    await new Promise((resolve, reject) => {
        ws.on('open', resolve);
        ws.on('error', reject);
    });
    
    console.log("✓ Connected!");
    
    try {
        // 1. Initialize
        console.log("\n=== 1. Initialize ===");
        let response = await sendRequest(ws, "initialize", null, 1);
        console.log(`Server: ${response.result.serverInfo.name} v${response.result.serverInfo.version}`);
        
        // 2. List tools
        console.log("\n=== 2. List Available Tools ===");
        response = await sendRequest(ws, "tools/list", null, 2);
        const tools = response.result.tools;
        console.log(`Found ${tools.length} tools:`);
        tools.forEach(tool => {
            console.log(`  - ${tool.name}: ${tool.description}`);
        });
        
        // 3. Call echo tool
        console.log("\n=== 3. Call Echo Tool ===");
        response = await sendRequest(ws, "tools/call", {
            name: "echo",
            arguments: {
                message: "Hello from Node.js client!"
            }
        }, 3);
        const echoResult = response.result.content[0].text;
        console.log(`Echo result: ${echoResult}`);
        
        // 4. Get device info
        console.log("\n=== 4. Get Device Info ===");
        response = await sendRequest(ws, "tools/call", {
            name: "get_device_info",
            arguments: {}
        }, 4);
        const deviceInfo = response.result.content[0].text;
        console.log(`Device info:\n${deviceInfo}`);
        
        // 5. Send notification
        console.log("\n=== 5. Send Notification ===");
        response = await sendRequest(ws, "tools/call", {
            name: "send_notification",
            arguments: {
                title: "MCP Test",
                message: "Notification sent from Node.js client!"
            }
        }, 5);
        const notifResult = response.result.content[0].text;
        console.log(`Notification result: ${notifResult}`);
        
        console.log("\n✓ All tests completed successfully!");
        
    } catch (error) {
        console.error(`\n✗ Error: ${error.message}`);
    } finally {
        ws.close();
        console.log("\n✓ Connection closed");
    }
}

main().catch(error => {
    console.error(`\n✗ Error: ${error.message}`);
    console.log("\nMake sure:");
    console.log("  1. The MasterControl app is running on your device");
    console.log("  2. The MCP server is started (tap 'Start MCP Server' button)");
    console.log("  3. You're connected to the same network or using adb port forwarding:");
    console.log("     adb forward tcp:8080 tcp:8080");
    process.exit(1);
});
