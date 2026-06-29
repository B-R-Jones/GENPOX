package com.example.genpox.data

import kotlinx.serialization.Serializable

@Serializable
data class MailMessage(
    val id: String,
    val from: String,
    val subject: String,
    val timestamp: String,
    val body: String,
    val isRead: Boolean = false,
    val attachedCreatureDna: String? = null,
    val attachedCreatureName: String? = null,
    val attachedCreatureFaction: String? = null,
    val attachedGeneSequence: String? = null,
    val transferGenes: Int = 0,
    val transferWaste: Int = 0,
    val isClaimed: Boolean = false
)
