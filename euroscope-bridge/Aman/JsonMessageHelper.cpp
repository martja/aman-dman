#include "stdafx.h"
#include "JsonMessageHelper.h"

#define RAPIDJSON_HAS_STDSTRING 1

#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

using namespace rapidjson;

const std::string JsonMessageHelper::getJsonOfArrivals(long requestId, const std::vector<AmanAircraft>& aircraftList) {
    Document document;
    document.SetObject();
    Value arrivalsArray(kArrayType);

    Document::AllocatorType& allocator = document.GetAllocator();

    for (auto& inbound : aircraftList) {
        Value arrivalObject(kObjectType);

        arrivalObject.AddMember("callsign", inbound.callsign, allocator);
        arrivalObject.AddMember("icaoType", inbound.icaoType, allocator);
        arrivalObject.AddMember("latitude", inbound.latitude, allocator);
        arrivalObject.AddMember("longitude", inbound.longitude, allocator);
        arrivalObject.AddMember("flightLevel", inbound.flightLevel, allocator);
        arrivalObject.AddMember("pressureAltitude", inbound.pressureAltitude, allocator);
        arrivalObject.AddMember("groundSpeed", inbound.groundSpeed, allocator);
        arrivalObject.AddMember("arrivalAirportIcao", inbound.arrivalAirportIcao, allocator);

        if (!inbound.scratchPad.empty())
            arrivalObject.AddMember("scratchPad", inbound.scratchPad, allocator);

        if (!inbound.assignedStar.empty())
            arrivalObject.AddMember("assignedStar", inbound.assignedStar, allocator);

        if (!inbound.assignedDirectRouting.empty())
            arrivalObject.AddMember("assignedDirect", inbound.assignedDirectRouting, allocator);

        if (!inbound.arrivalRunway.empty())
            arrivalObject.AddMember("assignedRunway", inbound.arrivalRunway, allocator);

        if (!inbound.trackingController.empty())
            arrivalObject.AddMember("trackingController", inbound.trackingController, allocator);

        if (inbound.flightPlanTas > 0)
            arrivalObject.AddMember("flightPlanTas", inbound.flightPlanTas, allocator);

        Value routePoints(kArrayType);
        for (auto& point : inbound.remainingRoute) {
            Value pointObject(kObjectType);
            pointObject.AddMember("name", point.name, allocator);
            pointObject.AddMember("isOnStar", point.isOnStar, allocator);
            pointObject.AddMember("latitude", point.latitude, allocator);
            pointObject.AddMember("longitude", point.longitude, allocator);
            pointObject.AddMember("isPassed", point.isPassed, allocator);
            routePoints.PushBack(pointObject, allocator);
        }

        arrivalObject.AddMember("route", routePoints, allocator);

        arrivalsArray.PushBack(arrivalObject, allocator);
    }

    document.AddMember("type", "arrivals", allocator);
    document.AddMember("requestId", requestId, allocator);
    document.AddMember("inbounds", arrivalsArray, allocator);

    StringBuffer sb;
    Writer<StringBuffer> writer(sb);
    document.Accept(writer);

    return sb.GetString();
}

const std::string JsonMessageHelper::getJsonOfDepartures(long requestId, const std::vector<DmanAircraft>& aircraftList) {
    Document document;
    document.SetObject();
    Value departuresArray(kArrayType);

    Document::AllocatorType& allocator = document.GetAllocator();

    for (auto& outbound : aircraftList) {
        Value departureObject(kObjectType);
        departureObject.AddMember("callsign", outbound.callsign, allocator);
        departureObject.AddMember("sid", outbound.sid, allocator);
        departureObject.AddMember("runway", outbound.runway, allocator);
        departureObject.AddMember("estimatedDepartureTime", outbound.estimatedDepartureTime, allocator);
        departureObject.AddMember("icaoType", outbound.icaoType, allocator);
        departureObject.AddMember("wakeCategory", outbound.wakeCategory, allocator);

        departuresArray.PushBack(departureObject, allocator);
    }

    document.AddMember("type", "departures", allocator);
    document.AddMember("requestId", requestId, allocator);
    document.AddMember("outbounds", departuresArray, allocator);

    StringBuffer sb;
    Writer<StringBuffer> writer(sb);
    document.Accept(writer);

    return sb.GetString();
}
