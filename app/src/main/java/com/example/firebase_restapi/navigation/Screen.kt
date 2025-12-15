package com.example.firebase_restapi.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object RekomendasiTempat : Screen("rekomendasi_tempat")
}

