
package com.example.internetphoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// ==========================================
// 1. DATA MODEL
// ==========================================

data class MarsPhoto(
    val id: String,
    @SerializedName("img_src")
    val imgSrc: String
)

// ==========================================
// 2. API SERVICE
// ==========================================

interface MarsApiService {

    @GET("photos")
    suspend fun getPhotos(): List<MarsPhoto>
}

// ==========================================
// 3. RETROFIT INSTANCE
// ==========================================

object RetrofitInstance {

    private const val BASE_URL =
        "https://android-kotlin-fun-mars-server.appspot.com/"

    val api: MarsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MarsApiService::class.java)
    }
}

// ==========================================
// 4. REPOSITORY
// ==========================================

class MarsPhotosRepository {

    suspend fun getMarsPhotos(): List<MarsPhoto> {
        return RetrofitInstance.api.getPhotos()
    }
}

// ==========================================
// 5. UI STATE
// ==========================================

sealed interface MarsUiState {

    data object Loading : MarsUiState

    data class Success(
        val photos: List<MarsPhoto>
    ) : MarsUiState

    data object Error : MarsUiState
}

// ==========================================
// 6. VIEW MODEL
// ==========================================

class MarsViewModel : ViewModel() {

    private val repository = MarsPhotosRepository()

    private val _marsUiState =
        MutableStateFlow<MarsUiState>(MarsUiState.Loading)

    val marsUiState: StateFlow<MarsUiState> =
        _marsUiState.asStateFlow()

    init {
        getMarsPhotos()
    }

    fun getMarsPhotos() {

        viewModelScope.launch {

            _marsUiState.value = MarsUiState.Loading

            _marsUiState.value = try {

                val result = withContext(Dispatchers.IO) {
                    repository.getMarsPhotos()
                }

                if (result.isEmpty()) {
                    MarsUiState.Error
                } else {
                    MarsUiState.Success(result)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                MarsUiState.Error
            }
        }
    }
}

// ==========================================
// 7. MAIN ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val marsViewModel =
            ViewModelProvider(this)[MarsViewModel::class.java]

        setContent {
            MarsPhotosApp(marsViewModel)
        }
    }
}

// ==========================================
// 8. MAIN APP
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsPhotosApp(
    viewModel: MarsViewModel
) {

    val marsUiState by
    viewModel.marsUiState.collectAsState()

    MaterialTheme {

        Scaffold(

            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Mars Photos",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFB74C32),
                        titleContentColor = Color.White
                    )
                )
            },

            containerColor = Color(0xFFF5F5F5)

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                when (val state = marsUiState) {

                    is MarsUiState.Loading -> {
                        LoadingScreen()
                    }

                    is MarsUiState.Success -> {
                        PhotosGridScreen(
                            photos = state.photos
                        )
                    }

                    is MarsUiState.Error -> {
                        ErrorScreen(
                            retryAction = {
                                viewModel.getMarsPhotos()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. LOADING SCREEN
// ==========================================

@Composable
fun LoadingScreen() {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        CircularProgressIndicator(
            color = Color(0xFFB74C32),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Loading Mars photos...",
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// ==========================================
// 10. ERROR SCREEN
// ==========================================

@Composable
fun ErrorScreen(
    retryAction: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "⚠",
            style = MaterialTheme.typography.displayLarge,
            color = Color(0xFFB74C32)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unable to load Mars photos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please check your Internet connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = retryAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB74C32)
            )
        ) {
            Text("Retry")
        }
    }
}

// ==========================================
// 11. PHOTOS GRID SCREEN
// ==========================================

@Composable
fun PhotosGridScreen(
    photos: List<MarsPhoto>,
    modifier: Modifier = Modifier
) {

    LazyVerticalGrid(

        columns = GridCells.Adaptive(
            minSize = 150.dp
        ),

        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),

        contentPadding = PaddingValues(
            bottom = 12.dp,
            top = 8.dp
        )

    ) {

        items(
            items = photos,
            key = { photo -> photo.id }
        ) { photo ->

            MarsPhotoCard(
                photo = photo,
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            )
        }
    }
}

// ==========================================
// 12. MARS PHOTO CARD
// ==========================================

@Composable
fun MarsPhotoCard(
    photo: MarsPhoto,
    modifier: Modifier = Modifier
) {

    var isLoading by remember(photo.id) {
        mutableStateOf(true)
    }

    var hasError by remember(photo.id) {
        mutableStateOf(false)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {

            AsyncImage(

                model = photo.imgSrc,

                contentDescription = "Mars photo ${photo.id}",

                contentScale = ContentScale.Crop,

                modifier = Modifier.fillMaxSize(),

                onLoading = {
                    isLoading = true
                    hasError = false
                },

                onSuccess = {
                    isLoading = false
                    hasError = false
                },

                onError = {
                    isLoading = false
                    hasError = true
                }
            )

            if (isLoading && !hasError) {

                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFFB74C32),
                    strokeWidth = 3.dp
                )
            }

            if (hasError) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Gray
                    )

                    Text(
                        text = "Image unavailable",
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
