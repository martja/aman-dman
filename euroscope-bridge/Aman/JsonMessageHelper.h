#pragma once

#include <string>
#include <vector>

#include "AmanAircraft.h"
#include "DmanAircraft.h"

class JsonMessageHelper {
public:
    const std::string getJsonOfAircraft(long timelineId, const std::vector<AmanAircraft>& aircraftList);
    const std::string getJsonOfDepartingAircraft(long timelineId, const std::vector<DmanAircraft>& aircraftList);
};

