package com.organiza.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.organiza.app.OrganizaViewModel
import com.organiza.app.ui.screens.*

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    BottomDestination("today", "Hoje", Icons.Rounded.Home),
    BottomDestination("calendar", "Agenda", Icons.Rounded.CalendarMonth),
    BottomDestination("plan", "Plano", Icons.Rounded.AutoAwesome),
    BottomDestination("tasks", "Tarefas", Icons.Rounded.CheckCircle),
    BottomDestination("more", "Mais", Icons.Rounded.MoreHoriz)
)

@Composable
fun OrganizaApp(viewModel: OrganizaViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "today") {
            composable("today") {
                DashboardScreen(
                    viewModel = viewModel, contentPadding = padding,
                    openPlan = { navController.navigate("plan") },
                    openCalendar = { navController.navigate("calendar") },
                    openTasks = { navController.navigate("tasks") }
                )
            }
            composable("calendar") { CalendarScreen(viewModel, padding) }
            composable("plan") { PlanScreen(viewModel, padding) }
            composable("tasks") { TasksScreen(viewModel, padding) }
            composable("more") { MoreScreen(viewModel, padding, openShifts = { navController.navigate("shifts") }, openGoals = { navController.navigate("goals") }) }
            composable("shifts") { ShiftsScreen(viewModel, padding) }
            composable("goals") { GoalsScreen(viewModel, padding) }
        }
    }
}
