package com.amplifyframework.ui.authenticator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

@Composable
internal fun QrCode(
    uri: String,
    modifier: Modifier = Modifier
) {
    val qrCode = rememberQrCode(data = uri)
    val borderCells = 1 // Border cells are needed in case the background is black - code must have a white border
    val xCells = qrCode.width + borderCells * 2 // cells in qr code + border on each side
    val yCells = qrCode.height + borderCells * 2

    Canvas(modifier = modifier) {
        val xScale = size.width / xCells.toFloat()
        val yScale = size.height / yCells.toFloat()
        val scaleFactor = minOf(xScale, yScale)

        drawRect(
            color = Color.White,
            size = size
        )
        for (x in 0 until qrCode.width) {
            for (y in 0 until qrCode.height) {
                val drawPixel = qrCode.get(x, y) == 1.toByte()
                if (drawPixel) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((x + borderCells) * scaleFactor, (y + borderCells) * scaleFactor),
                        size = Size(scaleFactor, scaleFactor)
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberQrCode(data: String) = remember(data) {
    Encoder.encode(
        data,
        ErrorCorrectionLevel.L,
        mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
    ).matrix
}
