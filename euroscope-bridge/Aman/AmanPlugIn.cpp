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
    for each(auto & timeline in inboundsSubscriptions) {
        auto inbounds = collectResponseToInboundsSubscription(timeline);
        auto inboundsJson = jsonSerializer.getJsonOfFixInbounds(timeline->requestId, inbounds);
        enqueueMessage(inboundsJson);
    }

    for each(auto & timeline in outboundsSubscriptions) {
        auto outbounds = collectResponseToOutboundsSubscription(timeline);
        auto outboundsJson = jsonSerializer.getJsonOfDepartures(timeline->requestId, outbounds);
        enqueueMessage(outboundsJson);
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

std::vector<AmanAircraft> AmanPlugIn::collectResponseToInboundsSubscription(std::shared_ptr<InboundsToFixSubscription> timeline) {
    auto pAircraftList = std::vector<AmanAircraft>();

    for each (auto finalFix in timeline->destinationFixes) {
        auto inbounds = getInboundsForFix(finalFix, timeline->viaFixes, timeline->destinationAirports);
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

            // Calculate the time spent at different altitudes and the average heading. Altitudes are in 5000 ft intervals
            auto altitudesAndDuration = std::map<int, VerticalProfileSection>();
            int altIntervalRestTime = -1;
            float altIntervalRestDistance = -1;
            for (int p = 0; p < predictions.GetPointsNumber() - 1; p++) {
                int alt = predictions.GetAltitude(p);
                int heading = predictions.GetPosition(p).DirectionTo(predictions.GetPosition(p + 1));
                float distanceToNext = predictions.GetPosition(p).DistanceTo(predictions.GetPosition(p + 1));

                int nextAlt = predictions.GetAltitude(p + 1);

                // Floor altitude to nearest 5000 ft
                int currentAltFloor = (alt / 5000) * 5000;

                int secondsToNext = 60; // There is 1 minute between each prediction

                if (nextAlt < currentAltFloor) {
                    // Calculate accurate reamining time inside the current 5000 ft interval
                    float ratio = (float)(alt - currentAltFloor) / (float)(alt - nextAlt);
                    secondsToNext = ratio * 60.0;
                    float distanceToNextAltInterval = distanceToNext * ratio;
                    altIntervalRestTime = 60 - secondsToNext;
                    altIntervalRestDistance = distanceToNext - distanceToNextAltInterval;
                    distanceToNext = distanceToNextAltInterval;
                } else if (altIntervalRestTime > 0) {
                    // The rest of the time is spent at the next altitude interval
                    secondsToNext = altIntervalRestTime;
                    distanceToNext = altIntervalRestDistance;
                    altIntervalRestTime = -1;
                    altIntervalRestDistance = -1;
                }

                if (altitudesAndDuration.find(currentAltFloor) == altitudesAndDuration.end()) {
                    altitudesAndDuration[currentAltFloor] = VerticalProfileSection{ currentAltFloor + 5000, currentAltFloor, secondsToNext, heading, distanceToNext };
                } else {
                    altitudesAndDuration[currentAltFloor].secDuration += secondsToNext;
                    altitudesAndDuration[currentAltFloor].averageHeading = (altitudesAndDuration[currentAltFloor].averageHeading + heading) / 2;
                    altitudesAndDuration[currentAltFloor].distance += distanceToNext;
                }
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
                ac.altitudesAndDuration = altitudesAndDuration;
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