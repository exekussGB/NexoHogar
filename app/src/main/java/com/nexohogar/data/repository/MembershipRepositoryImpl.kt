package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.GetUserUsageRequest
import com.nexohogar.data.network.IsPremiumRequest
import com.nexohogar.data.network.MembershipApi
import com.nexohogar.domain.model.UserUsage
import com.nexohogar.domain.repository.MembershipRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class MembershipRepositoryImpl(
    private val membershipApi: MembershipApi,
    private val supabaseClient: SupabaseClient
) : MembershipRepository {

    override suspend fun getUserUsage(householdId: String): AppResult<UserUsage> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return AppResult.Error("Usuario no autenticado")

            android.util.Log.d("MembershipRepo", "userId=$userId householdId=$householdId")


            val dtoList = membershipApi.getUserUsage(
                GetUserUsageRequest(
                    p_user_id      = userId,
                    p_household_id = householdId
                )
            )

            val dto = dtoList.firstOrNull() ?: run {
                android.util.Log.w("MembershipRepo", "RPC retornó array vacío para householdId=$householdId")
                return@run com.nexohogar.data.remote.dto.UserUsageDto()
            }

            AppResult.Success(dto.toDomain())
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepo", "Error: ${e.message}")
            AppResult.Error(e.message ?: "Error al obtener uso", e)
        }
    }

    override suspend fun isPremium(householdId: String): AppResult<Boolean> {
        return try {
            if (householdId.isBlank()) {
                android.util.Log.w("MembershipRepo", "isPremium() llamado con householdId vacío")
                return AppResult.Error("householdId es requerido")
            }

            val result = membershipApi.isPremium(IsPremiumRequest(p_household_id = householdId))
            AppResult.Success(result)
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepo", "Error al verificar premium: ${e.message}")
            AppResult.Error(e.message ?: "Error al verificar premium", e)
        }
    }
}