#include "stdafx.h"
#include "AmanServer.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <atomic>
#include <winsock2.h>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <string>
#include <windows.h>

#pragma comment(lib, "ws2_32.lib")  // Link with the Winsock library

// Helper function for debug logging in DLLs
void DebugOut(const std::string& message) {
    std::string logMsg = "[AmanServer] " + message + "\n";
    OutputDebugStringA(logMsg.c_str());
}

AmanServer::AmanServer() : isRunning(false), clientConnected(false), listenSocket(INVALID_SOCKET), clientSocket(INVALID_SOCKET) {
    startServer();
}

AmanServer::~AmanServer() {
    stop();
    WSACleanup();  // Clean up Winsock
}

void AmanServer::startServer() {
    // Initialize Winsock
    WSADATA wsaData;
    int result = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (result != 0) {
        DebugOut("WSAStartup failed: " + std::to_string(result));
        return;
    }

    // Start the server thread
    isRunning = true;
    serverThread = std::thread(&AmanServer::serverLoop, this);
    senderThread = std::thread(&AmanServer::senderThreadLoop, this);
}

void AmanServer::stop() {
    if (isRunning) {
        isRunning = false;
        clientConnected = false;
        
        // Close the listen socket first to unblock accept()
        if (listenSocket != INVALID_SOCKET) {
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
        }
        
        // Close the client socket
        if (clientSocket != INVALID_SOCKET) {
            closesocket(clientSocket);
            clientSocket = INVALID_SOCKET;
        }
        
        // Notify condition variable to unblock any waiting threads
        queueCondition.notify_all();
        
        // Wait for threads to finish
        if (serverThread.joinable()) {
            serverThread.join();
        }
        if (senderThread.joinable()) {
            senderThread.join();
        }
    }
}

void AmanServer::serverLoop() {
    while (isRunning) {
        listenSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (listenSocket == INVALID_SOCKET) {
            DebugOut("Error creating socket");
            return;
        }

        sockaddr_in serverAddr{};
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_addr.s_addr = INADDR_ANY;
        serverAddr.sin_port = htons(12345);
        
        if (bind(listenSocket, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
            DebugOut("Bind failed with error: " + std::to_string(WSAGetLastError()));
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
            return;
        }

        if (listen(listenSocket, 1) == SOCKET_ERROR) {
            DebugOut("Listen failed with error: " + std::to_string(WSAGetLastError()));
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
            return;        }

        DebugOut("Waiting for a client to connect...");

        clientSocket = accept(listenSocket, nullptr, nullptr);

        // accept() will return SOCKET_ERROR if listenSocket is closed
        if (clientSocket == INVALID_SOCKET) {
            if (isRunning) { // real error
                DebugOut("Accept failed with error: " + std::to_string(WSAGetLastError()));
            }
            if (listenSocket != INVALID_SOCKET) {
                closesocket(listenSocket);
                listenSocket = INVALID_SOCKET;
            }
            continue;
        }        // Client connected - close listen socket and handle communication
        if (listenSocket != INVALID_SOCKET) {
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
        }
        
        // Set client socket to non-blocking mode to prevent blocking on large sends
        u_long nonBlocking = 1;
        if (ioctlsocket(clientSocket, FIONBIO, &nonBlocking) == SOCKET_ERROR) {
            DebugOut("Failed to set client socket to non-blocking mode: " + std::to_string(WSAGetLastError()));
        } else {
            DebugOut("Client socket set to non-blocking mode");
        }

        clientConnected = true;
        DebugOut("Client connected successfully");
          // Notify sender thread that client is connected
        queueCondition.notify_all();
        
        // Notify derived class that client connected (for version handshake)
        onClientConnected();
        
        handleClientConnection();
        
        // Client disconnected
        DebugOut("Cleaning up client connection...");
        clientConnected = false;
        
        // Notify sender thread that client disconnected
        queueCondition.notify_all();
        if (clientSocket != INVALID_SOCKET) {
            closesocket(clientSocket);
            clientSocket = INVALID_SOCKET;
        }
    }
}

void AmanServer::handleClientConnection() {
    char buffer[4096];
    std::string receivedData;
    
    DebugOut("Starting client communication loop...");
    
    while (isRunning && clientConnected && clientSocket != INVALID_SOCKET) {
        int bytesReceived = recv(clientSocket, buffer, sizeof(buffer) - 1, 0);
        if (bytesReceived > 0) {
            buffer[bytesReceived] = '\0';
            receivedData += buffer;
            DebugOut("Received " + std::to_string(bytesReceived) + " bytes from client");
            
            // Process complete messages (assuming newline-delimited)
            size_t pos = 0;
            while ((pos = receivedData.find('\n')) != std::string::npos) {
                std::string message = receivedData.substr(0, pos);
                receivedData = receivedData.substr(pos + 1);
                
                if (!message.empty()) {
                    DebugOut("Processing message: " + message);
                    try {
                        processMessage(message);
                    } catch (const std::exception& e) {
                        DebugOut("Error processing message: " + std::string(e.what()));
                    }
                }
            }
        } else if (bytesReceived == 0) {
            DebugOut("Client disconnected gracefully (recv returned 0)");
            onClientDisconnected();
            break;
        } else {
            int error = WSAGetLastError();
            
            // WSAEWOULDBLOCK is expected for non-blocking sockets when no data is available
            if (error == WSAEWOULDBLOCK) {
                // No data available right now, sleep briefly and continue
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
                continue;
            }
            
            if (isRunning && clientConnected) {
                DebugOut("Recv failed with error: " + std::to_string(error) + " (WSAECONNRESET=" + std::to_string(WSAECONNRESET) + ")");
            }
            
            // Check if it's a connection reset or client disconnect
            if (error == WSAECONNRESET || error == WSAECONNABORTED) {
                DebugOut("Client connection was reset/aborted");
                onClientDisconnected();
            }
            break;
        }    }
    
    DebugOut("Client communication loop ended");
}

bool AmanServer::sendMessageSafely(const std::string& message) {
    if (clientSocket == INVALID_SOCKET || !clientConnected) {
        return false;
    }
    
    const char* data = message.c_str();
    int totalBytes = (int)message.length();
    int bytesSent = 0;
    int attempts = 0;
    const int maxAttempts = 100; // Prevent infinite loops
    
    DebugOut("Starting to send message safely: " + std::to_string(totalBytes) + " bytes");
    
    while (bytesSent < totalBytes && clientConnected && attempts < maxAttempts) {
        int result = send(clientSocket, data + bytesSent, totalBytes - bytesSent, 0);
        
        if (result > 0) {
            bytesSent += result;
            DebugOut("Sent " + std::to_string(result) + " bytes, total: " + std::to_string(bytesSent) + "/" + std::to_string(totalBytes));
        } else if (result == SOCKET_ERROR) {
            int error = WSAGetLastError();
            if (error == WSAEWOULDBLOCK) {
                // Socket buffer is full, wait a bit and retry
                attempts++;
                DebugOut("Socket would block, attempt " + std::to_string(attempts) + ", waiting...");
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
                continue;
            } else {
                DebugOut("Send failed with error: " + std::to_string(error));
                return false;
            }
        } else {
            DebugOut("Send returned 0, connection closed");
            return false;
        }
    }
    
    if (bytesSent == totalBytes) {
        DebugOut("Message sent successfully: " + std::to_string(bytesSent) + " bytes");
        return true;
    } else {
        DebugOut("Failed to send complete message: " + std::to_string(bytesSent) + "/" + std::to_string(totalBytes) + " bytes");
        return false;
    }
}

void AmanServer::senderThreadLoop() {
    DebugOut("Sender thread started");
    
    while (isRunning) {
        std::unique_lock<std::mutex> lock(queueMutex);
        DebugOut("Sender thread waiting for client connection or messages...");
        
        // Wait for either a client to connect AND have messages, or for shutdown
        queueCondition.wait(lock, [this] { 
            return (!messageQueue.empty() && clientConnected) || !isRunning;        });
        
        if (!isRunning) {
            DebugOut("Sender thread stopping - isRunning false");
            break;
        }
        
        if (!clientConnected) {
            DebugOut("Sender thread: No client connected, continuing to wait...");
            continue;
        }
        
        DebugOut("Sender thread woke up, queue size: " + std::to_string(messageQueue.size()));
        
        // Process all queued messages        while (!messageQueue.empty() && clientConnected && clientSocket != INVALID_SOCKET && isRunning) {
            std::string message = messageQueue.front();
            messageQueue.pop();
            lock.unlock();
            
            DebugOut("Sending message to client (length: " + std::to_string(message.length()) + "): " + message.substr(0, 50) + "...");
            
            // Send message to client using safe method for large messages
            message += "\n"; // Add newline delimiter
            bool success = sendMessageSafely(message);
            
            if (!success) {
                DebugOut("CRITICAL: Failed to send message, marking client as disconnected");
                clientConnected = false;
                // Don't break here - let the loop condition handle it            }
            
            lock.lock();
        }
        
        if (!clientConnected) {
            DebugOut("Sender thread: Client disconnected, clearing message queue");
            // Clear remaining messages since client is gone
            while (!messageQueue.empty()) {
                messageQueue.pop();
            }
        }
    }
    
    DebugOut("Sender thread exiting");
}


void AmanServer::enqueueMessage(const std::string& data) {
    static int messageCount = 0;
    messageCount++;
    
    // Only log every 10th message to reduce spam
    bool shouldLog = (messageCount % 10 == 1);
    
    if (shouldLog) {
        DebugOut("enqueueMessage called (#" + std::to_string(messageCount) + "), isRunning: " + 
                 std::to_string(isRunning) + ", clientConnected: " + std::to_string(clientConnected));
    }
    
    if (!isRunning || !clientConnected) {
        if (shouldLog) DebugOut("Message not queued - server not running or client not connected");
        return; // Don't queue messages if not running or no client
    }
    
    try {
        std::lock_guard<std::mutex> lock(queueMutex);
        messageQueue.push(data);
        if (shouldLog) {
            DebugOut("Message queued successfully, queue size: " + std::to_string(messageQueue.size()));
        }
        queueCondition.notify_one();
    } catch (const std::exception& e) {
        DebugOut("Error enqueueing message: " + std::string(e.what()));
    }
}

