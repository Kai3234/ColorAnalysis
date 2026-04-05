package com.example.coloranalysis.ui.navigation

object Destinations {

    const val HOME = "home"

    const val PHOTO = "photo/{profileId}"
    const val CAMERA = "camera/{profileId}"

    const val PHOTOPROCESS = "photoprocess/{profileId}"

    const val FACELANDMARK = "facelandmark/{profileId}"

    const val RESULT = "result/{profileId}"

    const val PREVIEW = "preview"

}

object Routes {

    fun photo(profileId: Int) = "photo/$profileId"

    fun camera(profileId: Int) = "camera/$profileId"

    fun photoprocess(profileId: Int) = "photoprocess/$profileId"

    fun facelandmark(profileId: Int) = "facelandmark/$profileId"

    fun result(profileId: Int) = "result/$profileId"


}