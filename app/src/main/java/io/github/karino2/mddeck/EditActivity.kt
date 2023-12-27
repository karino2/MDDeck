package io.github.karino2.mddeck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.karino2.mddeck.ui.theme.MDDeckTheme
import kotlinx.coroutines.delay
import java.util.Date

class EditActivity : ComponentActivity() {
    private fun onSave(text: String) {
        Intent().apply {
            putExtra("NEW_CONTENT", text)
            if (time != 0L)
                putExtra(MainActivity.EXTRA_DATE_KEY, time)
        }.also { setResult(RESULT_OK, it) }
        finish()
    }

    private val requester = FocusRequester()

    private var time = 0L

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("DT_LONG", time)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        time = savedInstanceState.getLong("DT_LONG")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        intent?.let {
            time = it.getLongExtra(MainActivity.EXTRA_DATE_KEY, 0)
        }

        setContent {
            MDDeckTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column{
                        var text by remember { mutableStateOf(defaultText) }
                        TopAppBar(title={
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { onSave(text) }) {
                                    Icon(imageVector = Icons.Default.Done, contentDescription = "Save")
                                }
                            }
                        },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            })
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(requester)
                        )
                        LaunchedEffect(Unit) {
                            // Need this delay for openning softkey.
                            delay(300)
                            requester.requestFocus()
                        }
                    }
                }
            }
        }
    }
}
