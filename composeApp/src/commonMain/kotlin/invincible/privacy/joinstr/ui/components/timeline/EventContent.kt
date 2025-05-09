package invincible.privacy.joinstr.ui.components.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.openLink
import invincible.privacy.joinstr.theme.lightBlue
import invincible.privacy.joinstr.ui.components.timeline.data.Item

@Composable
fun VerticalEventContent(item: Item, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            text = item.title,
        )
        item.description?.let {
            if (it.contains("PSBT")) {
                OutlinedTextField(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    value = it.replace("PSBT: ", ""),
                    onValueChange = { /* no-op */ },
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PSBT",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        text = it,
                    )
                }
            }
        }
        if (item.info.isNotEmpty()) {
            val (label, id) = item.info.split(":", limit = 2)
            val coloredText = buildAnnotatedString {
                append(label.plus(":"))
                withStyle(style = SpanStyle(color = lightBlue)) {
                    append(id)
                }
            }
            val url = "https://mempool.space/signet/tx/${id.trim()}"
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            openLink(url)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    text = coloredText,
                )
            }
        }
    }
}