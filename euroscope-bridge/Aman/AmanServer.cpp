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

#pragma comment(lib, "ws2_32.lib")  // Link with the Winsock library

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
        std::cerr << "WSAStartup failed: " << result << std::endl;
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
            std::cerr << "Error creating socket" << std::endl;
            return;
        }

        sockaddr_in serverAddr{};
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_addr.s_addr = INADDR_ANY;
        serverAddr.sin_port = htons(12345);

        if (bind(listenSocket, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
            std::cerr << "Bind failed" << std::endl;
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
            return;
        }

        if (listen(listenSocket, 1) == SOCKET_ERROR) {
            std::cerr << "Listen failed" << std::endl;
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
            return;
        }

        std::cout << "Waiting for a client to connect..." << std::endl;

        clientSocket = accept(listenSocket, nullptr, nullptr);

        // accept() will return SOCKET_ERROR if listenSocket is closed
        if (clientSocket == INVALID_SOCKET) {
            if (isRunning) { // real error
                std::cerr << "Accept failed" << std::endl;
            }
            if (listenSocket != INVALID_SOCKET) {
                closesocket(listenSocket);
                listenSocket = INVALID_SOCKET;
            }
            continue;
        }

        // Client connected - close listen socket and handle communication
        if (listenSocket != INVALID_SOCKET) {
            closesocket(listenSocket);
            listenSocket = INVALID_SOCKET;
        }

        clientConnected = true;
        std::cout << "Client connected" << std::endl;
        
        handleClientConnection();
        
        // Client disconnected
        clientConnected = false;
        if (clientSocket != INVALID_SOCKET) {
            closesocket(clientSocket);
            clientSocket = INVALID_SOCKET;
        }
    }
}

void AmanServer::handleClientConnection() {
    char buffer[4096];
    std::string receivedData;
    
    while (isRunning && clientConnected && clientSocket != INVALID_SOCKET) {
        int bytesReceived = recv(clientSocket, buffer, sizeof(buffer) - 1, 0);
        if (bytesReceived > 0) {
            buffer[bytesReceived] = '\0';
            receivedData += buffer;
            
            // Process complete messages (assuming newline-delimited)
            size_t pos = 0;
            while ((pos = receivedData.find('\n')) != std::string::npos) {
                std::string message = receivedData.substr(0, pos);
                receivedData = receivedData.substr(pos + 1);
                
                if (!message.empty()) {
                    try {
                        processMessage(message);
                    } catch (const std::exception& e) {
                        std::cerr << "Error processing message: " << e.what() << std::endl;
                    }
                }
            }
        } else if (bytesReceived == 0) {
            std::cout << "Client disconnected gracefully" << std::endl;
            onClientDisconnected();
            break;
        } else {
            if (isRunning && clientConnected) {
                std::cerr << "Recv failed with error: " << WSAGetLastError() << std::endl;
            }
            break;
        }
    }
}

void AmanServer::senderThreadLoop() {
    std::cout << "Sender thread started" << std::endl;
    
    while (isRunning) {
        std::unique_lock<std::mutex> lock(queueMutex);
        std::cout << "Sender thread waiting for messages..." << std::endl;
        queueCondition.wait(lock, [this] { return !messageQueue.empty() || !isRunning; });
        
        if (!isRunning) {
            std::cout << "Sender thread stopping - isRunning false" << std::endl;
            break;
        }
        
        std::cout << "Sender thread woke up, queue size: " << messageQueue.size() << std::endl;
        
        while (!messageQueue.empty() && clientConnected && clientSocket != INVALID_SOCKET) {
            std::string message = messageQueue.front();
            messageQueue.pop();
            lock.unlock();
            
            std::cout << "Sending message to client: " << message.substr(0, 50) << "..." << std::endl;
            
            // Send message to client
            message += "\n"; // Add newline delimiter
            int result = send(clientSocket, message.c_str(), (int)message.length(), 0);
            if (result == SOCKET_ERROR) {
                int error = WSAGetLastError();
                std::cerr << "Send failed with error: " << error << std::endl;
                clientConnected = false;
                break;
            } else {
                std::cout << "Message sent successfully, bytes: " << result << std::endl;
            }
            
            lock.lock();
        }
        
        if (!clientConnected) {
            std::cout << "Sender thread stopping - client disconnected" << std::endl;
            break;
        }
    }
    
    std::cout << "Sender thread exiting" << std::endl;
}


void AmanServer::enqueueMessage(const std::string& data) {
    std::cout << "enqueueMessage called, isRunning: " << isRunning << ", clientConnected: " << clientConnected << std::endl;
    
    if (!isRunning || !clientConnected) {
        std::cout << "Message not queued - server not running or client not connected" << std::endl;
        return; // Don't queue messages if not running or no client
    }
    
    try {
        std::lock_guard<std::mutex> lock(queueMutex);
        messageQueue.push(data);
        std::cout << "Message queued successfully, queue size: " << messageQueue.size() << std::endl;
        queueCondition.notify_one();
    } catch (const std::exception& e) {
        std::cerr << "Error enqueueing message: " << e.what() << std::endl;
    }
}

