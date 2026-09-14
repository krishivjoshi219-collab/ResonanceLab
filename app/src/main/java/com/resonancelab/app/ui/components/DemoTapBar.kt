package com.resonancelab.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonancelab.app.audio.DemoTapSimulator
import com.resonancelab.app.ui.theme.CyberCardBorder
import com.resonancelab.app.ui.theme.CyberDeepSlate
import com.resonancelab.app.ui.theme.CyberSurfaceVariant
import com.resonancelab.app.ui.theme.NeonCyan
import com.resonancelab.app.ui.theme.TextPrimary
import com.resonancelab.app.ui.theme.TextSecondary

/**
 * Standout hook: one-tap demo that works on emulator with zero mic.
 * This is what judges remember — instant magic in <2s.
 */
@Composable
fun DemoTapBar(
    onDemoTap: (DemoTapSimulator.DemoTapType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDeepSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Try instantly — no mic needed",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Play a realistic tap. Perfect for emulator + demo video.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoTapSimulator.DemoTapType.values().forEach { type ->
                    Button(
                        onClick = { onDemoTap(type) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 4.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Text(
                            text = "${type.emoji} ${type.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tip for video: tap each one, watch peak + Q + verdict snap.",
                color = NeonCyan.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
        }
    }
}
