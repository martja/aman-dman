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

struct InboundsToFixSubscription {
    int requestId;
    std::vector<std::string> viaFixes;
    std::vector<std::string> destinationFixes;
    std::vector<std::string> destinationAirports;
};

struct OutboundsSubscription {
    int requestId;
    std::string airport;
    std::vector<std::string> runways;
};

class AmanPlugIn : public CPlugIn, public AmanServer {
public:
    AmanPlugIn();
    std::vector<AmanAircraft> collectResponseToInboundsSubscription(std::shared_ptr<InboundsToFixSubscription> subscription);
    std::vector<DmanAircraft> collectResponseToOutboundsSubscription(std::shared_ptr<OutboundsSubscription> subscription);

    virtual ~AmanPlugIn();

private:
    JsonMessageHelper jsonSerializer;

    std::vector<std::shared_ptr<InboundsToFixSubscription>> inboundsSubscriptions;
    std::vector<std::shared_ptr<OutboundsSubscription>> outboundsSubscriptions;
    std::string pluginDirectory;

    virtual void OnTimer(int Counter);

    bool hasCorrectDestination(CFlightPlanData fpd, std::vector<std::string> destinationAirports);
    int getFixIndexByName(CFlightPlanExtractedRoute extractedRoute, const std::string& fixName);
    int getFirstViaFixIndex(CFlightPlanExtractedRoute extractedRoute, std::vector<std::string> viaFixes);
    std::vector<RouteFix> findExtractedRoutePoints(CRadarTarget radarTarget);

    std::vector<AmanAircraft> getInboundsForAirport(const std::string& fixName);
    std::vector<DmanAircraft> getOutboundsFromAirport(const std::string& airport);

    long processDepartureTime(const std::string& departureTime);
    
    static std::vector<std::string> splitString(const std::string& string, const char delim);

    // Server methods
    void onRequestInboundsForFix(long requestId, const std::vector<std::string>& viaFixes, const std::vector<std::string>& destinationFixes, const std::vector<std::string>& destinationAirports) override;
    void onRequestOutboundsFromAirport(long requestId, const std::string& icao) override;
    void onUnsubscribe(long requestId) override;
    void onSetCtot(const std::string& callSign, long ctot) override;
    void onClientDisconnected() override;
    void onErrorProcessingMessage(const std::string& errorMessage) override;
};
