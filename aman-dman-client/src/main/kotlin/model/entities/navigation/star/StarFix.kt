package org.example.model.entities.navigation.star

data class StarFix(
    val id: String,
    val starAltitudeConstraint: Constraint? = null,
    val starSpeedConstraint: Constraint? = null
) {
    class StarFixBuilder(private val id: String) {
        private var altitudeConstraint: Constraint? = null
        private var speedConstraint: Constraint? = null

        fun altitude(constraint: Constraint): StarFixBuilder {
            altitudeConstraint = constraint
            return this
        }

        fun speed(constraint: Constraint): StarFixBuilder {
            speedConstraint = constraint
            return this
        }

        fun build(): StarFix {
            return StarFix(id, altitudeConstraint, speedConstraint)
        }
    }
}



