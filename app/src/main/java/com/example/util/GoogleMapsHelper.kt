package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object GoogleMapsHelper {

    /**
     * Launches Google Maps navigation or search for a specific coordinate and place name.
     */
    fun openGoogleMaps(
        context: Context,
        latitude: Double,
        longitude: Double,
        destinationName: String
    ) {
        try {
            // Intent for Google Maps app
            val encodedName = Uri.encode(destinationName)
            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to geo URI or Web Google Maps
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&destination_place_id=$encodedName")
                val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            val fallbackUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * Launches phone dialer for medical facility contact.
     */
    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.trim()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}
