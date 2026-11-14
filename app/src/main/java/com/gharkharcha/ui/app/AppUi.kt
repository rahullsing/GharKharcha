package com.gharkharcha.ui.app

import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gharkharcha.R
import com.gharkharcha.ui.components.LocalSnackbarHostState
import com.gharkharcha.ui.navigation.AppNavHost
import com.gharkharcha.ui.navigation.TopLevelDestination

@Composable
fun GharKharchaAppUi(
    viewModel: AppViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination.isOn(it) }
    val resources = LocalResources.current
    val undoLabel = stringResource(R.string.action_undo)

    // Undo-delete snackbars, one at a time.
    LaunchedEffect(viewModel) {
        viewModel.undoRequests.collect { request ->
            val message = request.message.asString(resources)
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(request) else viewModel.commit(request)
        }
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    // Bar/rail slides away on pushed screens instead of popping out abruptly.
    val navSuiteState = rememberNavigationSuiteScaffoldState()
    val showNavSuite = currentTopLevel != null || destination == null
    LaunchedEffect(showNavSuite) {
        if (showNavSuite) navSuiteState.show() else navSuiteState.hide()
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        NavigationSuiteScaffold(
            layoutType = layoutType,
            state = navSuiteState,
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { dest ->
                    item(
                        selected = dest == currentTopLevel,
                        onClick = { navController.navigateToTopLevel(dest) },
                        icon = { Icon(painterResource(dest.icon), contentDescription = null) },
                        label = { Text(stringResource(dest.label)) },
                    )
                }
            },
        ) {
            AppNavHost(navController = navController, onNavigateTopLevel = { navController.navigateToTopLevel(it) })
        }
    }
}

private fun NavDestination?.isOn(dest: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(dest.route::class) } == true

fun NavHostController.navigateToTopLevel(dest: TopLevelDestination) {
    navigate(dest.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
