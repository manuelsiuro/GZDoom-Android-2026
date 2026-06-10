package net.nullsum.freedoom.ui.options

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.nullsum.freedoom.R
import net.nullsum.freedoom.ui.DoomIcons
import net.nullsum.freedoom.ui.theme.monospaceBody

/** Compose replacement for DirectoryChooserDialog: recursive browse + new-folder creation. */
@Composable
fun DirectoryPickerDialog(
    initialDir: String,
    onDismiss: () -> Unit,
    onChosen: (String) -> Unit,
) {
    val context = LocalContext.current
    val fallbackDir = remember {
        (context.getExternalFilesDir(null) ?: context.filesDir).absolutePath
    }
    var currentDir by remember {
        val f = File(initialDir)
        mutableStateOf(
            try {
                (if (f.exists() && f.isDirectory) f else File(fallbackDir)).canonicalPath
            } catch (e: IOException) {
                fallbackDir
            }
        )
    }
    var subdirs by remember { mutableStateOf<List<String>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }
    var showNewFolderPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(currentDir, refresh) {
        subdirs = withContext(Dispatchers.IO) {
            File(currentDir).listFiles().orEmpty()
                .filter { it.isDirectory }
                .map { it.name }
                .sorted()
        }
    }

    fun navigateUp() {
        if (currentDir != "/") {
            currentDir = File(currentDir).parent ?: currentDir
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.folder_name_prompt),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.size(8.dp))
                Text(currentDir, style = monospaceBody())
                Spacer(Modifier.size(12.dp))

                LazyColumn(Modifier.weight(1f)) {
                    item {
                        DirRow(name = "..", onClick = ::navigateUp)
                    }
                    items(subdirs, key = { it }) { name ->
                        DirRow(name = name) {
                            currentDir = try {
                                File("$currentDir/$name").canonicalPath
                            } catch (e: IOException) {
                                currentDir
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showNewFolderPrompt = true }) {
                        Text(stringResource(R.string.new_folder_button))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = { onChosen(currentDir) }) {
                        Text(stringResource(R.string.ok_confirm))
                    }
                }
            }
        }
    }

    if (showNewFolderPrompt) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderPrompt = false },
            title = { Text(stringResource(R.string.new_folder_name_prompt)) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    showNewFolderPrompt = false
                    val newDir = File("$currentDir/$folderName")
                    if (!newDir.exists() && newDir.mkdir()) {
                        currentDir = newDir.path
                        refresh++
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.create_folder_failed, folderName),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) {
                    Text(stringResource(R.string.ok_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderPrompt = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
        )
    }
}

@Composable
private fun DirRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            DoomIcons.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}
