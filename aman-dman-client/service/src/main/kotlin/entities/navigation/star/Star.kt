package org.example.entities.navigation.star


data class Star(
    val id: String,
    val airfieldElevationFt: Int,
    val fixes: List<StarFix>
)

fun starFix(id: String, block: StarFix.StarFixBuilder.() -> Unit): StarFix {
    return StarFix.StarFixBuilder(id).apply(block).build()
}

val lunip4l = Star(
    id = "LUNIP4L",
    airfieldElevationFt = 700,
    fixes = listOf(
        starFix("LUNIP") {
            speed(Constraint.Max(250))
        },
        starFix("DEVKU") {
            altitude(Constraint.Min(12000))
            speed(Constraint.Max(250))
        },
        starFix("GM416") {
            altitude(Constraint.Exact(11000))
            speed(Constraint.Max(220))
        },
        starFix("GM417") {
            altitude(Constraint.Exact(11000))
            speed(Constraint.Max(220))
        },
        starFix("GM415") {
            altitude(Constraint.Exact(11000))
            speed(Constraint.Max(220))
        },
        starFix("GM414") {
            altitude(Constraint.Exact(11000))
            speed(Constraint.Max(220))
        },
        starFix("INSUV") {
            altitude(Constraint.Min(5000))
            altitude(Constraint.Exact(5000))
            speed(Constraint.Max(220))
        },
        starFix("NOSLA") {
            speed(Constraint.Max(200))
        },
        starFix("XEMEN") {
            altitude(Constraint.Exact(3500))
            speed(Constraint.Max(200))
        }
    )
)
