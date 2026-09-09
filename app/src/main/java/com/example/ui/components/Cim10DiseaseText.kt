package com.example.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.local.model.Cim10Disease

@Composable
fun Cim10DiseaseText(
    text: String,
    diseases: List<Cim10Disease>,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    var selectedDisease by remember { mutableStateOf<Cim10Disease?>(null) }
    val diseaseColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, diseases, diseaseColor) {
        buildDiseaseAnnotatedString(text, diseases, diseaseColor)
    }

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotatedText.getStringAnnotations("cim10", offset, offset)
                .firstOrNull()
                ?.let { annotation -> diseases.firstOrNull { it.code == annotation.item } }
                ?.let { selectedDisease = it }
        }
    )

    selectedDisease?.let { disease ->
        AlertDialog(
            onDismissRequest = { selectedDisease = null },
            title = { Text(disease.maladie, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Code CIM-10 : ${disease.code}\n\n" +
                        "Informations issues du dictionnaire CIM-10 intégré à l’application."
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedDisease = null }) { Text("Fermer") }
            }
        )
    }
}

private fun buildDiseaseAnnotatedString(
    text: String,
    diseases: List<Cim10Disease>,
    diseaseColor: Color
): AnnotatedString {
    val matches = mutableListOf<Triple<Int, Int, Cim10Disease>>()
    diseases.asSequence()
        .sortedByDescending { it.maladie.length }
        .forEach { disease ->
            listOf(disease.maladie, disease.code).forEach { searchableValue ->
                if (searchableValue.length < 2) return@forEach
                var start = text.indexOf(searchableValue, ignoreCase = true)
                while (start >= 0) {
                    val end = start + searchableValue.length
                    if (matches.none { start < it.second && end > it.first }) {
                        matches += Triple(start, end, disease)
                    }
                    start = text.indexOf(searchableValue, start + 1, ignoreCase = true)
                }
            }
        }

    return AnnotatedString.Builder().apply {
        append(text)
        matches.forEach { (start, end, disease) ->
            addStyle(
                SpanStyle(
                    color = diseaseColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                start,
                end
            )
            addStringAnnotation("cim10", disease.code, start, end)
        }
    }.toAnnotatedString()
}