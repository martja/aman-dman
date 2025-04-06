#pragma once

#include <string>
#include <vector>

struct VerticalProfileSection {
    int maxAltitude;
    int minAltitude;
    int secDuration;
    int averageHeading;
    float distance;
};

struct RouteFix {
    std::string name;
    bool isOnStar;
    double latitude;
    double longitude;
};

class AmanAircraft {
public:
    std::string callsign;
    std::string finalFix;
    std::string arrivalRunway;
    std::string assignedStar;
    std::string icaoType;
    std::string assignedDirectRouting;
    std::string scratchPad;
    std::vector<RouteFix> remainingRoute;
    std::string trackingController;
    std::string arrivalAirportIcao;

    bool isSelected;
    char wtc;
    uint32_t secondsBehindPreceeding;
    float latitude;
    float longitude;

    int groundSpeed;
    int pressureAltitude;
    int flightLevel;
};