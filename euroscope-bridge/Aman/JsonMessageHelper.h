#pragma once

#include <string>
#include <vector>

#include "AmanDataTypes.h"

class JsonMessageHelper {
public:
    const std::string getJsonOfArrivals(long requestId, const std::vector<AmanAircraft>& aircraftList);
    const std::string getJsonOfDepartures(long requestId, const std::vector<DmanAircraft>& aircraftList);
    const std::string getJsonOfRunwayStatuses(long requestId, const std::vector<RunwayStatus>& runways);
};

