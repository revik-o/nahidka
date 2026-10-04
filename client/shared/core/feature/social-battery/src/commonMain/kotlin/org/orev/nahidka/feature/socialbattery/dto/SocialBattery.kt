package org.orev.nahidka.feature.socialbattery.dto

data class SocialBattery(val percentage: Int) {
    init {
        require(percentage in 0..100) { "Social battery percentage must be between 0 and 100" }
    }
}
