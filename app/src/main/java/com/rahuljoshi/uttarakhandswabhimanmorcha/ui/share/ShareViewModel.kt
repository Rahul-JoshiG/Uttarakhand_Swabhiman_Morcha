package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.share

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor() : ViewModel() {

    suspend fun generateQRCodeBitmapAsync(text: String, size: Int = 512): Bitmap =
        withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Generating QR code for text: $text")
                val hints = hashMapOf<EncodeHintType, Any>(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.ERROR_CORRECTION to "L"
                )
                val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                Log.d(TAG, "QR code generated successfully")
                bmp
            } catch (e: Exception) {
                Log.e(TAG, "Error generating QR code", e)
                throw e
            }
        }

    companion object {
        private const val TAG = "ShareViewModel"
    }
}

