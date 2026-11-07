package com.example.emergencyresponsesystem.model

import java.util.UUID

enum class UserRole {
    NONE, ADMIN, RESPONDER, REPORTER
}

enum class IncidentStatus {
    PENDING, ASSIGNED, EN_ROUTE, ARRIVED, RESOLVED
}

data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String, // Medical, Fire, Police, Accident, Disaster
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val photoUri: String? = null,
    val reporterName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: IncidentStatus = IncidentStatus.PENDING,
    val assignedResponder: String? = null,
    val assignedVehicle: String? = null,
    val responderLatitude: Double? = null,
    val responderLongitude: Double? = null
)

data class Responder(
    val id: String,
    val name: String,
    val role: String, // Paramedic, Firefighter, Police Officer
    val phone: String,
    val isAvailable: Boolean = true,
    val currentLatitude: Double = 14.5995,
    val currentLongitude: Double = 120.9842
)

data class Vehicle(
    val id: String,
    val name: String, // Ambulance Alpha, Firetruck Beta, Patrol Unit 1
    val type: String, // Ambulance, Fire Engine, Police Cruiser
    val licensePlate: String,
    val isAvailable: Boolean = true
)

data class UserAccount(
    val username: String,
    val password: String,
    val role: UserRole,
    val fullName: String,
    val responderType: String? = null
)

