package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName
import java.time.Instant

data class SoftDeleteAccountRequest(
    @SerializedName("is_deleted")
    val isDeleted: Boolean = true,

    @SerializedName("deleted_at")
    val deletedAt: String = Instant.now().toString()
) {
}