#include "stdafx.h"

#include <string>
#include <vector>

struct RegisterTimeline {
    long requestId;
    std::vector<std::string> viaFixes;
    std::vector<std::string> targetFixes;
};

struct UnregisterTimeline {
    long requestId;
};