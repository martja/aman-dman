#include "stdafx.h"
#include "JsonMessageHelper.h"

#define RAPIDJSON_HAS_STDSTRING 1

#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

using namespace rapidjson;

const std::string JsonMessageHelper::getJsonOfAircraft(long timelineId, const std::vector<AmanAircraft>& aircraftList) {
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

    document.AddMember("type", "timelineUpdate", allocator);
    document.AddMember("timelineId", timelineId, allocator);
    document.AddMember("arrivals", arrivalsArray, allocator);

    StringBuffer sb;
    Writer<StringBuffer> writer(sb);
    document.Accept(writer);

    return sb.GetString();
}
