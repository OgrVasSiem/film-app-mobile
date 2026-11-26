package com.example.film_app_mobile.core.models

import com.example.film_app_mobile.R
import com.example.film_app_mobile.navigation.Page

enum class Tabs(val page: Page, val label: Int, val icon: Int){
    HOME(page = Page.Home, label = R.string.home_title, icon = R.drawable.ic_home),
    LIBRARY(page = Page.Library , label = R.string.library_title, icon = R.drawable.ic_library),
    PROFILE(page = Page.Profile, label = R.string.profile_title, icon = R.drawable.ic_profile)
}
