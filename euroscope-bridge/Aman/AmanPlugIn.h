#pragma once

#include <string>
#include <vector>
#include <memory>
#include <set>
#include "EuroScopePlugIn.h"
#include "AmanServer.h"
#include "JsonMessageHelper.h"

using namespace EuroScopePlugIn;

class AmanAircraft;
class ExternalMessageHandler;

struct AmanTimeline {
    long identifier;
    std::vector<std::string> viaFixes;
    std::vector<std::string> destinationFixes;
    std::vector<std::string> destinationAirports;
};

class AmanPlugIn : public CPlugIn, public AmanServer {
public:
    AmanPlugIn();
    std::vector<AmanAircraft> getAircraftForTimeline(std::shared_ptr<AmanTimeline> timeline);
    virtual ~AmanPlugIn();

private:
    JsonMessageHelper jsonSerializer;

    std::vector<std::shared_ptr<AmanTimeline>> timelines;
    std::string pluginDirectory;

    virtual void OnTimer(int Counter);

    bool hasCorrectDestination(CFlightPlanData fpd, std::vector<std::string> destinationAirports);
    int getFixIndexByName(CFlightPlanExtractedRoute extractedRoute, const std::string& fixName);
    int getFirstViaFixIndex(CFlightPlanExtractedRoute extractedRoute, std::vector<std::string> viaFixes);
    double findRemainingDist(CRadarTarget radarTarget, CFlightPlanExtractedRoute extractedRoute, int fixIndex);

    std::vector<AmanAircraft> getInboundsForFix(const std::string& fixName, std::vector<std::string> viaFixes, std::vector<std::string> destinationAirports);
    std::vector<DmanAircraft> getOutboundsFromAirport(const std::string& airport);

    long processDepartureTime(const std::string& departureTime);
    
    static std::vector<std::string> splitString(const std::string& string, const char delim);

    // Server methods
    void onRegisterTimeline(long timelineId, const std::vector<std::string>& viaFixes, const std::vector<std::string>& destinationFixes, const std::vector<std::string>& destinationAirports) override;
    void onUnregisterTimeline(long timelineId) override;
    void onSetCtot(const std::string& callSign, long ctot) override;
};
