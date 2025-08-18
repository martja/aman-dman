#pragma once

#include <queue>
#include <thread>
#include <mutex>
#include <atomic>
#include <condition_variable>
#include <string>
#include <functional>
#include <winsock2.h>

#include "ServerEventsHandler.h"

class AmanServer : public ServerEventsHandler {
public:
    AmanServer();
    ~AmanServer();

protected:
    void startServer();
    void stop();
    void enqueueMessage(const std::string& data);

private:
    void serverLoop();
    void handleClientConnection();
    void senderThreadLoop();
    bool sendMessageSafely(const std::string& message);

    std::thread serverThread;
    std::thread senderThread;
    std::atomic<bool> isRunning;
    std::atomic<bool> clientConnected;
    SOCKET listenSocket;
    SOCKET clientSocket;

    std::queue<std::string> messageQueue;
    std::mutex queueMutex;
    std::condition_variable queueCondition;

};

