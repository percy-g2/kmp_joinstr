package invincible.privacy.joinstr.ui.components.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import invincible.privacy.joinstr.ui.components.timeline.data.Item
import invincible.privacy.joinstr.ui.components.timeline.data.extractFirstTime
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.image_1
import joinstr.composeapp.generated.resources.image_2
import org.jetbrains.compose.resources.painterResource

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
            text = item.name,
        )
        item.description?.let {
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

@Composable
fun HorizontalEventContent(item: Item, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .width(160.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(12.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            text = item.name,
        )
        item.description?.let {
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

@Composable
fun ExtendedEventAdditionalContent(item: Item, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            fontSize = 12.sp,
            text = item.description?.extractFirstTime() ?: "",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ExtendedEventContent(item: Item, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .wrapContentHeight(),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 4.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                text = item.name,
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 2.dp),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                text = item.info,
            )
            item.description?.let {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    text = it,
                )
            }
            if (item.images.isNotEmpty()) {
                Column {
                    Row {
                        item.images.forEach {
                            Image(
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(top = 12.dp, end = 8.dp)
                                    .clip(
                                        RoundedCornerShape(5),
                                    ),
                                contentScale = ContentScale.Crop,
                                painter = painterResource(resource = it),
                                contentDescription = null,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        text = "From Google Photos",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (item.showActions) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = {}) {
                        Icon(Icons.Filled.Check, "Add item")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Yes")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {}) {
                        Icon(Icons.Filled.Edit, "Edit item")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Edit")
                    }
                }
            }
        }
    }
}

@Composable
fun Int.decidePointAnimation(): EventPointAnimation? {
    return if (this == 2) JetLimeEventDefaults.pointAnimation() else null
}

fun placeImages(i: Int) = if (i == 1) {
    listOf(
        Res.drawable.image_1,
        Res.drawable.image_2,
    )
} else {
    listOf()
}

fun placeInfo(i: Int) = "Address ${i + 1}, City, Country"

fun placeDescription(i: Int) = "Visited at ${10 + i % 12}:${
    if (i % 2 == 0) {
        "00"
    } else {
        "30"
    }
} AM"

fun activityInfo(i: Int) = "${1 + i / 2} mi . ${15 + i * 2} min"

fun activityDescription(i: Int) = "${1 + i % 12}:${if (i % 2 == 0) "00" else "30"} PM - " +
    "${1 + (i + 1) % 12}:${if ((i + 1) % 2 == 0) "00" else "30"} PM"

fun Int.decidePointType(): EventPointType {
    return when (this) {
        1 -> EventPointType.filled(
            0.8f,
        )

        4 -> EventPointType.filled(0.2f)
        else -> EventPointType.Default
    }
}