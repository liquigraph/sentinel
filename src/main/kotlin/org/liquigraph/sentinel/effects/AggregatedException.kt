package org.liquigraph.sentinel.effects

class AggregatedException(val exceptions: List<Throwable>) : Throwable(messagesOf(exceptions), exceptions.firstOrNull()) {

    companion object {
        fun messagesOf(exceptions: List<Throwable>): String {
            return exceptions.map { it.message }.joinToString("\n")
        }
    }
}