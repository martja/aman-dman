#include "stdafx.h"
#include "JsonMessageHelper.h"

#define RAPIDJSON_HAS_STDSTRING 1

#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

using namespace rapidjson;

const std::string JsonMessageHelper::getJsonOfFixInbounds(long requestId, const std::vector<AmanAircraft>& aircraftList) {
    Document document;
    document.SetObject();
    Value arrivalsArray(kArrayType);

    Document::AllocatorType& allocator = document.GetAllocator();

    for (auto& inbound : aircraftList) {
        Value arrivalObject(kObjectType);

        arrivalObject.AddMember("callsign", inbound.callsign, allocator);
        arrivalObject.AddMember("icaoType", inbound.icaoType, allocator);
        arrivalObject.AddMember("wtc", inbound.wtc, allocator);
        arrivalObject.AddMember("runway", inbound.arrivalRunway, allocator);
        arrivalObject.AddMember("star", inbound.assignedStar, allocator);
        arrivalObject.AddMember("finalFixEta", inbound.targetFixEta, allocator);
        arrivalObject.AddMember("eta", inbound.destinationEta, allocator);
        arrivalObject.AddMember("remainingDist", inbound.distLeft, allocator);
        arrivalObject.AddMember("viaFix", inbound.viaFix, allocator);
        arrivalObject.AddMember("finalFix", inbound.finalFix, allocator);
        arrivalObject.AddMember("flightLevel", inbound.flightLevel, allocator);
        arrivalObject.AddMember("pressureAltitude", inbound.pressureAltitude, allocator);
        arrivalObject.AddMember("groundSpeed", inbound.groundSpeed, allocator);
        arrivalObject.AddMember("secondsBehindPreceeding", inbound.secondsBehindPreceeding, allocator);
        arrivalObject.AddMember("isAboveTransAlt", inbound.isAboveTransAlt, allocator);
        arrivalObject.AddMember("trackedByMe", inbound.trackedByMe, allocator);
        arrivalObject.AddMember("direct", inbound.nextFix, allocator);
        arrivalObject.AddMember("scratchPad", inbound.scratchPad, allocator);

        arrivalsArray.PushBack(arrivalObject, allocator);
    }

    document.AddMember("type", "fixInboundList", allocator);
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

    document.AddMember("type", "departureList", allocator);
    document.AddMember("requestId", requestId, allocator);
    document.AddMember("outbounds", departuresArray, allocator);

    StringBuffer sb;
    Writer<StringBuffer> writer(sb);
    document.Accept(writer);

    return sb.GetString();
}
