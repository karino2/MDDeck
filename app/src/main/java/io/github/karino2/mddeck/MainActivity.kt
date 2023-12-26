package io.github.karino2.mddeck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.karino2.mddeck.ui.MDDecks
import io.github.karino2.mddeck.ui.theme.MDDeckTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MainActivity : ComponentActivity() {
    companion object {
        const val  LAST_URI_KEY = "last_uri_path"
        fun lastUriStr(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)
        fun writeLastUriStr(ctx: Context, path : String) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, path)
            .commit()

        fun resetLastUriStr(ctx: Context) = sharedPreferences(ctx).edit()
            .putString(LAST_URI_KEY, null)
            .commit()

        private fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("MDDeck", Context.MODE_PRIVATE)

        fun showMessage(ctx: Context, msg : String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    val viewModel: MDDeckVM by viewModels()

    private var _url : Uri? = null

    private val rootDir: RootDir
        get() = _url?.let { RootDir(FastFile.fromTreeUri(this, it)) } ?: throw Exception("No url set")

    private fun writeLastUri(uri: Uri) = writeLastUriStr(this, uri.toString())
    private val lastUri: Uri?
        get() = lastUriStr(this)?.let { Uri.parse(it) }

    private val getRootDirUrl = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        // if cancel, null coming.
        uri?.let {
            contentResolver.takePersistableUriPermission(it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            writeLastUri(it)
            openRootDir(it)
        }
    }

    private val getEditResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result->
        if (result.resultCode == Activity.RESULT_OK) {
            val content = result.data?.getStringExtra("NEW_CONTENT") ?: ""
            val cell = MdCell(Date(), content)
            rootDir.saveMd(cell)
            viewModel.blocks.value.appendTail(cell)
        }
    }

    fun reloadHitokotos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val hitokotos = rootDir.listHitokotoFiles().take(30).map { MdCell.fromFile(it) }.toList()
            withContext(Dispatchers.Main) {
                viewModel.blocks.value = hitokotos
            }
        }
    }
    private fun openRootDir(url: Uri) {
        _url = url
        reloadHitokotos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MDDeckTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MDDecks(viewModel)
                    /*
                    Column {
                        TopAppBar(title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    showMessage(this@MainActivity, "Reload")
                                    reloadHitokotos()
                                }) {
                                    Icon(painter = painterResource(id = R.drawable.outline_refresh_24), contentDescription = "Reload")
                                }
                            }
                        },
                        )

                        Column(modifier= Modifier
                            .padding(13.dp, 5.dp)
                            .verticalScroll(rememberScrollState())
                            .weight(weight =1f, fill = false)) {
                            cells.value.forEach { CellView(it) }
                        }

                        Row(modifier = Modifier.padding(5.dp, 5.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End){
                            Button(onClick = {
                                Intent(this@MainActivity, EditActivity::class.java).also {
                                    getEditResult.launch(it)
                                }
                            }) {
                                Text("+", fontSize=23.sp)
                            }
                        }
                    }
                     */
                }
            }
        }

        try {
            lastUri?.let {
                openRootDir(it)

                if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
                    Intent(this@MainActivity, EditActivity::class.java).also { editIntent->
                        editIntent.putExtra(Intent.EXTRA_TEXT, receivedText(intent))
                        getEditResult.launch(editIntent)
                    }


                }
                return
            }
        } catch(_: Exception) {
            showMessage(this, "Can't open saved dir. Please reopen.")
        }
        getRootDirUrl.launch(null)
    }

    private fun receivedText(intent: Intent) : String {
        val body = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        return intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let { subject->
            "${subject} ${body}"
        } ?: body
    }
}
