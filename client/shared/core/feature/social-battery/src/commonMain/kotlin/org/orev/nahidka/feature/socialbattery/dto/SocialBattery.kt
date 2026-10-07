package org.orev.nahidka.feature.socialbattery.dto

data class SocialBattery(val percentage: Int) {
    init {
        require(percentage in PERCENTAGE_RANGE) {
            "Social battery percentage must be between ${PERCENTAGE_RANGE.first} and ${PERCENTAGE_RANGE.last}"
        }
    }

    companion object {
        val PERCENTAGE_RANGE = 0..100
    }
}
