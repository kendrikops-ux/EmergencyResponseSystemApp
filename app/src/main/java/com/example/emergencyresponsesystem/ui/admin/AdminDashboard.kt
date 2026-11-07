package com.example.emergencyresponsesystem.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencyresponsesystem.data.EmergencyRepository
import com.example.emergencyresponsesystem.model.Incident
import com.example.emergencyresponsesystem.model.IncidentStatus
import com.example.emergencyresponsesystem.model.UserAccount
import com.example.emergencyresponsesystem.model.UserRole
import com.example.emergencyresponsesystem.utils.IncidentPhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    adminName: String,
    onLogout: () -> Unit
) {
    val incidents by EmergencyRepository.incidents.collectAsState()
    val responders by EmergencyRepository.responders.collectAsState()
    val vehicles by EmergencyRepository.vehicles.collectAsState()
    val users by EmergencyRepository.users.collectAsState()

    var selectedIncidentForAssignment by remember { mutableStateOf<Incident?>(null) }
    var chosenResponder by remember { mutableStateOf("") }
    var chosenVehicle by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Finished
    var showRegisteredUsersDialog by remember { mutableStateOf(false) }

    val activeIncidents = incidents.filter { it.status != IncidentStatus.RESOLVED && it.status != IncidentStatus.ARRIVED }
    val finishedIncidents = incidents.filter { it.status == IncidentStatus.RESOLVED || it.status == IncidentStatus.ARRIVED }
    val displayedIncidents = if (selectedTab == 0) activeIncidents else finishedIncidents

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛡️ Admin Command ($adminName)") },
                actions = {
                    // Button to view registered users directory
                    IconButton(onClick = { showRegisteredUsersDialog = true }) {
                        Icon(Icons.Default.People, contentDescription = "View Registered Users")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            // Summary telemetry row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${incidents.count { it.status == IncidentStatus.PENDING }}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${activeIncidents.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Finished", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "${finishedIncidents.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs for Active vs Finished Reports
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
                    text = { Text("Active Dispatches (${activeIncidents.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Finished Reports (${finishedIncidents.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (displayedIncidents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedTab == 0) "No active or pending dispatches." else "No finished reports yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedIncidents) { incident ->
                        IncidentAdminCard(
                            incident = incident,
                            onAssignClick = {
                                selectedIncidentForAssignment = incident
                                val firstResp = responders.firstOrNull()?.name ?: ""
                                val firstVeh = vehicles.firstOrNull()?.name ?: ""
                                chosenResponder = firstResp
                                chosenVehicle = firstVeh
                            }
                        )
                    }
                }
            }
        }

        // Assignment Dialog - Combined Responder & Vehicle
        if (selectedIncidentForAssignment != null) {
            val inc = selectedIncidentForAssignment!!
            AlertDialog(
                onDismissRequest = { selectedIncidentForAssignment = null },
                title = { Text("Assign Responder & Vehicle Unit") },
                text = {
                    Column {
                        Text("Incident: ${inc.title}", fontWeight = FontWeight.Bold)
                        Text("Location: ${inc.address}")
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Select Dispatch Unit (Responder & Vehicle):", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))

                        responders.forEachIndexed { index, resp ->
                            val veh = vehicles.getOrElse(index) { vehicles.first() }
                            val isSelected = chosenResponder == resp.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        chosenResponder = resp.name
                                        chosenVehicle = veh.name
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${resp.name} (${resp.role} - ${veh.name})",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (chosenResponder.isNotBlank() && chosenVehicle.isNotBlank()) {
                            EmergencyRepository.assignIncident(inc.id, chosenResponder, chosenVehicle)
                            selectedIncidentForAssignment = null
                        }
                    }) {
                        Text("Confirm & Dispatch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedIncidentForAssignment = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Registered Users Directory Dialog - Separating Responders and Residents
        if (showRegisteredUsersDialog) {
            val respondersList = users.filter { it.role == UserRole.RESPONDER }
            val reportersList = users.filter { it.role == UserRole.REPORTER }
            val adminsList = users.filter { it.role == UserRole.ADMIN }

            AlertDialog(
                onDismissRequest = { showRegisteredUsersDialog = false },
                title = { Text("👥 Registered Users Directory") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Total Accounts: ${users.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Responders Section
                            item {
                                Text("🚨 Responders (${respondersList.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            items(respondersList) { user ->
                                UserCardItem(user = user)
                            }

                            // Residents / Reporters Section
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("👤 Residents / Reporters (${reportersList.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                            }
                            items(reportersList) { user ->
                                UserCardItem(user = user)
                            }

                            // Admins Section
                            if (adminsList.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("🛡️ Administrators (${adminsList.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                                }
                                items(adminsList) { user ->
                                    UserCardItem(user = user)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showRegisteredUsersDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun UserCardItem(user: UserAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            val roleDesc = when (user.role) {
                UserRole.RESPONDER -> "Responder (${user.responderType ?: "Unit"})"
                UserRole.REPORTER -> "Resident / Reporter"
                UserRole.ADMIN -> "Administrator"
                else -> user.role.name
            }
            Text("Role: $roleDesc", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Phone: ${user.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun IncidentAdminCard(
    incident: Incident,
    onAssignClick: () -> Unit
) {
    val isResolved = incident.status == IncidentStatus.RESOLVED || incident.status == IncidentStatus.ARRIVED

    val statusColor = when (incident.status) {
        IncidentStatus.PENDING -> MaterialTheme.colorScheme.error
        IncidentStatus.ASSIGNED -> MaterialTheme.colorScheme.tertiary
        IncidentStatus.EN_ROUTE -> MaterialTheme.colorScheme.primary
        IncidentStatus.ARRIVED -> MaterialTheme.colorScheme.secondary
        IncidentStatus.RESOLVED -> Color(0xFF81C784)
    }

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
                .padding(16.dp)
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
                    color = if (isResolved) Color(0xFF90CAF9) else MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isResolved) Color(0xFF1B5E20) else statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = incident.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isResolved) Color.White else statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = incident.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isResolved) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = incident.description,
                fontSize = 14.sp,
                color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if (isResolved) Color.DarkGray else MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("📍 ${incident.address}", fontSize = 12.sp, color = if (isResolved) Color.White else MaterialTheme.colorScheme.onSurface)
                    Text("👤 Reporter: ${incident.reporterName}", fontSize = 12.sp, color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface)
                    if (incident.assignedResponder != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("🚑 Responder: ${incident.assignedResponder}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isResolved) Color(0xFF81C784) else MaterialTheme.colorScheme.primary)
                        Text("🚐 Vehicle: ${incident.assignedVehicle}", fontSize = 12.sp, color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (incident.status == IncidentStatus.PENDING) {
                    Button(
                        onClick = onAssignClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Assign")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            IncidentPhotoThumbnail(
                photoUri = incident.photoUri,
                height = 100
            )
        }
    }
}
