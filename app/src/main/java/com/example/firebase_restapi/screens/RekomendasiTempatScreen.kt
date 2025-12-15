package com.example.firebase_restapi.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.firebase_restapi.model.TempatWisata
import com.example.firebase_restapi.utils.fetchTempatWisataFromFirestore
import com.example.firebase_restapi.utils.uploadImageToFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekomendasiTempatScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    var tempatWisataList by remember { mutableStateOf<List<TempatWisata>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val data: List<TempatWisata> = withContext(Dispatchers.IO) {
                fetchTempatWisataFromFirestore(firestore)
            }
            tempatWisataList = data
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekomendasi Tempat Wisata") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onBackToLogin()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (tempatWisataList.isEmpty()) {
                Text(
                    text = "Belum ada data tempat wisata.\nTambahkan dengan tombol + di bawah.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(tempatWisataList) { tempat ->
                        TempatItemEditable(
                            tempat = tempat,
                            onDelete = {
                                coroutineScope.launch {
                                    val data: List<TempatWisata> = withContext(Dispatchers.IO) {
                                        fetchTempatWisataFromFirestore(firestore)
                                    }
                                    tempatWisataList = data
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            TambahTempatWisataDialog(
                firestore = firestore,
                context = context,
                onDismiss = { showDialog = false },
                onTambah = { nama, deskripsi, gambarUri ->
                    coroutineScope.launch {
                        val data: List<TempatWisata> = withContext(Dispatchers.IO) {
                            fetchTempatWisataFromFirestore(firestore)
                        }
                        tempatWisataList = data
                    }
                }
            )
        }
    }
}

@Composable
fun TempatItemEditable(
    tempat: TempatWisata,
    onDelete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(tempat.gambarUriString?.let { Uri.parse(it) })
                        .crossfade(true)
                        .build()
                ),
                contentDescription = tempat.nama,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        text = tempat.nama,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp, top = 12.dp)
                    )
                    Text(
                        text = tempat.deskripsi,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(250.dp, 0.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            expanded = false
                            firestore.collection("tempat_wisata").document(tempat.nama)
                                .delete()
                                .addOnSuccessListener {
                                    onDelete()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("TempatItemEditable", "Error deleting document", e)
                                }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TambahTempatWisataDialog(
    firestore: FirebaseFirestore,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onTambah: (String, String, String?) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var gambarUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gambarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        gambarUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tempat Wisata Baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Tempat") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading,
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                gambarUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Gambar yang dipilih",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { gambarLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("Pilih Gambar")
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        nama.isBlank() -> {
                            errorMessage = "Nama tempat tidak boleh kosong"
                            return@Button
                        }
                        deskripsi.isBlank() -> {
                            errorMessage = "Deskripsi tidak boleh kosong"
                            return@Button
                        }
                        gambarUri == null -> {
                            errorMessage = "Pilih gambar terlebih dahulu"
                            return@Button
                        }
                    }

                    isUploading = true
                    errorMessage = null
                    val tempatWisata = TempatWisata(nama, deskripsi)
                    uploadImageToFirestore(
                        firestore,
                        context,
                        gambarUri!!,
                        tempatWisata,
                        onSuccess = { uploadedTempat: TempatWisata ->
                            isUploading = false
                            onTambah(nama, deskripsi, uploadedTempat.gambarUriString)
                            onDismiss()
                        },
                        onFailure = { e: Exception ->
                            isUploading = false
                            errorMessage = "Upload gagal: ${e.message}"
                        }
                    )
                },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Tambah")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Batal")
            }
        }
    )
}
