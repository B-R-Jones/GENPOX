package com.example.genpox.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.theme.*
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.cyberglass

/**
 * Interactive button size configurations.
 */
enum class PoxButtonSize(
    val height: Dp,
    val fontSize: TextUnit,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
) {
    COMPACT(28.dp, 8.sp, 8.dp, 2.dp),
    STANDARD(34.dp, 9.sp, 12.dp, 4.dp),
    LARGE(42.dp, 11.sp, 16.dp, 6.dp),
}

/**
 * Standardized color categories dynamically bound to CyberTheme.
 */
enum class PoxButtonType(
    val getBorderColor: () -> Color,
    val getTextColor: () -> Color,
    val getBgColor: () -> Color,
) {
    GREEN_PHOSPHOR({ CyberTheme.green }, { CyberTheme.green }, { CyberTheme.green.copy(alpha = 0.15f) }),
    GREEN_MUTED({ CyberTheme.greenDim }, { CyberTheme.greenDim }, { CyberTheme.greenDim.copy(alpha = 0.10f) }),
    CYAN_CELESTIAL({ CyberTheme.cyan }, { CyberTheme.cyan }, { CyberTheme.cyan.copy(alpha = 0.15f) }),
    PURPLE_ANOMALY({ CyberTheme.purple }, { CyberTheme.purple }, { CyberTheme.purple.copy(alpha = 0.15f) }),
    RED_DANGER({ CyberTheme.red }, { CyberTheme.red }, { CyberTheme.red.copy(alpha = 0.15f) }),
    YELLOW_WARNING({ Color.Yellow }, { Color.Yellow }, { Color.Yellow.copy(alpha = 0.15f) }),
}

/**
 * Preset audio feedback triggers for button interactions.
 */
enum class PoxButtonSound {
    NONE,
    BEEP_DEFAULT,
    BEEP_HIGH,
    BEEP_LOW,
    COMBINATOR_TICK,
    REJECT_BEEP,
    SUCCESS_CHIME,
}

/**
 * A standardized, double-layered cyberglass in-frame button for menu controls and dialogs.
 */
@Composable
fun PoxButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    buttonType: PoxButtonType = PoxButtonType.GREEN_PHOSPHOR,
    buttonSize: PoxButtonSize = PoxButtonSize.STANDARD,
    sound: PoxButtonSound = PoxButtonSound.BEEP_DEFAULT,
    viewModel: MainViewModel? = null,
) {
    // Dim the styling when disabled
    val activeBorderColor = if (enabled) buttonType.getBorderColor() else Color.Gray.copy(alpha = 0.25f)
    val activeTextColor = if (enabled) buttonType.getTextColor() else Color.Gray.copy(alpha = 0.45f)
    val activeBgColor = if (enabled) buttonType.getBgColor() else Color.Black.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .height(buttonSize.height)
            .cyberglass(
                borderColor = activeBorderColor,
                backgroundColor = activeBgColor,
            )
            .pointerInput(enabled, sound, viewModel, onClick) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        if (viewModel != null) {
                            when (sound) {
                                PoxButtonSound.BEEP_DEFAULT -> viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                PoxButtonSound.BEEP_HIGH -> viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                                PoxButtonSound.BEEP_LOW -> viewModel.synthManager.playBeep(400f, 0.08f, "sine")
                                PoxButtonSound.COMBINATOR_TICK -> viewModel.synthManager.playCombinatorTick()
                                PoxButtonSound.REJECT_BEEP -> viewModel.synthManager.playReject()
                                PoxButtonSound.SUCCESS_CHIME -> viewModel.synthManager.playSynthesisSuccess()
                                PoxButtonSound.NONE -> {}
                            }
                        }
                    },
                    onTap = {
                        onClick()
                    },
                )
            }
            .padding(horizontal = buttonSize.horizontalPadding, vertical = buttonSize.verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(), // Enforce uppercase standard
            color = activeTextColor,
            fontSize = buttonSize.fontSize,
            fontFamily = FontFamily.Default, // Enforce Sans-serif standard for commands
            fontWeight = FontWeight.Bold,
        )
    }
}
