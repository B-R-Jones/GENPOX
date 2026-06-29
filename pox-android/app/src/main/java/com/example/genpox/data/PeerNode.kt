package com.example.genpox.data

import kotlinx.serialization.Serializable

@Serializable
data class PeerNode(
    val id: String,
    val name: String,
    val sector: String,
    val latency: String,
    val status: String
)
