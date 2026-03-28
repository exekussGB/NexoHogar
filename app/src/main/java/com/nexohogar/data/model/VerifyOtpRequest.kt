package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    val email: String,
    val token: String,
    val type: String = "recovery"
)