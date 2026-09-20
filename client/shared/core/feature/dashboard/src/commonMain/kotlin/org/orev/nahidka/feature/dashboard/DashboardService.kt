package org.orev.nahidka.feature.dashboard

import org.orev.nahidka.api.Dashboard
import org.orev.nahidka.api.FeaturesApi

class DashboardService(private val featuresApi: FeaturesApi) {

    suspend fun getDashboard(): Dashboard {
        return featuresApi.getDashboard()
    }
}
