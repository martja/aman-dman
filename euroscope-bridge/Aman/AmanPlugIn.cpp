#include "stdafx.h"

#include "AmanDataTypes.h"
#include "AmanPlugIn.h"
#include "windows.h"

#include <algorithm>
#include <ctime>
#include <iterator>
#include <regex>
#include <sstream>
#include <string>
#include <vector>
#include <fstream>
#include <map>
#include <iostream>

#define TO_UPPERCASE(str) std::transform(str.begin(), str.end(), str.begin(), ::toupper);
#define REMOVE_EMPTY(strVec, output)                                                                                   \
    std::copy_if(strVec.begin(), strVec.end(), std::back_inserter(output), [](std::string i) { return !i.empty(); });
#define REMOVE_LAST_CHAR(str)                                                                                          \
    if (str.length() > 0)                                                                                              \
        str.pop_back();
#define DISPLAY_WARNING(str) DisplayUserMessage("Aman", "Warning", str, true, true, true, true, false);

// Plugin metadata
#define MY_PLUGIN_NAME          "AMAN-ES-Bridge"
#define MY_PLUGIN_VERSION       PLUGIN_VERSION
#define MY_PLUGIN_DEVELOPER     CONTRIBUTORS
#define MY_PLUGIN_COPYRIGHT     "GPL v3"

AmanPlugIn::AmanPlugIn() 
    : CPlugIn(COMPATIBILITY_CODE, MY_PLUGIN_NAME, MY_PLUGIN_VERSION, MY_PLUGIN_DEVELOPER, MY_PLUGIN_COPYRIGHT)
    , AmanServer()
    , jsonSerializer()
{
    // Find directory of this .dll
    char fullPluginPath[_MAX_PATH];
    GetModuleFileNameA((HINSTANCE)&__ImageBase, fullPluginPath, sizeof(fullPluginPath));
    std::string fullPluginPathStr(fullPluginPath);
    pluginDirectory = fullPluginPathStr.substr(0, fullPluginPathStr.find_last_of("\\"));
}

AmanPlugIn::~AmanPlugIn() { 
    
}

void AmanPlugIn::OnTimer(int Counter) {
    std::cout << "OnTimer called, Counter: " << Counter << std::endl;
    
    for each(auto & timeline in inboundsSubscriptions) {
        auto inbounds = collectResponseToInboundsSubscription(timeline);
        auto inboundsJson = jsonSerializer.getJsonOfArrivals(timeline->requestId, inbounds);
        std::cout << "Enqueueing inbounds message: " << inboundsJson.substr(0, 100) << "..." << std::endl;
        enqueueMessage(inboundsJson);
    }

    for each(auto & timeline in outboundsSubscriptions) {
        auto outbounds = collectResponseToOutboundsSubscription(timeline);
        auto outboundsJson = jsonSerializer.getJsonOfDepartures(timeline->requestId, outbounds);
        std::cout << "Enqueueing outbounds message: " << outboundsJson.substr(0, 100) << "..." << std::endl;
        enqueueMessage(outboundsJson);
    }
}

void AmanPlugIn::OnAirportRunwayActivityChanged(void) {
    for each(auto & timeline in inboundsSubscriptions) {
        sendUpdatedRunwayStatuses(timeline->requestId);
    }
}

bool AmanPlugIn::hasCorrectDestination(CFlightPlanData fpd, std::vector<std::string> destinationAirports) {
    return destinationAirports.size() == 0 ? 
        true : std::find(destinationAirports.begin(), destinationAirports.end(), fpd.GetDestination()) != destinationAirports.end();
}

int AmanPlugIn::getFixIndexByName(CFlightPlanExtractedRoute extractedRoute, const std::string& fixName) {
    for (int i = 0; i < extractedRoute.GetPointsNumber(); i++) {
        if (!strcmp(extractedRoute.GetPointName(i), fixName.c_str())) {
            return i;
        }
    }
    return -1;
}

int AmanPlugIn::getFirstViaFixIndex(CFlightPlanExtractedRoute extractedRoute, std::vector<std::string> viaFixes) {
    for (int i = 0; i < viaFixes.size(); i++) {
        if (getFixIndexByName(extractedRoute, viaFixes[i]) != -1) {
            return i;
        }
    }
    return -1;
}

std::vector<RouteFix> AmanPlugIn::findExtractedRoutePoints(CRadarTarget radarTarget) {
    auto extractedRoute = radarTarget.GetCorrelatedFlightPlan().GetExtractedRoute();
    int closestFixIndex = extractedRoute.GetPointsCalculatedIndex();
    int assignedDirectFixIndex = extractedRoute.GetPointsAssignedIndex();
    int routeLength = extractedRoute.GetPointsNumber();

    int nextFixIndex = assignedDirectFixIndex > -1 ? assignedDirectFixIndex : closestFixIndex;

    std::vector<RouteFix> route;

    // Ignore waypoints prior to nextFixIndex
    for (int i = 0; i < routeLength; i++) {
        RouteFix fix;
        auto airwayName = extractedRoute.GetPointAirwayName(i);
        fix.name = extractedRoute.GetPointName(i);
        fix.latitude = extractedRoute.GetPointPosition(i).m_Latitude;
        fix.longitude = extractedRoute.GetPointPosition(i).m_Longitude;
        fix.isPassed = i < nextFixIndex;
        route.push_back(fix);
    }
    return route;
}

std::vector<AmanAircraft> AmanPlugIn::collectResponseToInboundsSubscription(std::shared_ptr<InboundsToFixSubscription> timeline) {
    auto pAircraftList = std::vector<AmanAircraft>();

    for each (auto airportIcao in timeline->destinationAirports) {
        auto inbounds = getInboundsForAirport(airportIcao);
        pAircraftList.insert(pAircraftList.end(), inbounds.begin(), inbounds.end());
    }

    return pAircraftList;
}

std::vector<DmanAircraft> AmanPlugIn::collectResponseToOutboundsSubscription(std::shared_ptr<OutboundsSubscription> subscription) {
    return getOutboundsFromAirport(subscription->airport);
}

std::vector<std::string> AmanPlugIn::splitString(const std::string& string, const char delim) {
    std::vector<std::string> output;
    size_t startServer;
    size_t end = 0;
    while ((startServer = string.find_first_not_of(delim, end)) != std::string::npos) {
        end = string.find(delim, startServer);
        output.push_back(string.substr(startServer, end - startServer));
    }
    return output;
}

void AmanPlugIn::sendUpdatedRunwayStatuses(long requestId) {
    for each(auto& timeline in inboundsSubscriptions) {
        if (timeline->requestId != requestId)
            continue;

        auto runwayStatuses = collectRunwayStatuses(timeline->destinationAirports.size() > 0 ? timeline->destinationAirports[0] : "");
        auto runwaysJson = jsonSerializer.getJsonOfRunwayStatuses(timeline->requestId, runwayStatuses);
        enqueueMessage(runwaysJson);
    }
}

void AmanPlugIn::onRequestInboundsForFix(long requestId, const std::vector<std::string>& viaFixes, const std::vector<std::string>& destinationFixes, const std::vector<std::string>& destinationAirports) {

    // If id exists, update the subscription
    for (auto& sub : inboundsSubscriptions) {
        if (sub->requestId == requestId) {
            sub->viaFixes = viaFixes;
            sub->destinationFixes = destinationFixes;
            sub->destinationAirports = destinationAirports;
            return;
        }
    }

    // Else create a new timeline
    std::shared_ptr<InboundsToFixSubscription> sub = std::make_shared<InboundsToFixSubscription>();
    sub->requestId = requestId;
    sub->viaFixes = viaFixes;
    sub->destinationFixes = destinationFixes;
    sub->destinationAirports = destinationAirports;
    inboundsSubscriptions.push_back(sub);

    sendUpdatedRunwayStatuses(requestId);
}

void AmanPlugIn::onRequestOutboundsFromAirport(long requestId, const std::string& icao) {
    // If id exists, update the subscription
    for (auto& sub : outboundsSubscriptions) {
        if (sub->requestId == requestId) {
            sub->airport = icao;
            return;
        }
    }
    // Else create a new timeline
    std::shared_ptr<OutboundsSubscription> sub = std::make_shared<OutboundsSubscription>();
    sub->requestId = requestId;
    sub->airport = icao;
    outboundsSubscriptions.push_back(sub);

    sendUpdatedRunwayStatuses(requestId);
}

void AmanPlugIn::onUnsubscribe(long requestId) {
    for (auto it = inboundsSubscriptions.begin(); it != inboundsSubscriptions.end(); ++it) {
        if ((*it)->requestId == requestId) {
            inboundsSubscriptions.erase(it);
            break;
        }
    }
}

void AmanPlugIn::onSetCtot(const std::string& callSign, long ctot) {
    CRadarTarget rt = RadarTargetSelect(callSign.c_str());
    if (rt.IsValid()) {
        CFlightPlan fp = rt.GetCorrelatedFlightPlan();
        if (fp.IsValid()) {
            // Format ctot (unix ts) to HH:MM
            time_t ctotTime = ctot;
            struct tm* ctotTm = gmtime(&ctotTime);
            char ctotStr[6];
            strftime(ctotStr, sizeof(ctotStr), "%H:%M", ctotTm);
            fp.GetFlightPlanData().SetEstimatedDepartureTime(ctotStr);
        }
    }
}

void AmanPlugIn::onClientDisconnected() {
    // Remove all subscriptions when the client disconnects
    inboundsSubscriptions.clear();
    outboundsSubscriptions.clear();
}

void AmanPlugIn::onErrorProcessingMessage(const std::string& errorMessage) {
    // Display an error message to the user
    DISPLAY_WARNING(errorMessage.c_str());
}

std::vector<AmanAircraft> AmanPlugIn::getInboundsForAirport(const std::string& airportIcao) {
    long int timeNow = static_cast<long int>(std::time(nullptr)); // Current UNIX-timestamp in seconds
    int transAlt = this->GetTransitionAltitude();

    CRadarTarget asel = RadarTargetSelectASEL();
    CRadarTarget rt;
    std::vector<AmanAircraft> aircraftList;
    for (rt = RadarTargetSelectFirst(); rt.IsValid(); rt = RadarTargetSelectNext(rt)) {
        float groundSpeed = rt.GetPosition().GetReportedGS();
        if (groundSpeed < 60) {
            continue;
        }

        CFlightPlanData fpd = rt.GetCorrelatedFlightPlan().GetFlightPlanData();
        if (fpd.GetDestination() != airportIcao) {
            continue;
        }

        CFlightPlanExtractedRoute route = rt.GetCorrelatedFlightPlan().GetExtractedRoute();
        bool isSelectedAircraft = asel.IsValid() && rt.GetCallsign() == asel.GetCallsign();
        auto assignedStarName = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetStarName();

        AmanAircraft ac;
        ac.callsign = rt.GetCallsign();
        ac.arrivalRunway = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetArrivalRwy();
        ac.assignedStar = assignedStarName;
        ac.icaoType = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetAircraftFPType();
        ac.assignedDirectRouting = rt.GetCorrelatedFlightPlan().GetControllerAssignedData().GetDirectToPointName();
        ac.trackingController = rt.GetCorrelatedFlightPlan().GetTrackingControllerId();
        ac.isSelected = isSelectedAircraft;
        ac.wtc = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetAircraftWtc();
        ac.secondsBehindPreceeding = 0; // Updated in the for-loop below
        ac.scratchPad = rt.GetCorrelatedFlightPlan().GetControllerAssignedData().GetScratchPadString();
        ac.groundSpeed = rt.GetPosition().GetReportedGS();
        ac.pressureAltitude = rt.GetPosition().GetPressureAltitude();
        ac.flightLevel = rt.GetPosition().GetFlightLevel();
        ac.remainingRoute = findExtractedRoutePoints(rt);
        ac.arrivalAirportIcao = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetDestination();
        ac.latitude = rt.GetPosition().GetPosition().m_Latitude;
        ac.longitude = rt.GetPosition().GetPosition().m_Longitude;
        ac.flightPlanTas = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetTrueAirspeed();
        aircraftList.push_back(ac);
    }

    return aircraftList;
}

std::vector<DmanAircraft> AmanPlugIn::getOutboundsFromAirport(const std::string& airport) {

    auto departures = std::vector<DmanAircraft>();

    // Get every flight plan
    for (CFlightPlan fp = FlightPlanSelectFirst(); fp.IsValid(); fp = FlightPlanSelectNext(fp)) {

        auto fpd = fp.GetFlightPlanData();

        // Check if the flight plan is a departure
        if (fp.GetFlightPlanData().GetOrigin() == airport) {
            DmanAircraft ac;
            ac.callsign = fp.GetCallsign();
            ac.sid = fpd.GetSidName();
            ac.runway = fpd.GetDepartureRwy();
            const char* departureTime = fpd.GetEstimatedDepartureTime();
            ac.estimatedDepartureTime = processDepartureTime(departureTime);
            ac.icaoType = fpd.GetAircraftFPType();
            ac.wakeCategory = fpd.GetAircraftWtc();

            departures.push_back(ac);
        }
    }

    return departures;
}

std::vector<RunwayStatus> AmanPlugIn::collectRunwayStatuses(const std::string& airportIcao) {
    std::vector<RunwayStatus> activeRunways;

    for (auto airport = this->SectorFileElementSelectFirst(EuroScopePlugIn::SECTOR_ELEMENT_AIRPORT);
         airport.IsValid();
         airport = this->SectorFileElementSelectNext(airport, EuroScopePlugIn::SECTOR_ELEMENT_AIRPORT)) {

        std::string currentIcao = airport.GetName();
        if (currentIcao != airportIcao)
            continue;

        for (auto runway = this->SectorFileElementSelectFirst(EuroScopePlugIn::SECTOR_ELEMENT_RUNWAY);
                runway.IsValid();
                runway = this->SectorFileElementSelectNext(runway, EuroScopePlugIn::SECTOR_ELEMENT_RUNWAY)) {

            auto runwayAirportName = trimString(std::string(runway.GetAirportName()));
            if (runwayAirportName == airportIcao) {
                for (int runwayDirection = 0; runwayDirection < 2; runwayDirection++) {
                    if (runwayAirportName == airportIcao) {
                        auto runwayName = trimString(std::string(runway.GetRunwayName(runwayDirection)));
                        activeRunways.push_back({ 
                            airportIcao,
                            runwayName,
                            runway.IsElementActive(true, runwayDirection), // Departures
                            runway.IsElementActive(false, runwayDirection) // Arrivals
                        });
                    }
                }
            }
        }
        break; // ICAO found, no need to continue
    }

    return activeRunways;
}

inline std::string AmanPlugIn::trimString(const std::string& value) {
    return std::regex_replace(value, std::regex("^ +| +$|( ) +"), "$1");
}

// HHMM to epoch time
long AmanPlugIn::processDepartureTime(const std::string& departureTime) {
    // Validate length (should be exactly 4 characters)
    if (departureTime.length() != 4) {
        std::cerr << "Invalid departure time format: " << departureTime << std::endl;
        return -1;
    }

    try {
        // Parse hour and minute
        int hour = std::stoi(departureTime.substr(0, 2));
        int minute = std::stoi(departureTime.substr(2, 2));

        // Validate hour and minute range
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw std::out_of_range("Hour or minute out of range");
        }

        // Get current UTC date
        struct tm tm {};
        time_t now;
        time(&now);
        gmtime_s(&tm, &now);  // Use UTC time

        // Set parsed values (keeping the same date)
        tm.tm_hour = hour;
        tm.tm_min = minute;
        tm.tm_sec = 0;

        // Convert to epoch time (UTC)
        time_t t = _mkgmtime(&tm); // Windows-specific function for UTC conversion

        return static_cast<long>(t);
    } catch (const std::exception& e) {
        std::cerr << "Error parsing departure time: " << e.what() << std::endl;
        return -1;
    }
}