package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SectionEntity
import com.example.ui.theme.ClinicalAnatomy
import com.example.ui.theme.ClinicalApproach
import com.example.ui.theme.ClinicalDanger
import com.example.ui.theme.ClinicalFixation
import com.example.ui.theme.ClinicalPearl
import com.example.ui.theme.ClinicalRehab
import com.example.ui.theme.ClinicalWarning
import com.example.utils.MediaActions

@Composable
fun PageSectionCard(
    section: SectionEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (badgeColor, typeIcon, typeLabel) = when (section.sectionType.uppercase()) {
        "ANATOMY" -> Triple(ClinicalAnatomy, Icons.Default.Visibility, "ANATOMY & LANDMARKS")
        "APPROACH" -> Triple(ClinicalApproach, Icons.Default.MedicalServices, "SURGICAL APPROACH")
        "REDUCTION" -> Triple(ClinicalWarning, Icons.Default.PanTool, "REDUCTION MANEUVER")
        "FIXATION" -> Triple(ClinicalFixation, Icons.Default.Healing, "FIXATION CONSTRUCT")
        "PEARLS" -> Triple(ClinicalPearl, Icons.Default.Psychology, "SURGICAL PEARLS")
        "COMPLICATIONS" -> Triple(ClinicalDanger, Icons.Default.Warning, "COMPLICATIONS & PITFALLS")
        "REHAB" -> Triple(ClinicalRehab, Icons.Default.Speed, "REHABILITATION")
        else -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.MedicalServices, section.sectionType)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("section_card_${section.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Section Type Badge + Actions (Copy & Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            val copyText = buildString {
                                appendLine(section.title)
                                appendLine()
                                appendLine(section.content)
                                if (section.pearls.isNotBlank()) {
                                    appendLine("\nPearls:\n${section.pearls}")
                                }
                                if (section.warnings.isNotBlank()) {
                                    appendLine("\nDanger Zone:\n${section.warnings}")
                                }
                            }
                            MediaActions.copyToClipboard(context, section.title, copyText)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("copy_section_${section.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Section",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            MediaActions.shareClinicalTip(context, section.title, section.content)
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("share_section_${section.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Section",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section Title
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Operative Content Body
            OperativeText(content = section.content)

            // Pearls callout if present
            if (section.pearls.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                PearlCard(pearlText = section.pearls)
            }

            // Warning callout if present
            if (section.warnings.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                WarningBanner(warningText = section.warnings)
            }
        }
    }
}
