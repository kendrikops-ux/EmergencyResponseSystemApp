package com.example.emergencyresponsesystem.data

import com.example.emergencyresponsesystem.model.Incident
import com.example.emergencyresponsesystem.model.IncidentStatus
import com.example.emergencyresponsesystem.model.Responder
import com.example.emergencyresponsesystem.model.Vehicle
import com.example.emergencyresponsesystem.model.UserAccount
import com.example.emergencyresponsesystem.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object EmergencyRepository {

    private val _incidents = MutableStateFlow<List<Incident>>(
        listOf(
            Incident(
                id = "inc-101",
                title = "Car Accident on Main St",
                description = "Two vehicles collided, potential injuries reported.",
                category = "Accident",
                latitude = 14.5995,
                longitude = 120.9842,
                address = "Main Street, Sector 4",
                reporterName = "Juan Dela Cruz",
                timestamp = System.currentTimeMillis() - 300000,
                status = IncidentStatus.PENDING
            )
        )
    )
    val incidents: StateFlow<List<Incident>> = _incidents.asStateFlow()

    private val _responders = MutableStateFlow<List<Responder>>(
        listOf(
            Responder("res-1", "Dr. Alex Santos", "Paramedic", "0917-555-0101", true, 14.5900, 120.9800),
            Responder("res-2", "Capt. Marco Reyes", "Firefighter", "0918-555-0202", true, 14.6000, 120.9900),
            Responder("res-3", "Officer Elena Cruz", "Police Officer", "0919-555-0303", true, 14.5850, 120.9750)
        )
    )
    val responders: StateFlow<List<Responder>> = _responders.asStateFlow()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(
        listOf(
            Vehicle("veh-1", "Ambulance Unit 1", "Ambulance", "ABC-1234", true),
            Vehicle("veh-2", "Fire Engine 5", "Fire Truck", "XYZ-9876", true),
            Vehicle("veh-3", "Patrol Cruiser 12", "Police Car", "PNP-4321", true)
        )
    )
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    // Pre-registered accounts
    private val _users = MutableStateFlow<MutableList<UserAccount>>(
        mutableListOf(
            UserAccount("admin", "admin123", UserRole.ADMIN, "Chief Admin"),
            UserAccount("09175550101", "password", UserRole.RESPONDER, "Dr. Alex Santos", "Paramedic"),
            UserAccount("09185550202", "password", UserRole.RESPONDER, "Capt. Marco Reyes", "Firefighter"),
            UserAccount("09195550303", "password", UserRole.RESPONDER, "Officer Elena Cruz", "Police Officer"),
            UserAccount("09171234567", "password", UserRole.REPORTER, "Juan Dela Cruz")
        )
    )
    val users: StateFlow<List<UserAccount>> = _users.asStateFlow()

    // Trigger for responder sound notification when assigned
    private val _assignedNotificationEvent = MutableStateFlow<Pair<String, Incident>?>(null)
    val assignedNotificationEvent: StateFlow<Pair<String, Incident>?> = _assignedNotificationEvent.asStateFlow()

    fun registerUser(account: UserAccount): Boolean {
        val existing = _users.value.find { it.username.lowercase() == account.username.lowercase() }
        if (existing != null) return false // Username taken

        _users.update { list ->
            val mutable = list.toMutableList()
            mutable.add(account)
            mutable
        }

        // If registered as responder, also add to responders list
        if (account.role == UserRole.RESPONDER) {
            _responders.update { list ->
                if (list.none { it.name == account.fullName }) {
                    list + Responder(
                        id = "res-${System.currentTimeMillis()}",
                        name = account.fullName,
                        role = account.responderType ?: "Paramedic",
                        phone = "0917-000-0000"
                    )
                } else list
            }
        }
        return true
    }

    fun loginUser(username: String, pass: String): UserAccount? {
        return _users.value.find { 
            it.username.equals(username, ignoreCase = true) && it.password == pass 
        }
    }

    fun reportIncident(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        address: String,
        photoUri: String?,
        reporterName: String
    ): Incident {
        val newIncident = Incident(
            title = title,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            address = address,
            photoUri = photoUri,
            reporterName = reporterName,
            status = IncidentStatus.PENDING
        )
        _incidents.update { listOf(newIncident) + it }
        return newIncident
    }

    fun assignIncident(incidentId: String, responderName: String, vehicleName: String) {
        _incidents.update { list ->
            list.map { inc ->
                if (inc.id == incidentId) {
                    val updated = inc.copy(
                        status = IncidentStatus.ASSIGNED,
                        assignedResponder = responderName,
                        assignedVehicle = vehicleName
                    )
                    _assignedNotificationEvent.value = Pair(responderName, updated)
                    updated
                } else inc
            }
        }
    }

    fun updateIncidentStatus(incidentId: String, newStatus: IncidentStatus) {
        _incidents.update { list ->
            list.map { inc ->
                if (inc.id == incidentId) {
                    inc.copy(status = newStatus)
                } else inc
            }
        }
    }

    fun updateResponderLiveLocation(responderName: String, lat: Double, lng: Double) {
        _responders.update { list ->
            list.map { res ->
                if (res.name == responderName) {
                    res.copy(currentLatitude = lat, currentLongitude = lng)
                } else res
            }
        }
        _incidents.update { list ->
            list.map { inc ->
                if (inc.assignedResponder == responderName && inc.status != IncidentStatus.RESOLVED) {
                    inc.copy(responderLatitude = lat, responderLongitude = lng)
                } else inc
            }
        }
    }

    fun clearNotificationEvent() {
        _assignedNotificationEvent.value = null
    }
}
