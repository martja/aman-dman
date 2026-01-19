package no.vaccsca.amandman.view.entity

import javax.swing.SwingUtilities

class SharedValue<T>(initialValue: T) {
    private val listeners = mutableListOf<(T) -> Unit>()

    var value: T = initialValue
        set(newValue) {
            if (field != newValue) {
                field = newValue
                notifyListeners(newValue)
            }
        }

    fun addListener(listener: (T) -> Unit) {
        listeners.add(listener)
        listener(value)
    }

    fun removeListener(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    private fun notifyListeners(newValue: T) {
        if (SwingUtilities.isEventDispatchThread()) {
            listeners.forEach { it(newValue) }
        } else {
            SwingUtilities.invokeLater {
                listeners.forEach { it(newValue) }
            }
        }
    }

    fun update(transform: (T) -> T) {
        value = transform(value)
    }

    fun peek(): T = value
}