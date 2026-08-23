package com.example.navigation

import android.net.Uri

object OrthoDestinations {
    const val SPLASH = "splash"
    const val MAIN = "main"
    const val CHAPTER_BONES = "chapter/{chapterId}/{chapterTitle}"
    const val BONE_DETAIL = "bone/{boneId}"
    const val IMAGE_VIEWER = "image/{imageId}"
    const val IMPORT = "import"

    fun chapterBones(chapterId: String, chapterTitle: String): String {
        return "chapter/${Uri.encode(chapterId)}/${Uri.encode(chapterTitle)}"
    }

    fun boneDetail(boneId: String): String {
        return "bone/${Uri.encode(boneId)}"
    }

    fun imageViewer(imageId: String): String {
        return "image/${Uri.encode(imageId)}"
    }
}
