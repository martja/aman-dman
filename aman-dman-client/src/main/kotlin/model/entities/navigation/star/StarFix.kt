package org.example.model.entities.navigation.star

data class StarFix(
    val id: String,
    val starAltitudeConstraint: StarAltitudeConstraint? = null,
    val starSpeedConstraint: StarSpeedConstraint? = null
) {
    class StarFixBuilder(private val id: String) {
        private var minAltFt: Int? = null
        private var maxAltFt: Int? = null
        private var exactAltFt: Int? = null

        private var minSpeedKts: Int? = null
        private var maxSpeedKts: Int? = null
        private var exactSpeedKts: Int? = null

        fun minAlt(ft: Int) = apply { minAltFt = ft }
        fun maxAlt(ft: Int) = apply { maxAltFt = ft }
        fun exactAlt(ft: Int) = apply { exactAltFt = ft }

        fun minSpeed(kts: Int) = apply { minSpeedKts = kts }
        fun maxSpeed(kts: Int) = apply { maxSpeedKts = kts }
        fun exactSpeed(kts: Int) = apply { exactSpeedKts = kts }

        fun build(): StarFix {
            val altConstraint = if (minAltFt != null || maxAltFt != null || exactAltFt != null) {
                StarAltitudeConstraint(minAltFt, maxAltFt, exactAltFt)
            } else null

            val starSpeedConstraint = if (minSpeedKts != null || maxSpeedKts != null || exactSpeedKts != null) {
                StarSpeedConstraint(minSpeedKts, maxSpeedKts, exactSpeedKts)
            } else null

            return StarFix(id, altConstraint, starSpeedConstraint)
        }
    }
}



