#pragma once
#include <string>
#include <vector>

class ServerEventsHandler {
public:
    void processMessage(const std::string& message);
protected:
    virtual void onRequestInboundsForFix(long requestId, const std::vector<std::string>& viaFixes, const std::vector<std::string>& destinationFixes, const std::vector<std::string>& destinationAirports) = 0;
    virtual void onRequestOutboundsFromAirport(long requestId, const std::string& icao) = 0;
    virtual void onUnsubscribe(long requestId) = 0;
    virtual void onSetCtot(const std::string& callSign, long ctot) = 0;
    virtual void onClientDisconnected() = 0;
    virtual void onErrorProcessingMessage(const std::string& errorMessage) = 0;
};

