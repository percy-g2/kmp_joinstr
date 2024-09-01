package invincible.privacy.joinstr.ui.pools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import invincible.privacy.joinstr.ui.components.ProgressDialog

@Composable
fun PoolScreen(
    poolsViewModel: PoolsViewModel
) {
    val isLoading by poolsViewModel.isLoading.collectAsState()

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

    if (isLoading && selectedTab == 0) {
        ProgressDialog()
    }

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
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                }
            ) {
                val color = if (selectedTab == 0) Color.DarkGray else Color.Gray
                Text(
                    text = "Create New Pool",
                    style = MaterialTheme.typography.labelMedium.copy(color = color)
                )
            }
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                }
            ) {
                val color = if (selectedTab == 1) Color.DarkGray else Color.Gray
                Text(
                    text = "My Pools",
                    style = MaterialTheme.typography.labelMedium.copy(color = color)
                )
            }
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                }
            ) {
                val color = if (selectedTab == 2) Color.DarkGray else Color.Gray
                Text(
                    text = "View Other Pools",
                    style = MaterialTheme.typography.labelMedium.copy(color = color)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(3.dp),
                    ambientColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .2f),
                    spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .2f)
                ),
            color = MaterialTheme.colorScheme.background.copy(alpha = .5f),
            thickness = 1.dp
        )

        AnimatedVisibility(
            visible = selectedTab == 0,
            enter = expandIn(expandFrom = Alignment.Center),
            exit = shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            CreateNewPoolScreen(poolsViewModel = poolsViewModel)
        }

        AnimatedVisibility(
            visible = selectedTab == 1,
            enter = expandIn(expandFrom = Alignment.Center),
            exit = shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            MyPoolsScreens(poolsViewModel = poolsViewModel)
        }

        AnimatedVisibility(
            visible = selectedTab == 2,
            enter = expandIn(expandFrom = Alignment.Center),
            exit = shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            OtherPoolsScreen(poolsViewModel = poolsViewModel)
        }
    }
}