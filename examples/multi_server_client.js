/**
 * Multi-Server MCP Client Example (JavaScript/Node.js)
 * Demonstrates connecting to both MasterControl Android and Google Docs MCP
 * 
 * Prerequisites:
 *     npm install ws
 * 
 * Setup:
 *     1. Start MasterControl app on Android device
 *     2. Run: adb forward tcp:8080 tcp:8080
 *     3. Start Google Docs MCP server (see GOOGLE_DOCS_INTEGRATION.md)
 *     4. Update GOOGLE_DOCS_MCP_PATH below
 * 
 * Usage:
 *     node multi_server_client.js
 */

const WebSocket = require('ws');
const { spawn } = require('child_process');

// Configuration
const ANDROID_WS_URL = 'ws://localhost:8080/mcp';
const GOOGLE_DOCS_MCP_PATH = '/path/to/google-docs-mcp/build/index.js';

/**
 * StdioMCPClient - Client for stdio-based MCP servers like Google Docs MCP
 */
class StdioMCPClient {
    constructor(command, args = []) {
        this.requestId = 0;
        this.pendingRequests = new Map();
        
        this.process = spawn(command, args, {
            stdio: ['pipe', 'pipe', 'pipe']
        });
        
        this.process.stdout.on('data', (data) => {
            const lines = data.toString().split('\n').filter(l => l.trim());
            lines.forEach(line => {
                try {
                    const response = JSON.parse(line);
                    console.log('← Google Docs MCP:', JSON.stringify(response, null, 2));
                    
                    if (response.id && this.pendingRequests.has(response.id)) {
                        const resolve = this.pendingRequests.get(response.id);
                        this.pendingRequests.delete(response.id);
                        resolve(response);
                    }
                } catch (e) {
                    console.error('Failed to parse response:', line);
                }
            });
        });
        
        this.process.stderr.on('data', (data) => {
            console.error('Google Docs MCP Error:', data.toString());
        });
    }
    
    sendRequest(method, params = null) {
        return new Promise((resolve, reject) => {
            this.requestId++;
            const request = {
                jsonrpc: "2.0",
                id: this.requestId,
                method: method
            };
            
            if (params) {
                request.params = params;
            }
            
            console.log('→ Google Docs MCP:', JSON.stringify(request, null, 2));
            
            this.pendingRequests.set(this.requestId, resolve);
            
            this.process.stdin.write(JSON.stringify(request) + '\n');
            
            // Timeout after 10 seconds
            setTimeout(() => {
                if (this.pendingRequests.has(this.requestId)) {
                    this.pendingRequests.delete(this.requestId);
                    reject(new Error('Request timeout'));
                }
            }, 10000);
        });
    }
    
    close() {
        this.process.kill();
    }
}

/**
 * Send request to Android MCP server
 */
function sendAndroidRequest(ws, method, params = null, requestId = 1) {
    return new Promise((resolve, reject) => {
        const request = {
            jsonrpc: "2.0",
            id: requestId,
            method: method
        };
        
        if (params) {
            request.params = params;
        }
        
        console.log('\n→ Android MCP:', JSON.stringify(request, null, 2));
        
        const messageHandler = (data) => {
            const response = JSON.parse(data);
            console.log('← Android MCP:', data.toString());
            ws.removeListener('message', messageHandler);
            resolve(response);
        };
        
        ws.on('message', messageHandler);
        ws.send(JSON.stringify(request));
        
        setTimeout(() => {
            ws.removeListener('message', messageHandler);
            reject(new Error('Request timeout'));
        }, 5000);
    });
}

/**
 * Example 1: Android MCP Server Only
 */
async function exampleAndroidOnly() {
    console.log('='.repeat(60));
    console.log('Example 1: Android MCP Server Only');
    console.log('='.repeat(60));
    
    const ws = new WebSocket(ANDROID_WS_URL);
    
    await new Promise((resolve, reject) => {
        ws.on('open', resolve);
        ws.on('error', reject);
    });
    
    console.log('✓ Connected to Android MCP server');
    
    try {
        // Initialize
        const initResponse = await sendAndroidRequest(ws, 'initialize', null, 1);
        console.log(`✓ Server: ${initResponse.result.serverInfo.name}`);
        
        // List tools
        const toolsResponse = await sendAndroidRequest(ws, 'tools/list', null, 2);
        const tools = toolsResponse.result.tools;
        console.log(`\n✓ Found ${tools.length} Android tools:`);
        tools.forEach(tool => {
            console.log(`  - ${tool.name}: ${tool.description}`);
        });
        
        // Get device info
        const deviceResponse = await sendAndroidRequest(ws, 'tools/call', {
            name: 'get_device_info',
            arguments: {}
        }, 3);
        const deviceInfo = deviceResponse.result.content[0].text;
        console.log(`\n✓ Device Information:\n${deviceInfo}`);
        
        // Send notification
        const notifResponse = await sendAndroidRequest(ws, 'tools/call', {
            name: 'send_notification',
            arguments: {
                title: 'MCP Test',
                message: 'Successfully connected to Android MCP!'
            }
        }, 4);
        console.log(`\n✓ ${notifResponse.result.content[0].text}`);
        
    } finally {
        ws.close();
        console.log('\n✓ Disconnected from Android MCP');
    }
}

/**
 * Example 2: Both Android and Google Docs MCP
 */
async function exampleBothServers() {
    console.log('\n' + '='.repeat(60));
    console.log('Example 2: Both Android MCP and Google Docs MCP');
    console.log('='.repeat(60));
    
    // Connect to Android
    const androidWs = new WebSocket(ANDROID_WS_URL);
    await new Promise((resolve, reject) => {
        androidWs.on('open', resolve);
        androidWs.on('error', reject);
    });
    console.log('✓ Connected to Android MCP server');
    
    // Initialize Android
    await sendAndroidRequest(androidWs, 'initialize', null, 1);
    
    let googleClient = null;
    
    try {
        // Start Google Docs MCP
        console.log('\n✓ Starting Google Docs MCP server...');
        googleClient = new StdioMCPClient('node', [GOOGLE_DOCS_MCP_PATH]);
        
        // Initialize Google Docs MCP
        await googleClient.sendRequest('initialize');
        console.log('✓ Google Docs MCP initialized');
        
        // List tools from both servers
        console.log('\n--- Listing tools from both servers ---');
        
        const androidResponse = await sendAndroidRequest(androidWs, 'tools/list', null, 2);
        const androidTools = androidResponse.result.tools;
        console.log(`\nAndroid Tools (${androidTools.length}):`);
        androidTools.forEach(tool => {
            console.log(`  - ${tool.name}`);
        });
        
        const googleResponse = await googleClient.sendRequest('tools/list');
        if (googleResponse && googleResponse.result) {
            const googleTools = googleResponse.result.tools || [];
            console.log(`\nGoogle Docs Tools (${googleTools.length}):`);
            googleTools.slice(0, 5).forEach(tool => {
                console.log(`  - ${tool.name || 'unknown'}`);
            });
            if (googleTools.length > 5) {
                console.log(`  ... and ${googleTools.length - 5} more`);
            }
        }
        
        // Cross-server workflow example
        console.log('\n--- Cross-server workflow ---');
        const deviceResponse = await sendAndroidRequest(androidWs, 'tools/call', {
            name: 'get_device_info',
            arguments: {}
        }, 3);
        const deviceInfo = deviceResponse.result.content[0].text;
        console.log(`Device info retrieved from Android:\n${deviceInfo}`);
        
        // Example: You could create a Google Doc with device info here
        // const docResponse = await googleClient.sendRequest('create_document', {
        //     title: 'Device Report',
        //     content: deviceInfo
        // });
        
        console.log('\n✓ Multi-server workflow completed!');
        
    } catch (error) {
        console.error('\n✗ Error with Google Docs MCP:', error.message);
        console.log('   Make sure GOOGLE_DOCS_MCP_PATH is correct');
        console.log('   Continuing with Android-only...');
    } finally {
        if (googleClient) {
            googleClient.close();
        }
        androidWs.close();
        console.log('\n✓ Disconnected from all servers');
    }
}

/**
 * Main function
 */
async function main() {
    console.log('Multi-Server MCP Client Example');
    console.log('Connecting to MasterControl Android + Google Docs MCP');
    console.log();
    
    try {
        // Run Android-only example
        await exampleAndroidOnly();
        
        // Run both servers example (comment out if Google Docs MCP not available)
        // await exampleBothServers();
        
        console.log('\n' + '='.repeat(60));
        console.log('✓ All examples completed successfully!');
        console.log('='.repeat(60));
        
    } catch (error) {
        console.error('\n✗ Error:', error.message);
        console.log('\nTroubleshooting:');
        console.log('  1. Is MasterControl app running on Android?');
        console.log('  2. Did you run: adb forward tcp:8080 tcp:8080');
        console.log('  3. Is the MCP server started in the app?');
        console.log('  4. Is Google Docs MCP server path correct? (if using)');
        process.exit(1);
    }
}

main();
