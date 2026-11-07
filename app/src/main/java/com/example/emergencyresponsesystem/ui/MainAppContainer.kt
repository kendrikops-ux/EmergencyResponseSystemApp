package com.example.emergencyresponsesystem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.emergencyresponsesystem.model.UserRole
import com.example.emergencyresponsesystem.ui.admin.AdminDashboard
import com.example.emergencyresponsesystem.ui.login.LoginScreen
import com.example.emergencyresponsesystem.ui.reporter.ReporterDashboard
import com.example.emergencyresponsesystem.ui.responder.ResponderDashboard

@Composable
fun MainAppContainer() {
    var currentRole by remember { mutableStateOf(UserRole.NONE) }
    var currentUserName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentRole) {
            UserRole.NONE -> {
                LoginScreen(
                    onLoginSuccess = { role, name ->
                        currentRole = role
                        currentUserName = name
                    }
                )
            }
            UserRole.ADMIN -> {
                AdminDashboard(
                    adminName = currentUserName,
                    onLogout = { currentRole = UserRole.NONE }
                )
            }
            UserRole.RESPONDER -> {
                ResponderDashboard(
                    responderName = currentUserName,
                    onLogout = { currentRole = UserRole.NONE }
                )
            }
            UserRole.REPORTER -> {
                ReporterDashboard(
                    reporterName = currentUserName,
                    onLogout = { currentRole = UserRole.NONE }
                )
            }
        }
    }
}
