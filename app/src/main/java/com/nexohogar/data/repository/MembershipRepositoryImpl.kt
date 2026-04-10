package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.GetUserUsageRequest
import com.nexohogar.data.network.IsPremiumRequest
import com.nexohogar.data.network.MembershipApi
import com.nexohogar.domain.model.UserUsage
import com.nexohogar.domain.repository.MembershipRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth


// FIX: Reescrito para usar suspend + AppResult, igual que WishlistRepositoryImpl.
// Se eliminaron Flow<Result<>>, upgradeToMembership, checkMembershipStatus y
// getMembershipInfo que no están en la interfaz MembershipRepository.
// Constructor reducido a 1 parámetro (membershipApi) — sin authRepository.
class MembershipRepositoryImpl(
    private val membershipApi: MembershipApi,
    private val supabaseClient: SupabaseClient
) : MembershipRepository {

    override suspend fun getUserUsage(householdId: String): AppResult<UserUsage> {
        return try {
            // Obtener userId del usuario autenticado
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return AppResult.Error("Usuario no autenticado")

            val dto = membershipApi.getUserUsage(
                GetUserUsageRequest(
                    p_user_id      = userId,
                    p_household_id = householdId
                )
            )
            AppResult.Success(dto.toDomain())
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener uso", e)
        }
    }

    override suspend fun isPremium(): AppResult<Boolean> {
        return try {
            val result = membershipApi.isPremium(IsPremiumRequest(p_household_id = ""))
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al verificar premium", e)
        }
    }
}
