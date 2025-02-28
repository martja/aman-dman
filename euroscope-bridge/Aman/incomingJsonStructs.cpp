#include "stdafx.h"

#include <string>
#include <vector>

struct RegisterTimeline {
    long timelineId;
    std::vector<std::string> viaFixes;
    std::vector<std::string> targetFixes;
};

struct UnregisterTimeline {
    long timelineId;
};