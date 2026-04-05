package com.example.coloranalysis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coloranalysis.ui.face.FaceLandmarkScreen
import com.example.coloranalysis.ui.home.HomeScreen
import com.example.coloranalysis.ui.photo.CameraScreen
import com.example.coloranalysis.ui.photo.PhotoProcessScreen
import com.example.coloranalysis.ui.photo.PhotoScreen
import com.example.coloranalysis.ui.result.ResultScreen
import com.example.coloranalysis.ui.result.SeasonPreviewScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier : Modifier = Modifier
)
{
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {

        // HOME
        composable(route = Destinations.HOME) {
            HomeScreen(
                navigateToPhoto = { profileId ->
                    navController.navigate(Routes.photo(profileId))
                },
                navigateToResult = { profileId ->
                    navController.navigate(Routes.result(profileId))
                },
                navigateToPreview = {
                    navController.navigate(Destinations.PREVIEW)
                }

            )
        }

        // PHOTO
        composable(route = Destinations.PHOTO) { backStackEntry ->

            val profileId =
                backStackEntry.arguments?.getString("profileId")?.toInt() ?: 0

            PhotoScreen(
                profileId = profileId,
                navigateToCamera = {
                    navController.navigate(Routes.camera(profileId))
                },
                navigateToResult = {
                    navController.navigate(Routes.result(profileId))
                },
                navigateToHome = {
                    navController.navigate(Destinations.HOME)
                },
                navigateToPhotoProcess = {
                    navController.navigate(Routes.photoprocess(profileId))
                }
            )
        }

        // CAMERA
        composable(route = Destinations.CAMERA) { backStackEntry ->

            val profileId =
                backStackEntry.arguments?.getString("profileId")?.toInt() ?: 0

            CameraScreen(
                profileId = profileId,
                navigateToResult = {
                    navController.navigate(Routes.result(profileId))
                },
                navigateToHome = {
                    navController.navigate(Destinations.HOME)
                },
                navigateToPhoto = {
                    navController.navigate(Routes.photo(profileId))
                },
                navigateToPhotoProcess = {
                    navController.navigate(Routes.photoprocess(profileId))
                }


            )
        }

        // PHOTOPROCESS
        composable(route = Destinations.PHOTOPROCESS) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")?.toInt() ?: 0

            PhotoProcessScreen (
                profileId = profileId,
                navigateToFaceLandmark = {
                    navController.navigate(Routes.facelandmark(profileId))
                },
                navigateToResult = {
                    navController.navigate(Routes.result(profileId))
                },
            )
        }

        // FACELANDMARK
        composable(route = Destinations.FACELANDMARK) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")?.toInt() ?: 0

            FaceLandmarkScreen (
                profileId = profileId,
                navigateToResult = {
                    navController.navigate(Routes.result(profileId))
                },
            )
        }

        // RESULT
        composable(route = Destinations.RESULT) { backStackEntry ->

            val profileId =
                backStackEntry.arguments?.getString("profileId")?.toInt() ?: 0

            ResultScreen(
                profileId = profileId,
                navigateToHome = {
                    navController.navigate(Destinations.HOME)
                }
            )
        }

        // PREVIEW
        composable(route = Destinations.PREVIEW) {
            SeasonPreviewScreen(
                onBack = {
                    navController.navigate(Destinations.HOME)
                }
            )
        }

    }
}