package cn.xing.fileclouddisk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.xing.fileclouddisk.data.SessionViewModel
import cn.xing.fileclouddisk.network.FileItem
import cn.xing.fileclouddisk.ui.screens.*
import cn.xing.fileclouddisk.ui.theme.CloudDiskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloudDiskTheme {
                CloudDiskApp()
            }
        }
    }
}

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val MAIN = "main"
    const val UPLOAD = "upload"
    const val FILE_DETAIL = "file_detail/{fileId}"
    fun fileDetail(id: String) = "file_detail/$id"
}

@Composable
fun CloudDiskApp() {
    val session: SessionViewModel = viewModel()
    val navController = rememberNavController()

    
    val startDestination = if (!session.isLoggedIn) Routes.LOGIN else Routes.MAIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                session = session,
                onLoginSuccess = { navController.navigateSingleTop(Routes.MAIN) },
                onGoRegister = { navController.navigate(Routes.REGISTER) },
                onGoForgot = { navController.navigate(Routes.FORGOT) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                session = session,
                onRegistered = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                session = session,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainTabsScreen(
                session = session,
                onOpenFile = { file -> navController.navigate(Routes.fileDetail(file.id)) },
                onUpload = { navController.navigate(Routes.UPLOAD) },
                onLoggedOut = { navController.navigateSingleTop(Routes.LOGIN) }
            )
        }

        composable(Routes.UPLOAD) {
            UploadScreen(
                session = session,
                onUploaded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.FILE_DETAIL,
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("fileId") ?: ""
            FileDetailScreen(
                session = session,
                fileId = id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainTabsScreen(
    session: SessionViewModel,
    onOpenFile: (FileItem) -> Unit,
    onUpload: () -> Unit,
    onLoggedOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "文件") },
                    label = { Text("文件") }
                )
                if (session.isAdmin) {
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "回收站") },
                        label = { Text("回收站") }
                    )
                }
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                    label = { Text("我的") }
                )
            }
        }
    ) { padding ->
        
        Box(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            when (selectedTab) {
                0 -> HomeScreen(session = session, onOpenFile = onOpenFile, onUpload = onUpload)
                1 -> if (session.isAdmin) TrashScreen(session = session) else HomeScreen(session = session, onOpenFile = onOpenFile, onUpload = onUpload)
                else -> ProfileScreen(session = session, onLoggedOut = onLoggedOut)
            }
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}
