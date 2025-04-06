package org.example.entities.navigation.star

sealed class Constraint {
    class Min(val value: Int) : Constraint()
    class Max(val value: Int) : Constraint()
    class Exact(val value: Int) : Constraint()
    class Between(val min: Int, val max: Int) : Constraint()
}
