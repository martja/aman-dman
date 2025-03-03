#pragma once

#include <string>
#include <vector>

class MessageProcessor {
public:
    void processMessage(const std::string& message);

protected:
    virtual void onRegisterTimeline(long timelineId, const std::vector<std::string>& viaFixes, const std::vector<std::string>& destinationFixes, const std::vector<std::string>& destinationAirports) = 0;
    virtual void onUnregisterTimeline(long timelineId) = 0;
    virtual void onSetCtot(const std::string& callSign, long ctot) = 0;

};

