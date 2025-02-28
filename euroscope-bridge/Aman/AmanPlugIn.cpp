#include "stdafx.h"

#include "AmanAircraft.h"
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

AmanPlugIn::AmanPlugIn() 
    : CPlugIn(COMPATIBILITY_CODE, "Arrival Manager", "3.2.0", "https://git.io/Jt3S8", "Open source")
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
    std::vector<std::string> viaFixes({ "VALPU", "INSUV" });
    std::vector<std::string> destinationAirports({"ENGM"});


    for each(auto & timeline in timelines) {
        auto inbounds = getAircraftForTimeline(timeline);
        auto inboundsJson = jsonSerializer.getJsonOfAircraft(timeline->identifier, inbounds);
        sendData(inboundsJson);
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

double AmanPlugIn::findRemainingDist(CRadarTarget radarTarget, CFlightPlanExtractedRoute extractedRoute, int fixIndex) {
    int closestFixIndex = extractedRoute.GetPointsCalculatedIndex();
    int assignedDirectFixIndex = extractedRoute.GetPointsAssignedIndex();

    int nextFixIndex = assignedDirectFixIndex > -1 ? assignedDirectFixIndex : closestFixIndex;
    double totalDistance =
        radarTarget.GetPosition().GetPosition().DistanceTo(extractedRoute.GetPointPosition(nextFixIndex));

    // Ignore waypoints prior to nextFixIndex
    for (int i = nextFixIndex; i < fixIndex; i++) {
        totalDistance += extractedRoute.GetPointPosition(i).DistanceTo(extractedRoute.GetPointPosition(i + 1));
    }
    return totalDistance;
}

std::vector<AmanAircraft> AmanPlugIn::getAircraftForTimeline(std::shared_ptr<AmanTimeline> timeline) {
    auto pAircraftList = std::vector<AmanAircraft>();

    for each (auto finalFix in timeline->destinationFixes) {
        auto inbounds = getInboundsForFix(finalFix, timeline->viaFixes, timeline->destinationAirports);
        pAircraftList.insert(pAircraftList.end(), inbounds.begin(), inbounds.end());
    }

    return pAircraftList;
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

void AmanPlugIn::onRegisterTimeline(long timelineId, const std::vector<std::string>& viaFixes, const std::vector<std::string>& destinationFixes, const std::vector<std::string>& destinationAirports) {

    // If id exists, update the timeline
    for (auto& timeline : timelines) {
        if (timeline->identifier == timelineId) {
            timeline->viaFixes = viaFixes;
            timeline->destinationFixes = destinationFixes;
            timeline->destinationAirports = destinationAirports;
            return;
        }
    }

    // Else create a new timeline
    std::shared_ptr<AmanTimeline> timeline = std::make_shared<AmanTimeline>();
    timeline->identifier = timelineId;
    timeline->viaFixes = viaFixes;
    timeline->destinationFixes = destinationFixes;
    timeline->destinationAirports = destinationAirports;
    timelines.push_back(timeline);
}

void AmanPlugIn::onUnregisterTimeline(long timelineId) {
    for (auto it = timelines.begin(); it != timelines.end(); ++it) {
        if ((*it)->identifier == timelineId) {
            timelines.erase(it);
            break;
        }
    }
}

std::vector<AmanAircraft> AmanPlugIn::getInboundsForFix(const std::string& fixName, std::vector<std::string> viaFixes, std::vector<std::string> destinationAirports) {
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

        CFlightPlanExtractedRoute route = rt.GetCorrelatedFlightPlan().GetExtractedRoute();
        CFlightPlanPositionPredictions predictions = rt.GetCorrelatedFlightPlan().GetPositionPredictions();
        bool isSelectedAircraft = asel.IsValid() && rt.GetCallsign() == asel.GetCallsign();

        int targetFixIndex = getFixIndexByName(route, fixName);

        if (targetFixIndex > -1 && // Target fix found
            route.GetPointDistanceInMinutes(targetFixIndex) > -1 && // Target fix has not been passed
            hasCorrectDestination(rt.GetCorrelatedFlightPlan().GetFlightPlanData(), destinationAirports)) { // Aircraft going to the correct destination

            bool targetFixIsDestination = targetFixIndex == route.GetPointsNumber() - 1;
            int timeToFix;

            float restDistance = predictions.GetPosition(predictions.GetPointsNumber() - 1).DistanceTo(route.GetPointPosition(targetFixIndex));
            int timeToDestination = (predictions.GetPointsNumber() - 1) * 60 + (restDistance / groundSpeed) * 60.0 * 60.0;

            if (targetFixIsDestination) {
                timeToFix = timeToDestination;
            } else {
                // Find the two position prediction points closest to the target point
                float min1dist = INFINITE;
                float min2dist = INFINITE;
                float minScore = INFINITE;
                int predIndexBeforeWp = 0;

                for (int p = 0; p < predictions.GetPointsNumber(); p++) {
                    float dist1 = predictions.GetPosition(p).DistanceTo(route.GetPointPosition(targetFixIndex));
                    float dist2 = predictions.GetPosition(p + 1).DistanceTo(route.GetPointPosition(targetFixIndex));

                    if (dist1 + dist2 < minScore) {
                        min1dist = dist1;
                        min2dist = dist2;
                        minScore = dist1 + dist2;
                        predIndexBeforeWp = p;
                    }
                }
                float ratio = (min1dist / (min1dist + min2dist));
                timeToFix = predIndexBeforeWp * 60.0 + ratio * 60.0;
            }

            if (timeToFix > 0) {
                int viaFixIndex = getFirstViaFixIndex(route, viaFixes);

                AmanAircraft ac;
                ac.callsign = rt.GetCallsign();
                ac.finalFix = fixName;
                ac.arrivalRunway = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetArrivalRwy();
                ac.assignedStar = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetStarName();
                ac.icaoType = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetAircraftFPType();
                ac.nextFix = rt.GetCorrelatedFlightPlan().GetControllerAssignedData().GetDirectToPointName();
                ac.viaFix = viaFixes.at(viaFixIndex);
                ac.trackedByMe = rt.GetCorrelatedFlightPlan().GetTrackingControllerIsMe();
                ac.isSelected = isSelectedAircraft;
                ac.wtc = rt.GetCorrelatedFlightPlan().GetFlightPlanData().GetAircraftWtc();
                ac.targetFixEta = timeNow + timeToFix - rt.GetPosition().GetReceivedTime();
                ac.destinationEta = timeNow + timeToDestination - rt.GetPosition().GetReceivedTime();
                ac.distLeft = findRemainingDist(rt, route, targetFixIndex);
                ac.secondsBehindPreceeding = 0; // Updated in the for-loop below
                ac.scratchPad = rt.GetCorrelatedFlightPlan().GetControllerAssignedData().GetScratchPadString();
                ac.groundSpeed = rt.GetPosition().GetReportedGS();
                ac.pressureAltitude = rt.GetPosition().GetPressureAltitude();
                ac.flightLevel = rt.GetPosition().GetFlightLevel();
                ac.isAboveTransAlt = ac.pressureAltitude > transAlt;
                aircraftList.push_back(ac);
            }
        }
    }
    if (aircraftList.size() > 0) {
        std::sort(aircraftList.begin(), aircraftList.end());
        std::reverse(aircraftList.begin(), aircraftList.end());
        for (int i = 0; i < aircraftList.size() - 1; i++) {
            auto curr = &aircraftList[i];
            auto next = &aircraftList[i + 1];

            curr->secondsBehindPreceeding = curr->targetFixEta - next->targetFixEta;
        }
    }

    return aircraftList;
}
