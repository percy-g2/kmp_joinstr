package invincible.privacy.joinstr.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import joinstr.composeapp.generated.resources.Res
import joinstr.composeapp.generated.resources.joinstr_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.wrapContentSize(),
            painter = painterResource(Res.drawable.joinstr_logo),
            contentDescription = "logo"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Joinstr",
            style = MaterialTheme.typography.titleMedium
        )
    }
}