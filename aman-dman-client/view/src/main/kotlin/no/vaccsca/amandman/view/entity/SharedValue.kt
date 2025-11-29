package no.vaccsca.amandman.view.entity

class SharedValue<T>(initialValue: T) {
    private val listeners = mutableListOf<(T) -> Unit>()
    var value: T = initialValue
        set(newValue) {
            field = newValue
            listeners.forEach { it(newValue) }
        }

    fun addListener(listener: (T) -> Unit) {
        listeners.add(listener)
    }
}