package com.sparkx.fairyos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sparkx.fairyos.ui.theme.SparkGlass

@Composable
fun SparkGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SparkGlass.Panel)
            .border(1.dp, SparkGlass.Stroke, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        content()
    }
}