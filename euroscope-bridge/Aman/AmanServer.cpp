#include "stdafx.h"
#include "AmanServer.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <winsock2.h>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <string>

#pragma comment(lib, "ws2_32.lib")  // Link with the Winsock library

AmanServer::AmanServer() : isRunning(false), clientSocket(INVALID_SOCKET) {
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
}

void AmanServer::stop() {
    if (isRunning) {
        isRunning = false;
        queueCondition.notify_all();
        serverThread.join();

        // Close the client socket when stopping the server
        if (clientSocket != INVALID_SOCKET) {
            closesocket(clientSocket);
        }
    }
}

void AmanServer::serverLoop() {
    SOCKET listenSocket = INVALID_SOCKET;

    while (isRunning) {
        // Set up the listening socket
        listenSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (listenSocket == INVALID_SOCKET) {
            std::cerr << "Error creating socket" << std::endl;
            return;
        }

        sockaddr_in serverAddr;
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_addr.s_addr = INADDR_ANY;
        serverAddr.sin_port = htons(12345);  // Change to your desired port

        // Bind the socket
        if (bind(listenSocket, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
            std::cerr << "Bind failed" << std::endl;
            closesocket(listenSocket);
            return;
        }

        // Listen for incoming connections
        if (listen(listenSocket, 1) == SOCKET_ERROR) {
            std::cerr << "Listen failed" << std::endl;
            closesocket(listenSocket);
            return;
        }

        std::cout << "Waiting for a client to connect..." << std::endl;

        // Accept a connection from a client
        clientSocket = accept(listenSocket, nullptr, nullptr);
        closesocket(listenSocket);  // Close the listening socket to handle one client at a time

        if (clientSocket == INVALID_SOCKET) {
            std::cerr << "Accept failed, retrying..." << std::endl;
            continue;  // Retry accepting a new connection
        }

        std::cout << "Client connected!" << std::endl;

        // Set client socket to non-blocking mode
        u_long mode = 1;
        ioctlsocket(clientSocket, FIONBIO, &mode);

        // Main loop to process messages
        while (isRunning) {
            std::this_thread::sleep_for(std::chrono::milliseconds(50));  // Reduce sleep to increase responsiveness

            // **Check if messages exist without blocking**
            std::unique_lock<std::mutex> lock(queueMutex);
            if (!messageQueue.empty()) {
                std::string message = messageQueue.front() + "\n";
                messageQueue.pop();
                lock.unlock();  // Unlock before sending to avoid blocking other operations

                // Send the message over the socket to the connected client
                if (send(clientSocket, message.c_str(), static_cast<int>(message.length()), 0) == SOCKET_ERROR) {
                    std::cerr << "Send failed, client might have disconnected" << std::endl;
                    break;
                }
            } else {
                lock.unlock();
            }

            // **Process incoming data from the client**
            char buffer[1024];
            int bytesRead = recv(clientSocket, buffer, sizeof(buffer), 0);
            if (bytesRead > 0) {
                std::string incomingData(buffer, bytesRead);

                // Split the incoming data by newlines
                size_t startServer = 0;
                size_t end = incomingData.find("\n");
                while (end != std::string::npos) {
                    std::string message = incomingData.substr(startServer, end - startServer);
                    processMessage(message); 
                    startServer = end + 1;
                    end = incomingData.find("\n", startServer);
                }
            }

            // **Check if the client is still connected (non-blocking)**
            char peekBuffer[1];
            int recvResult = recv(clientSocket, peekBuffer, sizeof(peekBuffer), MSG_PEEK);
            if (recvResult == 0 || (recvResult == SOCKET_ERROR && WSAGetLastError() != WSAEWOULDBLOCK)) {
                std::cerr << "Client disconnected" << std::endl;
                onClientDisconnected();
                break;
            }
        }

        // Cleanup client socket before waiting for a new connection
        closesocket(clientSocket);
        clientSocket = INVALID_SOCKET;
    }
}

void AmanServer::enqueueMessage(const std::string& data) {
    std::lock_guard<std::mutex> lock(queueMutex);
    messageQueue.push(data);
    queueCondition.notify_one();
}

