package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.InviteCodeRequest
import com.nexohogar.data.remote.dto.JoinHouseholdRequest
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.model.HouseholdMember
import com.nexohogar.domain.repository.HouseholdRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage

class HouseholdRepositoryImpl(
    private val authApi: AuthApi,
    private val supabaseClient: SupabaseClient
) : HouseholdRepository {

    override suspend fun getHouseholds(): AppResult<List<Household>> {
        return try {
            val response = authApi.getHouseholds()
            if (response.isSuccessful) {
                val households = response.body()?.map { dto ->
                    Household(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        imageUri = dto.imageUrl,
                        gradientIndex = dto.gradientIndex ?: 0
                    )
                } ?: emptyList()
                AppResult.Success(households)
            } else {
                val msg = response.message()
                if (response.code() == 401 && msg.contains("session expired", ignoreCase = true)) {
                    AppResult.Error("Unauthorized - session expired")
                } else {
                    AppResult.Error("Error al obtener hogares: ${response.code()}")
                }
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
                            id = body.id,
                            name = body.name,
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

    override suspend fun regenerateInviteCode(householdId: String): AppResult<String> {
        return try {
            val response = authApi.regenerateInviteCode(InviteCodeRequest(householdId))
            if (response.isSuccessful) {
                val code = response.body()
                if (!code.isNullOrBlank()) {
                    AppResult.Success(code.trim('"'))
                } else {
                    AppResult.Error("No se recibió código de invitación")
                }
            } else {
                AppResult.Error("Error al regenerar código: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun joinHouseholdByCode(inviteCode: String): AppResult<Boolean> {
        return try {
            val response = authApi.joinHouseholdByCode(
                JoinHouseholdRequest(inviteCode.trim().uppercase())
            )
            if (response.isSuccessful) {
                val body = response.body()
                val success = body?.get("success")?.asBoolean ?: true
                if (success) {
                    AppResult.Success(true)
                } else {
                    val msg = body?.get("message")?.asString ?: "Error al unirse"
                    AppResult.Error(msg)
                }
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

    override suspend fun acceptMember(memberId: String): AppResult<Boolean> {
        return try {
            val response = authApi.acceptMember(mapOf("p_member_id" to memberId))
            if (response.isSuccessful) {
                AppResult.Success(true)
            } else {
                AppResult.Error("Error al aceptar miembro: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
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
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun removeMember(
        householdId: String,
        memberUserId: String
    ): AppResult<Boolean> {
        return try {
            val response = authApi.removeMember(
                mapOf(
                    "p_household_id" to householdId,
                    "p_member_user_id" to memberUserId
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                val success = body?.get("success")?.asBoolean ?: true
                if (success) {
                    AppResult.Success(true)
                } else {
                    AppResult.Error("No se pudo eliminar al miembro")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMsg = when {
                    response.code() == 403 || errorBody.contains("administrador", ignoreCase = true) ->
                        "Solo el administrador puede eliminar miembros"
                    errorBody.contains("ti mismo", ignoreCase = true) ->
                        "No puedes eliminarte a ti mismo"
                    errorBody.contains("no encontrado", ignoreCase = true) ->
                        "Miembro no encontrado"
                    else -> "Error al eliminar miembro: ${response.code()}"
                }
                AppResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    override suspend fun deleteHousehold(
        householdId: String,
        confirmName: String
    ): AppResult<Boolean> {
        return try {
            val response = authApi.deleteHousehold(
                mapOf(
                    "p_household_id" to householdId,
                    "p_confirm_name" to confirmName
                )
            )
            if (response.isSuccessful) {
                AppResult.Success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = when (response.code()) {
                    403 -> "No tienes permisos para eliminar este hogar"
                    400 -> "El nombre ingresado no coincide con el hogar"
                    else -> errorBody ?: "Error al eliminar hogar: ${response.code()}"
                }
                AppResult.Error(message)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ── Actualizar apariencia (gradiente y/o imagen URL) ────────────────
    override suspend fun updateHouseholdAppearance(
        householdId: String,
        imageUrl: String?,
        gradientIndex: Int?
    ): AppResult<Boolean> {
        return try {
            val params = mutableMapOf<String, Any?>(
                "p_household_id" to householdId
            )
            if (imageUrl != null) params["p_image_url"] = imageUrl
            if (gradientIndex != null) params["p_gradient_index"] = gradientIndex

            val response = authApi.updateHouseholdAppearance(params)
            if (response.isSuccessful) {
                val body = response.body()
                val success = body?.get("success")?.asBoolean ?: false
                if (success) AppResult.Success(true)
                else AppResult.Error(body?.get("error")?.asString ?: "Error desconocido")
            } else {
                AppResult.Error("Error al actualizar apariencia: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ── Subir imagen a Supabase Storage ─────────────────────────────────
    override suspend fun uploadHouseholdImage(
        householdId: String,
        imageBytes: ByteArray,
        mimeType: String
    ): AppResult<String> {
        return try {
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val fileName = "$householdId/${System.currentTimeMillis()}.$extension"
            val bucket = supabaseClient.storage.from("household-images")

            bucket.upload(fileName, imageBytes) { upsert = true }

            val publicUrl = bucket.publicUrl(fileName)
            AppResult.Success(publicUrl)
        } catch (e: Exception) {
            AppResult.Error("Error al subir imagen: ${e.message}")
        }
    }
}
