package com.example.emergencyresponsesystem.ui.responder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencyresponsesystem.data.EmergencyRepository
import com.example.emergencyresponsesystem.model.Incident
import com.example.emergencyresponsesystem.model.IncidentStatus
import com.example.emergencyresponsesystem.utils.IncidentPhotoThumbnail
import com.example.emergencyresponsesystem.utils.SoundUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponderDashboard(
    responderName: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val incidents by EmergencyRepository.incidents.collectAsState()
    val responders by EmergencyRepository.responders.collectAsState()
    val notificationEvent by EmergencyRepository.assignedNotificationEvent.collectAsState()

    var activeNotificationBanner by remember { mutableStateOf<Incident?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: History

    val currentResponder = responders.find { it.name.equals(responderName, ignoreCase = true) }
    val responderRole = currentResponder?.role ?: "Responder"

    // Listen for assignment notifications targeting this responder
    LaunchedEffect(notificationEvent) {
        notificationEvent?.let { (targetResponder, incident) ->
            if (targetResponder == responderName) {
                activeNotificationBanner = incident
                SoundUtils.playEmergencyAlert(context)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            SoundUtils.stopEmergencyAlert()
        }
    }

    val myAssignedIncidents = incidents.filter { it.assignedResponder == responderName && it.status != IncidentStatus.RESOLVED }
    val historyIncidents = incidents.filter { it.assignedResponder == responderName && it.status == IncidentStatus.RESOLVED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚑 $responderName - $responderRole") },
                actions = {
                    IconButton(onClick = {
                        SoundUtils.stopEmergencyAlert()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Notification Banner if newly assigned
            activeNotificationBanner?.let { inc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🚨 NEW EMERGENCY ASSIGNED!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(inc.title, fontWeight = FontWeight.Bold)
                        Text("Location: ${inc.address}")
                        Text("Vehicle Assigned: ${inc.assignedVehicle}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                SoundUtils.stopEmergencyAlert()
                                EmergencyRepository.updateIncidentStatus(inc.id, IncidentStatus.EN_ROUTE)
                                activeNotificationBanner = null
                                EmergencyRepository.clearNotificationEvent()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("ACKNOWLEDGE & EN ROUTE")
                        }
                    }
                }
            }

            // Tab navigation for Active Dispatches vs History
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Active Dispatches (${myAssignedIncidents.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Dispatch History (${historyIncidents.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentList = if (selectedTab == 0) myAssignedIncidents else historyIncidents

            if (currentList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (selectedTab == 0) "No active assignments currently. Standby for dispatch."
                            else "No completed dispatch history yet."
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList) { incident ->
                        ResponderIncidentCard(incident = incident, responderName = responderName)
                    }
                }
            }
        }
    }
}

@Composable
fun ResponderIncidentCard(
    incident: Incident,
    responderName: String
) {
    val isResolved = incident.status == IncidentStatus.RESOLVED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isResolved) Color(0xFF121212) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = incident.category.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isResolved) Color(0xFF90CAF9) else MaterialTheme.colorScheme.secondary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isResolved) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isResolved) "✅ COMPLETED" else incident.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isResolved) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = incident.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isResolved) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = incident.description,
                fontSize = 14.sp,
                color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = if (isResolved) Color.DarkGray else MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "📍 Location Address: ${incident.address}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isResolved) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "🚐 Vehicle Assigned: ${incident.assignedVehicle}",
                fontSize = 13.sp,
                color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "👤 Reporter: ${incident.reporterName}",
                fontSize = 13.sp,
                color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface
            )

            // PHOTO PROVIDED BY REPORTER
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "📸 Photo Provided by Reporter:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isResolved) Color(0xFF90CAF9) else MaterialTheme.colorScheme.primary
            )
            IncidentPhotoThumbnail(
                photoUri = incident.photoUri,
                height = 140
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Google Maps Style Map Card for Responder to see Reporter Location
            Text(
                text = "🗺️ GPS Location Map:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isResolved) Color(0xFF90CAF9) else Color(0xFF1976D2)
            )
            ResponderMapVisualizerCard(
                address = incident.address,
                latitude = incident.latitude,
                longitude = incident.longitude
            )

            if (!isResolved) {
                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons to update status and simulate GPS movement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (incident.status == IncidentStatus.ASSIGNED) {
                        Button(
                            onClick = {
                                SoundUtils.stopEmergencyAlert()
                                EmergencyRepository.updateIncidentStatus(incident.id, IncidentStatus.EN_ROUTE)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("En Route")
                        }
                    }
                    if (incident.status == IncidentStatus.EN_ROUTE) {
                        Button(
                            onClick = {
                                EmergencyRepository.updateIncidentStatus(incident.id, IncidentStatus.ARRIVED)
                                EmergencyRepository.updateResponderLiveLocation(responderName, incident.latitude, incident.longitude)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Arrived")
                        }
                    }
                    if (incident.status == IncidentStatus.ARRIVED) {
                        Button(
                            onClick = {
                                EmergencyRepository.updateIncidentStatus(incident.id, IncidentStatus.RESOLVED)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Resolve Incident")
                        }
                    }

                    // Simulate GPS broadcast button
                    OutlinedButton(
                        onClick = {
                            val newLat = incident.latitude + (Math.random() - 0.5) * 0.002
                            val newLng = incident.longitude + (Math.random() - 0.5) * 0.002
                            EmergencyRepository.updateResponderLiveLocation(responderName, newLat, newLng)
                        }
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "Ping GPS", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ResponderMapVisualizerCard(
    address: String,
    latitude: Double,
    longitude: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                drawRect(Color(0xFFF5F5F3))

                val roadColor = Color(0xFFFFFFFF)
                val highwayColor = Color(0xFFFFCC80)

                for (i in 1..4) {
                    val y = height * i / 5f
                    drawLine(roadColor, Offset(0f, y), Offset(width, y), 6f)
                }
                for (j in 1..5) {
                    val x = width * j / 6f
                    drawLine(roadColor, Offset(x, 0f), Offset(x, height), 6f)
                }

                drawLine(highwayColor, Offset(0f, height * 0.7f), Offset(width, height * 0.3f), 12f)
            }

            // Center Red Pin
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Reporter Location",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(38.dp)
                )
            }

            // Top Header Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REPORTER MAP LOCATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }

            // Bottom Address Footer
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("📍 Location: $address", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                    Text("GPS: ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}
