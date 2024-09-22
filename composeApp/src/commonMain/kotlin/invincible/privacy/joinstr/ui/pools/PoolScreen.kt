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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import invincible.privacy.joinstr.ui.components.ProgressDialog
import kotlinx.coroutines.launch

@Composable
fun PoolScreen(
    poolsViewModel: PoolsViewModel,
) {
    val isLoading by poolsViewModel.isLoading.collectAsState()
    val tabScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val tabTitles = listOf("Create New Pool", "My Pools", "View Other Pools", "History")
    val tabWidths = remember {
        tabTitles.map { title ->
            with(density) {
                title.length * 10.dp.toPx() + 24.dp.toPx()
            }
        }
    }

    @Composable
    fun Tab(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
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

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
        val scrollPosition = tabWidths.take(selectedTab).sum().toInt()
        tabScrollState.animateScrollTo(scrollPosition)
    }

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
                .horizontalScroll(tabScrollState)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                            val scrollPosition = tabWidths.take(index).sum().toInt()
                            tabScrollState.animateScrollTo(scrollPosition)
                        }
                    }
                ) {
                    val color = if (selectedTab == index) Color.DarkGray else Color.Gray
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(color = color)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .shadow(
                    elevation = 1.dp,
                    spotColor = MaterialTheme.colorScheme.onSurface
                ),
            color = MaterialTheme.colorScheme.surface,
            thickness = 1.dp
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> AnimatedVisibility(
                    visible = selectedTab == 0,
                    enter = expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center)
                ) {
                    CreateNewPoolScreen(poolsViewModel = poolsViewModel)
                }

                1 -> AnimatedVisibility(
                    visible = selectedTab == 1,
                    enter = expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center)
                ) {
                    MyPoolsScreens(poolsViewModel = poolsViewModel)
                }

                2 -> AnimatedVisibility(
                    visible = selectedTab == 2,
                    enter = expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center)
                ) {
                    OtherPoolsScreen(poolsViewModel = poolsViewModel)
                }

                3 -> AnimatedVisibility(
                    visible = selectedTab == 3,
                    enter = expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center)
                ) {
                    CoinJoinHistoryScreen(poolsViewModel = poolsViewModel)
                }
            }
        }
    }
}