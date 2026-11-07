package com.example.emergencyresponsesystem.ui.reporter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.emergencyresponsesystem.data.EmergencyRepository
import com.example.emergencyresponsesystem.model.Incident
import com.example.emergencyresponsesystem.model.IncidentStatus
import com.example.emergencyresponsesystem.utils.IncidentPhotoThumbnail
import com.example.emergencyresponsesystem.utils.PhotoUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporterDashboard(
    reporterName: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val incidents by EmergencyRepository.incidents.collectAsState()
    val myIncidents = incidents.filter { it.reporterName == reporterName }

    val myActiveIncidents = myIncidents.filter { it.status != IncidentStatus.RESOLVED }
    val myHistoryIncidents = myIncidents.filter { it.status == IncidentStatus.RESOLVED }

    var selectedTab by remember { mutableStateOf(0) } // 0: Active Reports, 1: Report History

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Medical") }
    
    var address by remember { mutableStateOf("Locating exact device GPS...") }
    var latitude by remember { mutableStateOf(14.5995) }
    var longitude by remember { mutableStateOf(120.9842) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val categories = listOf("Medical", "Fire", "Police", "Accident", "Disaster")

    // Function to strictly fetch the user's actual device location via LocationManager & Geocoder
    val fetchDeviceLocation = {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
            val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

            val location: Location? = try {
                if (isGpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            } catch (_: SecurityException) {
                null
            } ?: try {
                if (isNetworkEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            } catch (_: SecurityException) {
                null
            }

            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude

                var resolvedAddress: String? = null
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Geocoder.isPresent()) {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            resolvedAddress = addresses[0].getAddressLine(0)
                        }
                    }
                } catch (_: Exception) {}

                address = resolvedAddress ?: "Device GPS Location (${String.format(Locale.US, "%.5f, %.5f", latitude, longitude)})"
            } else {
                address = "Waiting for Device GPS Lock... (Check location settings)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            address = "Device GPS Error: Unable to acquire location fix."
        }
    }

    // Runtime Permission Launcher (Popup Allow / Deny)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            fetchDeviceLocation()
        } else {
            address = "Location Permission Denied. Tap 'Grant GPS' to allow."
        }
    }

    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
            fetchDeviceLocation()
        } else {
            // Triggers the system popup dialog (Allow / Deny)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Photo picker launcher (Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    // Camera Capture launcher
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedPath = PhotoUtils.saveBitmapToCache(context, bitmap)
            photoUri = savedPath ?: "content://mock/captured_emergency_photo.jpg"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 Resident Portal ($reporterName)") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showReportDialog = true },
                icon = { Icon(Icons.Default.Report, contentDescription = null) },
                text = { Text("REPORT EMERGENCY") },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Google Maps Style Card with Runtime Permission & Real Device Location
            GoogleMapsStyleCard(
                address = address,
                hasPermission = hasLocationPermission,
                onRecalibrate = {
                    if (hasLocationPermission) {
                        fetchDeviceLocation()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            )

            // Navigation Tabs: Active Reports vs Report History
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
                            "Active Reports (${myActiveIncidents.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Report History (${myHistoryIncidents.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            val currentList = if (selectedTab == 0) myActiveIncidents else myHistoryIncidents

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selectedTab == 0) "No active emergency reports currently submitted."
                            else "No completed report history available yet."
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList) { incident ->
                        ReporterIncidentCard(incident = incident)
                    }
                }
            }
        }

        // Report Dialog with Capture & Real Device Address
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("🚨 Report Emergency") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Incident Title (e.g. House Fire)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            categories.take(3).forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description & Details") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Exact Device GPS Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Photo Capture & Gallery Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraCaptureLauncher.launch(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Capture Photo")
                            }

                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery")
                            }
                        }

                        if (photoUri != null) {
                            Text("✓ Incident Photo Attached Successfully", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                EmergencyRepository.reportIncident(
                                    title = title,
                                    description = description,
                                    category = category,
                                    latitude = latitude,
                                    longitude = longitude,
                                    address = address,
                                    photoUri = photoUri,
                                    reporterName = reporterName
                                )
                                title = ""
                                description = ""
                                photoUri = null
                                showReportDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("SUBMIT REPORT")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GoogleMapsStyleCard(
    address: String,
    hasPermission: Boolean,
    onRecalibrate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
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
                    contentDescription = "Map Pin",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(42.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = address,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onRecalibrate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "GPS Lock", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasPermission) "Real Device GPS Pin" else "Location Permission Required", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (hasPermission) Color(0xFF1976D2) else Color.Red)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = address,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 2
                        )
                    }

                    Button(
                        onClick = onRecalibrate,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasPermission) Color(0xFF1976D2) else Color.Red)
                    ) {
                        Text(if (hasPermission) "Update GPS" else "Grant GPS", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReporterIncidentCard(incident: Incident) {
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
                    color = if (isResolved) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isResolved) "✅ RESOLVED" else incident.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isResolved) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
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
            Text(
                text = incident.description,
                fontSize = 14.sp,
                color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if (isResolved) Color.DarkGray else MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "📍 Location Address: ${incident.address}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isResolved) Color.White else MaterialTheme.colorScheme.onSurface
            )

            // Attached Incident Photo Thumbnail
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📸 Attached Photo:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isResolved) Color(0xFF90CAF9) else MaterialTheme.colorScheme.primary
            )
            IncidentPhotoThumbnail(
                photoUri = incident.photoUri,
                height = 120
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (incident.assignedResponder != null) {
                Text(
                    text = "🚑 Dispatched Responder: ${incident.assignedResponder}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isResolved) Color(0xFF81C784) else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "🚐 Assigned Vehicle: ${incident.assignedVehicle}",
                    fontSize = 13.sp,
                    color = if (isResolved) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.onSurface
                )
                if (incident.responderLatitude != null && !isResolved) {
                    Text("📡 Live Responder GPS: (${String.format(Locale.US, "%.4f", incident.responderLatitude)}, ${String.format(Locale.US, "%.4f", incident.responderLongitude)})", fontSize = 12.sp, color = Color(0xFF4CAF50))
                }
            } else if (!isResolved) {
                Text("⏳ Status: Awaiting Admin assignment...", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
