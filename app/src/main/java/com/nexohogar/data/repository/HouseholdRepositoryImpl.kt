package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.InviteCodeRequest
import com.nexohogar.data.remote.dto.JoinHouseholdRequest
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.model.HouseholdMember
import com.nexohogar.domain.repository.HouseholdRepository

class HouseholdRepositoryImpl(
    private val authApi: AuthApi
) : HouseholdRepository {

    override suspend fun getHouseholds(): AppResult<List<Household>> {
        return try {
            val response = authApi.getHouseholds()
            if (response.isSuccessful) {
                val households = response.body()?.map { dto ->
                    Household(
                        id          = dto.id,
                        name        = dto.name,
                        description = dto.description
                    )
                } ?: emptyList()
                AppResult.Success(households)
            } else {
                AppResult.Error("Error al obtener hogares: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun createHousehold(name: String): AppResult<Household> {
        return try {
            val response = authApi.createHousehold(CreateHouseholdRequest(name))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    AppResult.Success(
                        Household(
                            id          = body.id,
                            name        = body.name,
                            description = body.description
                        )
                    )
                } else {
                    AppResult.Error("El servidor no devolvió datos del hogar")
                }
            } else {
                AppResult.Error("Error al crear hogar: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getOrCreateInviteCode(householdId: String): AppResult<String> {
        return try {
            val response = authApi.getOrCreateInviteCode(InviteCodeRequest(householdId))
            if (response.isSuccessful) {
                val code = response.body()
                if (!code.isNullOrBlank()) {
                    AppResult.Success(code.trim('"'))
                } else {
                    AppResult.Error("No se recibió código de invitación")
                }
            } else {
                AppResult.Error("Error al obtener código: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun joinHouseholdByCode(inviteCode: String): AppResult<String> {
        return try {
            val response = authApi.joinHouseholdByCode(
                JoinHouseholdRequest(inviteCode.trim().uppercase())
            )
            if (response.isSuccessful) {
                val message = response.body()?.message
                    ?: "Solicitud enviada. El administrador debe aprobarla."
                AppResult.Success(message)
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Código de invitación inválido o expirado"
                    409 -> "Ya eres miembro de este hogar"
                    else -> "Error al unirse al hogar: ${response.code()}"
                }
                AppResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun getHouseholdMembers(householdId: String): AppResult<List<HouseholdMember>> {
        return try {
            val response = authApi.getHouseholdMembersWithEmail(
                mapOf("p_household_id" to householdId)
            )
            if (response.isSuccessful) {
                val members = response.body()?.map { it.toDomain() } ?: emptyList()
                AppResult.Success(members)
            } else {
                AppResult.Error("Error al obtener miembros: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun removeHouseholdMember(householdId: String, userId: String): AppResult<Boolean> {
        return try {
            val response = authApi.removeHouseholdMember(
                mapOf("p_household_id" to householdId, "p_user_id" to userId)
            )
            if (response.isSuccessful) {
                AppResult.Success(true)
            } else {
                val error = response.errorBody()?.string() ?: "Error desconocido"
                AppResult.Error(
                    if (error.contains("administrador", ignoreCase = true))
                        "Solo el administrador puede eliminar miembros"
                    else "Error al eliminar miembro: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al eliminar miembro")
        }
    }

    // ── Solicitudes pendientes ────────────────────────────────────────────────

    override suspend fun getPendingMembers(householdId: String): AppResult<List<HouseholdMember>> {
        return try {
            val response = authApi.getPendingMembers(
                mapOf("p_household_id" to householdId)
            )
            if (response.isSuccessful) {
                val members = response.body()?.map { it.toDomain() } ?: emptyList()
                AppResult.Success(members)
            } else {
                AppResult.Error("Error al obtener solicitudes: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun acceptMember(memberId: String): AppResult<Boolean> {
        return try {
            val response = authApi.acceptMember(mapOf("p_member_id" to memberId))
            if (response.isSuccessful) {
                AppResult.Success(true)
            } else {
                AppResult.Error("Error al aceptar miembro: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al aceptar miembro")
        }
    }

    override suspend fun rejectMember(memberId: String): AppResult<Boolean> {
        return try {
            val response = authApi.rejectMember(mapOf("p_member_id" to memberId))
            if (response.isSuccessful) {
                AppResult.Success(true)
            } else {
                AppResult.Error("Error al rechazar miembro: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al rechazar miembro")
        }
    }
}
