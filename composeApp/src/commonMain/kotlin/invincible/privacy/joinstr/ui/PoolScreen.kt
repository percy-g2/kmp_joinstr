package invincible.privacy.joinstr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PoolScreen() {
    @Composable
    fun Tab(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color.LightGray else Color.Transparent)
                .clickable(role = Role.Tab, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                val color = if (selectedTab == 0) Color.DarkGray else Color.Gray
                Text("Create New Pool", style = MaterialTheme.typography.labelMedium.copy(color = color))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                val color = if (selectedTab == 1) Color.DarkGray else Color.Gray
                Text("My Pools", style = MaterialTheme.typography.labelMedium.copy(color = color))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                val color = if (selectedTab == 2) Color.DarkGray else Color.Gray
                Text("View Other Pools", style = MaterialTheme.typography.labelMedium.copy(color = color))
            }
        }

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                var denomination by remember { mutableStateOf("") }
                var peers by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = denomination,
                    onValueChange = {
                        denomination = it
                    },
                    label = { Text("Denomination") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.wrapContentSize(),
                    trailingIcon = {
                        if (denomination.isNotEmpty()) {
                            IconButton(onClick = { denomination = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear text"
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = peers,
                    onValueChange = {
                        peers = it
                    },
                    label = { Text("Number of peers") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.wrapContentSize(),
                    trailingIcon = {
                        if (peers.isNotEmpty()) {
                            IconButton(onClick = { peers = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear text"
                                )
                            }
                        }
                    }
                )

                Button(
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = denomination.isNotEmpty() && peers.isNotEmpty(),
                    onClick = {
                        // TODO
                    }
                ) {
                    Text(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.Bottom),
                        text = "Create",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}