#pragma once

#include <string>
#include <vector>

#include "AmanAircraft.h"
#include "DmanAircraft.h"

class JsonMessageHelper {
public:
    const std::string getJsonOfArrivals(long requestId, const std::vector<AmanAircraft>& aircraftList);
    const std::string getJsonOfDepartures(long requestId, const std::vector<DmanAircraft>& aircraftList);
};

