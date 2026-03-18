package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MonthlyBalanceDto(
    @SerializedName("year_num")  val yearNum: Int,
    @SerializedName("month_num") val monthNum: Int,
    @SerializedName("income")    val income: Long,
    @SerializedName("expense")   val expense: Long,
    @SerializedName("net")       val net: Long
)
