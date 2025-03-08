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

    if (type == "requestInboundsForFix") {
        long requestId = document["requestId"].GetInt64();
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

        onRequestInboundsForFix(requestId, viaFixes, targetFixes, destinationAirports);
    } else if (type == "requestOutbounds") {
        long requestId = document["requestId"].GetInt64();
        std::string icao = document["airportIcao"].GetString();

        onRequestOutboundsFromAirport(requestId, icao);
    } else if (type == "unregisterTimeline") {
        long requestId = document["requestId"].GetInt64();

        onUnsubscribe(requestId);
    } else if (type == "setCtot") {
        std::string callsign = document["callsign"].GetString();
        long ctot = document["ctot"].GetInt64();
        onSetCtot(callsign, ctot);
    }
}
