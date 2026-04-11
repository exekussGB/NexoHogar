package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.network.SubscriptionsApi
import com.nexohogar.domain.model.Plan
import com.nexohogar.domain.model.Subscription
import com.nexohogar.domain.repository.SubscriptionsRepository
import java.io.IOException

class SubscriptionsRepositoryImpl(
    private val api: SubscriptionsApi
) : SubscriptionsRepository {

    override suspend fun getCurrentUserSubscription(): AppResult<Subscription> {
        return try {
            val response = api.getCurrentUserSubscription()
            if (response.isSuccessful && response.body() != null) {
                AppResult.Success(response.body()!!.toDomain())
            } else {
                AppResult.Error("Error obteniendo suscripción: ${response.code()}")
            }
        } catch (e: IOException) {
            AppResult.Error("Error de red: ${e.message}")
        } catch (e: Exception) {
            AppResult.Error("Error inesperado: ${e.message}")
        }
    }

    override suspend fun getUserSubscription(userId: String): AppResult<Subscription> {
        return try {
            val response = api.getUserSubscription(userId)
            if (response.isSuccessful && response.body() != null) {
                AppResult.Success(response.body()!!.toDomain())
            } else {
                AppResult.Error("Error obteniendo suscripción: ${response.code()}")
            }
        } catch (e: IOException) {
            AppResult.Error("Error de red: ${e.message}")
        } catch (e: Exception) {
            AppResult.Error("Error inesperado: ${e.message}")
        }
    }

    override suspend fun getCurrentUserPlan(): AppResult<Plan> {
        return when (val result = getCurrentUserSubscription()) {
            is AppResult.Success -> AppResult.Success(result.data.plan)
            is AppResult.Error -> AppResult.Error(result.message)
            is AppResult.Loading -> AppResult.Loading
        }
    }
}
