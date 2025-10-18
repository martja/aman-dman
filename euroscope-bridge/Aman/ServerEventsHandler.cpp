#include "stdafx.h"
#include "ServerEventsHandler.h"

#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"

#include <string>

void ServerEventsHandler::processMessage(const std::string& message) {
    // Parse the JSON using rapidjson
    rapidjson::Document document;
    document.Parse(message.c_str());


    if (document.HasParseError()) {
        onErrorProcessingMessage("Error parsing JSON message: " + message);
        return;
    }

    const char* messageType = document["type"].GetString();

    // requestInboundsForFix
    if (strcmp(messageType, "requestInboundsForFix") == 0) {
        long requestId = document["requestId"].GetInt64();
        std::vector<std::string> viaFixes;
        for (const auto& fix : document["viaFixes"].GetArray()) {
            viaFixes.push_back(fix.GetString());
        }
        std::vector<std::string> destinationFixes;
        for (const auto& fix : document["targetFixes"].GetArray()) {
            destinationFixes.push_back(fix.GetString());
        }
        std::vector<std::string> destinationAirports;
        for (const auto& airport : document["destinationAirports"].GetArray()) {
            destinationAirports.push_back(airport.GetString());
        }
        onRequestInboundsForFix(requestId, viaFixes, destinationFixes, destinationAirports);
    }
    else if (strcmp(messageType, "cancelRequest") == 0) {
        long requestId = document["requestId"].GetInt64();
        onCancelRequest(requestId);
    }
    else if (strcmp(messageType, "assignRunway") == 0) {
        long requestId = document["requestId"].GetInt64();
        onRequestAssignRunway(requestId, document["callsign"].GetString(), document["runway"].GetString());
    }
    else {
        onErrorProcessingMessage("Unknown message type: " + std::string(messageType));
    }

}