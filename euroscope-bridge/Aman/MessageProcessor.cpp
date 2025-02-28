#include "stdafx.h"
#include "MessageProcessor.h"

#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

void MessageProcessor::processMessage(const std::string& message) {
    
    // Parse the incoming JSON message
    rapidjson::Document document;
    document.Parse(message.c_str());

    auto type = std::string(document["type"].GetString());

    if (type == "registerTimeline") {
        long timelineId = document["timelineId"].GetInt64();
        std::vector<std::string> viaFixes;
        std::vector<std::string> targetFixes;
        for (const auto& viaFix : document["viaFixes"].GetArray()) {
            viaFixes.push_back(viaFix.GetString());
        }
        for (const auto& targetFix : document["targetFixes"].GetArray()) {
            targetFixes.push_back(targetFix.GetString());
        }
        std::vector<std::string> destinationAirports;
        for (const auto& destinationAirport : document["destinationAirports"].GetArray()) {
            destinationAirports.push_back(destinationAirport.GetString());
        }

        onRegisterTimeline(timelineId, viaFixes, targetFixes, destinationAirports);
    } else if (type == "unregisterTimeline") {
        long timelineId = document["timelineId"].GetInt64();

        onUnregisterTimeline(timelineId);
    }
}
