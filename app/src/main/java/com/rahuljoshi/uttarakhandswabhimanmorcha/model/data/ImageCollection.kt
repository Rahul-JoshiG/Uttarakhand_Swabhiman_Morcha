package com.rahuljoshi.uttarakhandswabhimanmorcha.model.data

import com.rahuljoshi.uttarakhandswabhimanmorcha.R

object ImageCollection {

    fun imageForHome(): List<Int>{
        return listOf<Int>(
            R.drawable.twitter, R.drawable.facebook, R.drawable.instagram,
            R.drawable.ums_logo, R.drawable.new_complaint, R.drawable.my_complaint
        )
    }

    fun allImages(): List<Int>{
        return listOf<Int>(

        )
    }
}