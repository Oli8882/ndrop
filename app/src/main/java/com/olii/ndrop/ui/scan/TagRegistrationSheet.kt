package com.olii.ndrop.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olii.ndrop.nfc.TagType
import com.olii.ndrop.ui.theme.NDropColors
import com.olii.ndrop.ui.theme.NDropTypography

/**
 * NDrop — TagRegistrationSheet
 *
 * Shown when an UNKNOWN tag UID is scanned.
 * Lets user assign a name and type to the physical tag.
 *
 * Signature: Olii-8882
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRegistrationSheet(
    uid: String,
    onRegister: (type: TagType, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<TagType?>(null) }
    var tagLabel by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NDropColors.NavySubtle,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NDropColors.WhiteDim)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("New Tag Detected", style = NDropTypography.titleLarge)
                Text(
                    "UID: $uid",
                    style = NDropTypography.labelSmall,
                    color = NDropColors.WhiteDim
                )
            }

            // Tag label input
            OutlinedTextField(
                value = tagLabel,
                onValueChange = { tagLabel = it },
                label = { Text("Tag Name (e.g. Blue Keyring)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NDropColors.Indigo,
                    unfocusedBorderColor = NDropColors.WhiteDim,
                    focusedLabelColor = NDropColors.Indigo,
                    unfocusedLabelColor = NDropColors.WhiteDim,
                    cursorColor = NDropColors.Indigo,
                    focusedTextColor = NDropColors.White,
                    unfocusedTextColor = NDropColors.WhiteMuted
                )
            )

            // Tag type selector
            Text("Assign Tag Type", style = NDropTypography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TagTypeChip(
                    label = "🅿️  Parking",
                    type = TagType.PARKING,
                    selected = selectedType == TagType.PARKING,
                    accentColor = NDropColors.Mint,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = TagType.PARKING }
                )
                TagTypeChip(
                    label = "✦  Discovery",
                    type = TagType.DISCOVERY,
                    selected = selectedType == TagType.DISCOVERY,
                    accentColor = NDropColors.Amber,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = TagType.DISCOVERY }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TagTypeChip(
                    label = "⏱  Timer",
                    type = TagType.TIMER,
                    selected = selectedType == TagType.TIMER,
                    accentColor = NDropColors.IndigoLight,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedType = TagType.TIMER }
                )
                Spacer(Modifier.weight(1f))
            }

            // Register button
            val canRegister = selectedType != null && tagLabel.isNotBlank()

            Button(
                onClick = {
                    if (canRegister) onRegister(selectedType!!, tagLabel.trim())
                },
                enabled = canRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NDropColors.Indigo,
                    contentColor = NDropColors.White,
                    disabledContainerColor = NDropColors.WhiteDim.copy(alpha = 0.3f)
                )
            ) {
                Text("Register Tag", style = NDropTypography.labelLarge)
            }
        }
    }
}

@Composable
private fun TagTypeChip(
    label: String,
    type: TagType,
    selected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.15f) else NDropColors.NavyElevated
    val borderColor = if (selected) accentColor else NDropColors.WhiteDim.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = NDropTypography.labelLarge,
            color = if (selected) accentColor else NDropColors.WhiteMuted,
            textAlign = TextAlign.Center
        )
    }
}
