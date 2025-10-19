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
    if (strcmp(messageType, "registerAirport") == 0) {
        auto airportIcao = document["icao"].GetString();
        onRegisterAirport(airportIcao);
    }
    else if (strcmp(messageType, "unregisterAirport") == 0) {
        auto airportIcao = document["icao"].GetString();
        onUnregisterAirport(airportIcao);
    }
    else if (strcmp(messageType, "assignRunway") == 0) {
        onRequestAssignRunway(document["callsign"].GetString(), document["runway"].GetString());
    }
    else {
        onErrorProcessingMessage("Unknown message type: " + std::string(messageType));
    }

}