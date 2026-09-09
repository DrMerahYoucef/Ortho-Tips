package com.example.data.local.model

import kotlinx.serialization.Serializable

@Serializable
data class Cim10Disease(
    val code: String,
    val maladie: String
)