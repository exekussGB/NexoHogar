package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.SubscriptionDto
import retrofit2.Response
import retrofit2.http.GET

interface SubscriptionsApi {
    /**
     * Obtiene la suscripción actual del usuario autenticado.
     * La API debe devolver la suscripción con el plan embebido.
     */
    @GET("api/v1/subscriptions/current")
    suspend fun getCurrentUserSubscription(): Response<SubscriptionDto>

    /**
     * Obtiene la suscripción del usuario por ID
     */
    @GET("api/v1/subscriptions/{userId}")
    suspend fun getUserSubscription(userId: String): Response<SubscriptionDto>
}
