#pragma once

#include <queue>
#include <thread>
#include <mutex>
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

    std::thread serverThread;
    bool isRunning;
    SOCKET clientSocket;

    std::queue<std::string> messageQueue;
    std::mutex queueMutex;
    std::condition_variable queueCondition;

};

