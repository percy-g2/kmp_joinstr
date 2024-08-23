package invincible.privacy.joinstr

import androidx.compose.ui.window.ComposeUIViewController
import invincible.privacy.joinstr.utils.CryptoUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun MainViewController() = ComposeUIViewController {
    App()
    main()
}