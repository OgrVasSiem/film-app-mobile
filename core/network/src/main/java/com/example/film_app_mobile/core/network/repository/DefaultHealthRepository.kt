package com.example.film_app_mobile.core.network.repository

import com.example.film_app_mobile.core.network.ApiExecutor
import com.example.film_app_mobile.core.network.api.HealthApi
import com.example.film_app_mobile.core.network.model.ApiResult
import com.example.film_app_mobile.core.network.model.map

internal class DefaultHealthRepository(
    private val api: HealthApi,
    private val executor: ApiExecutor,
) : HealthRepository {
    override suspend fun checkServer(): ApiResult<HealthStatus> =
        executor.execute { api.health() }.map { HealthStatus(it.status) }

    override suspend fun checkDatabase(): ApiResult<Unit> =
        executor.executeUnit { api.databaseHealth() }
}
