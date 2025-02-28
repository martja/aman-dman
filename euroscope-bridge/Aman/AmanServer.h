#pragma once

#include <queue>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <string>
#include <functional>
#include <winsock2.h>

#include "MessageProcessor.h"

class AmanServer : public MessageProcessor {
public:
    AmanServer();
    ~AmanServer();

protected:
    void startServer();
    void stop();
    void sendData(const std::string& data);
    void sendDataToClient(const std::string& message);

private:
    void serverLoop();

    std::thread serverThread;
    bool isRunning;
    SOCKET clientSocket;

    std::queue<std::string> messageQueue;
    std::mutex queueMutex;
    std::condition_variable queueCondition;

};

