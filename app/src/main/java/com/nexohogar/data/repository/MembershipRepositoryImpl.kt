package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.GetUserUsageRequest
import com.nexohogar.data.network.IsPremiumRequest
import com.nexohogar.data.network.MembershipApi
import com.nexohogar.domain.model.UserUsage
import com.nexohogar.domain.repository.MembershipRepository

// FIX: Reescrito para usar suspend + AppResult, igual que WishlistRepositoryImpl.
// Se eliminaron Flow<Result<>>, upgradeToMembership, checkMembershipStatus y
// getMembershipInfo que no están en la interfaz MembershipRepository.
// Constructor reducido a 1 parámetro (membershipApi) — sin authRepository.
class MembershipRepositoryImpl(
    private val membershipApi: MembershipApi
) : MembershipRepository {

    override suspend fun getUserUsage(householdId: String): AppResult<UserUsage> {
        return try {
            val dto = membershipApi.getUserUsage(
                GetUserUsageRequest(household_id = householdId)
            )
            AppResult.Success(dto.toDomain())
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al obtener uso", e)
        }
    }

    override suspend fun isPremium(): AppResult<Boolean> {
        return try {
            // El household_id se infiere del JWT en el backend (RLS).
            // Se pasa vacío porque la interfaz no recibe householdId en isPremium().
            val result = membershipApi.isPremium(IsPremiumRequest(household_id = ""))
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al verificar premium", e)
        }
    }
}
