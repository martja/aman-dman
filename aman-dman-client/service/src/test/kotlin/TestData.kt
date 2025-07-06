import no.vaccsca.amandman.common.AircraftPerformance

val b738performance = AircraftPerformance(
    ICAO = "B738",
    takeOffV2 = 145,
    takeOffDistance = 2300,
    takeOffWTC = 'M',
    takeOffRECAT = "Upper Medium",
    takeOffMTOW = 70530,
    initialClimbIAS = 165,
    initialClimbROC = 3000,
    climb150IAS = 290,
    climb150ROC = 2000,
    climb240IAS = 290,
    climb240ROC = 2000,
    machClimbMACH = 0.78f,
    machClimbROC = 1500,
    cruiseTAS = 460,
    cruiseMACH = 0.79f,
    cruiseCeiling = 41000, // in feet
    cruiseRange = 2000, // in nautical miles
    initialDescentMACH = 0.78f,
    initialDescentROD = 800, // in feet per minute
    descentIAS = 280,
    descentROD = 3500, // in feet per minute
    approachIAS = 250,
    approachROD = 1500, // in feet per minute
    approachMCS = 210, // minimum clean speed
    landingVat = 147, // Vapp
    landingDistance = 1600, // in meters
    landingAPC = "D" // Approach Category D
)