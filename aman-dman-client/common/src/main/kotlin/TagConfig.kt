package org.example

data class TagConfig(
    val name: String,
    var included: Boolean = true,
    var align: String = "Left",
    var paddingLeft: Int = 0,
    var paddingRight: Int = 0
)