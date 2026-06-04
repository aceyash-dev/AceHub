package com.ace.hub.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun MeScreen(
    onAccentColorChanged: (Color) -> Unit,
    useSystemTheme: Boolean,
    onUseSystemThemeChanged: (Boolean) -> Unit,
    username: String,
    onUsernameChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val pfpFile = File(context.filesDir, "profile_pic.jpg")
    var pfpUri by remember { mutableStateOf<Uri?>(if (pfpFile.exists()) Uri.fromFile(pfpFile) else null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                pfpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            pfpUri = Uri.fromFile(pfpFile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp).clip(CircleShape).clickable { launcher.launch("image/*") },
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (pfpUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(pfpUri),
                    contentDescription = "PFP",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = "PFP", modifier = Modifier.padding(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChanged,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Dark mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Switch to dark mode with system")
            Switch(checked = useSystemTheme, onCheckedChange = onUseSystemThemeChanged)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Accent Color", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Color.Blue, Color.Red, Color.Green, Color.Magenta).forEach { color ->
                Surface(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onAccentColorChanged(color) },
                    color = color
                ) {}
            }
        }
        
        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))
        Text("All-Time Playing Stats", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Play Time: 0m")
                Text("Games Played: 0")
                Text("Favorite Game: --")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aceyash-dev"))
            context.startActivity(intent)
        }) {
            Text("@aceyash-dev")
        }
        Text("©Ace Horizon 2026", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
