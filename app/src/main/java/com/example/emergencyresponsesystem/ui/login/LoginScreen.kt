package com.example.emergencyresponsesystem.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencyresponsesystem.data.EmergencyRepository
import com.example.emergencyresponsesystem.model.UserAccount
import com.example.emergencyresponsesystem.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole, String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.REPORTER) }

    // Form fields
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var securityPasscode by remember { mutableStateOf("") }
    var responderType by remember { mutableStateOf("Paramedic") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val responderTypes = listOf("Paramedic", "Firefighter", "Police Officer")

    val ADMIN_BUILTIN_PASSWORD = "ADMIN999"
    val RESPONDER_REGISTRATION_PASSCODE = "RESPONDER911"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2C1E23),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1200f
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emergency Icon Header
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "EMERGENCY DISPATCH",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRegisterMode) "Register New Responder / Resident" else "Secure Command Portal",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Mode Tab selector (Login vs Register)
                if (selectedRole != UserRole.ADMIN) {
                    TabRow(
                        selectedTabIndex = if (isRegisterMode) 1 else 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        indicator = {}
                    ) {
                        Tab(
                            selected = !isRegisterMode,
                            onClick = { isRegisterMode = false; errorMessage = null },
                            text = { Text("Login", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = isRegisterMode,
                            onClick = { 
                                isRegisterMode = true; 
                                errorMessage = null
                                if (selectedRole == UserRole.ADMIN) {
                                    selectedRole = UserRole.REPORTER
                                }
                            },
                            text = { Text("Register", fontWeight = FontWeight.Bold) }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    isRegisterMode = false
                }

                // Role selector
                Text(
                    text = "SELECT ACCESS PORTAL", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == UserRole.REPORTER,
                        onClick = { selectedRole = UserRole.REPORTER; securityPasscode = ""; errorMessage = null },
                        label = { Text("Reporter", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRole == UserRole.RESPONDER,
                        onClick = { selectedRole = UserRole.RESPONDER; securityPasscode = ""; errorMessage = null },
                        label = { Text("Responder", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    if (!isRegisterMode) {
                        FilterChip(
                            selected = selectedRole == UserRole.ADMIN,
                            onClick = { selectedRole = UserRole.ADMIN; securityPasscode = ""; errorMessage = null },
                            label = { Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (selectedRole == UserRole.ADMIN) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Admin Built-In Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Phone Number (09XXXXXXXXX)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                if (isRegisterMode && selectedRole == UserRole.RESPONDER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = securityPasscode,
                        onValueChange = { securityPasscode = it },
                        label = { Text("Responder Built-In Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "RESPONDER UNIT TYPE:", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        responderTypes.forEach { type ->
                            FilterChip(
                                selected = responderType == type,
                                onClick = { responderType = type },
                                label = { Text(type, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (selectedRole == UserRole.ADMIN) {
                            if (password == ADMIN_BUILTIN_PASSWORD) {
                                onLoginSuccess(UserRole.ADMIN, "Chief Administrator")
                            } else {
                                errorMessage = "Invalid Admin Password! (Hint: ADMIN999)"
                            }
                            return@Button
                        }

                        val phoneInput = username.trim()
                        if (!phoneInput.startsWith("09") || phoneInput.length != 11 || !phoneInput.all { it.isDigit() }) {
                            errorMessage = "Phone number must start with '09' and be exactly 11 digits."
                            return@Button
                        }

                        if (password.isBlank() || (isRegisterMode && fullName.isBlank())) {
                            errorMessage = "Please fill in all required fields."
                            return@Button
                        }

                        if (isRegisterMode) {
                            if (selectedRole == UserRole.RESPONDER && securityPasscode != RESPONDER_REGISTRATION_PASSCODE) {
                                errorMessage = "Invalid Responder Passcode! (Hint: RESPONDER911)"
                                return@Button
                            }

                            val newAccount = UserAccount(
                                username = phoneInput,
                                password = password,
                                role = selectedRole,
                                fullName = fullName.trim(),
                                responderType = if (selectedRole == UserRole.RESPONDER) responderType else null
                            )
                            val success = EmergencyRepository.registerUser(newAccount)
                            if (success) {
                                onLoginSuccess(selectedRole, fullName.trim())
                            } else {
                                errorMessage = "Phone number already registered."
                            }
                        } else {
                            val account = EmergencyRepository.loginUser(phoneInput, password)
                            if (account != null) {
                                onLoginSuccess(account.role, account.fullName)
                            } else {
                                errorMessage = "Invalid phone number or password. (Tip: 09175550101 / password)"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isRegisterMode) "COMPLETE REGISTRATION" else "SECURE LOGIN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
