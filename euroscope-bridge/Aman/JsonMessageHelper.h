#pragma once

#include <string>
#include <vector>

#include "AmanAircraft.h"

class JsonMessageHelper {
public:
    const std::string getJsonOfAircraft(long timelineId, const std::vector<AmanAircraft>& aircraftList);
};

