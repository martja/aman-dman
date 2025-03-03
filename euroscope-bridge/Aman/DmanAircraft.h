#pragma once

#include <string>

class DmanAircraft {
public:
    std::string callsign;
    std::string sid;
    std::string runway;
    std::string icaoType;
    char wakeCategory;

    long estimatedDepartureTime;
};