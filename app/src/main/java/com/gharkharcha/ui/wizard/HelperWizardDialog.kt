package com.gharkharcha.ui.wizard

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gharkharcha.R

private data class WizardStep(
    @DrawableRes val icon: Int,
    val titleRes: Int,
    val subtitleRes: Int,
    val descRes: Int,
)

private val steps = listOf(
    WizardStep(
        icon = R.drawable.ic_house,
        titleRes = R.string.wizard_step1_title,
        subtitleRes = R.string.wizard_step1_subtitle,
        descRes = R.string.wizard_step1_desc,
    ),
    WizardStep(
        icon = R.drawable.ic_build,
        titleRes = R.string.wizard_step2_title,
        subtitleRes = R.string.wizard_step2_subtitle,
        descRes = R.string.wizard_step2_desc,
    ),
    WizardStep(
        icon = R.drawable.ic_groups,
        titleRes = R.string.wizard_step3_title,
        subtitleRes = R.string.wizard_step3_subtitle,
        descRes = R.string.wizard_step3_desc,
    ),
    WizardStep(
        icon = R.drawable.ic_store,
        titleRes = R.string.wizard_step4_title,
        subtitleRes = R.string.wizard_step4_subtitle,
        descRes = R.string.wizard_step4_desc,
    ),
    WizardStep(
        icon = R.drawable.ic_chart,
        titleRes = R.string.wizard_step5_title,
        subtitleRes = R.string.wizard_step5_subtitle,
        descRes = R.string.wizard_step5_desc,
    ),
)

@Composable
fun HelperWizardDialog(
    onDismiss: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]
    val isLast = currentStep == steps.lastIndex

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (isLast) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.wizard_finish))
                }
            } else {
                Button(onClick = { currentStep++ }) {
                    Text(stringResource(R.string.wizard_next))
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = { currentStep-- }) {
                        Text(stringResource(R.string.wizard_previous))
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.wizard_skip))
                    }
                }
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Step dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStep) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentStep) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.wizard_step_count, currentStep + 1, steps.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut(),
                    )
                },
                label = "wizard_step_animation",
            ) { targetStep ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(targetStep.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(targetStep.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = stringResource(targetStep.subtitleRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(targetStep.descRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        },
    )
}
