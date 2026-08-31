package org.orev.nahidka.feature.dashboard

import kotlinx.coroutines.Deferred
import org.orev.nahidka.api.Dashboard
import org.orev.nahidka.api.FeaturesApi

class DashboardService(private val featuresApi: FeaturesApi) {

    fun getDashboard(): Deferred<Dashboard> {
        return featuresApi.getDashboard()
    }
}
