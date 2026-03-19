package com.example.moviexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.moviexplorer.data.Movie
import com.example.moviexplorer.data.Rental
import com.example.moviexplorer.data.local.AppDatabase
import com.example.moviexplorer.data.remote.MovieApiService
import com.example.moviexplorer.data.repository.MovieRepository
import com.example.moviexplorer.ui.theme.*
import com.example.moviexplorer.viewmodel.MovieViewModel
import com.example.moviexplorer.viewmodel.SortOrder
import com.example.moviexplorer.worker.RentalWorker
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

enum class AppTheme {
    DARK, LIGHT, COLORFUL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://fooapi.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(MovieApiService::class.java)
        val repository = MovieRepository(apiService, database.rentalDao())
        
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MovieViewModel(repository) as T
            }
        }

        scheduleRentalReminder()
        enableEdgeToEdge()

        setContent {
            var currentTheme by rememberSaveable { mutableStateOf(AppTheme.DARK) }
            
            val themeColors = when (currentTheme) {
                AppTheme.DARK -> Pair(GradientStart, GradientEnd)
                AppTheme.LIGHT -> Pair(Color(0xFFE0E0E0), Color(0xFFF5F5F5))
                AppTheme.COLORFUL -> Pair(Color(0xFF4A148C), Color(0xFFD32F2F))
            }

            MoviexplorerTheme {
                MainAppContent(viewModelFactory, currentTheme, themeColors) { newTheme ->
                    currentTheme = newTheme
                }
            }
        }
    }

    private fun scheduleRentalReminder() {
        val workRequest = PeriodicWorkRequestBuilder<RentalWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    factory: ViewModelProvider.Factory, 
    currentTheme: AppTheme,
    themeColors: Pair<Color, Color>,
    onThemeChange: (AppTheme) -> Unit
) {
    val navController = rememberNavController()
    val viewModel: MovieViewModel = viewModel(factory = factory)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(260.dp),
                drawerContainerColor = themeColors.first,
                drawerContentColor = if (currentTheme == AppTheme.LIGHT) Color.Black else Color.White
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    "NAVIGATION",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("movie_list") {
                            popUpTo("movie_list") { inclusive = true }
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    label = { Text("My Rentals") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("rental_list")
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        themeColors.first.copy(alpha = 0.75f), 
                        themeColors.second.copy(alpha = 0.85f)
                    )
                )
            ))

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { Text("Movie Explorer", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (currentTheme == AppTheme.LIGHT) Color.Black else Color.White,
                            navigationIconContentColor = if (currentTheme == AppTheme.LIGHT) Color.Black else Color.White,
                            actionIconContentColor = if (currentTheme == AppTheme.LIGHT) Color.Black else Color.White
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { 
                                val nextTheme = when(currentTheme) {
                                    AppTheme.DARK -> AppTheme.LIGHT
                                    AppTheme.LIGHT -> AppTheme.COLORFUL
                                    AppTheme.COLORFUL -> AppTheme.DARK
                                }
                                onThemeChange(nextTheme)
                            }) {
                                Icon(Icons.Default.Palette, contentDescription = "Switch Theme")
                            }
                            IconButton(onClick = { viewModel.fetchMovies() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = "movie_list",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("movie_list") {
                        MovieListScreen(viewModel, currentTheme)
                    }
                    composable("rental_list") {
                        RentalScreen(viewModel, currentTheme)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(viewModel: MovieViewModel, theme: AppTheme) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val textColor = if (theme == AppTheme.LIGHT) Color.Black else Color.White
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search movies...", color = textColor.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = textColor) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MovieRed,
                unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = MovieRed
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sortOrder == SortOrder.HIGH_RATING,
                onClick = { 
                    viewModel.onSortOrderChange(SortOrder.HIGH_RATING)
                    scope.launch { gridState.animateScrollToItem(0) }
                },
                label = { Text("High Rating") },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = textColor,
                    selectedLabelColor = Color.White,
                    selectedContainerColor = MovieRed
                )
            )
            FilterChip(
                selected = sortOrder == SortOrder.LOW_RATING,
                onClick = { 
                    viewModel.onSortOrderChange(SortOrder.LOW_RATING)
                    scope.launch { gridState.animateScrollToItem(0) }
                },
                label = { Text("Low Rating") },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = textColor,
                    selectedLabelColor = Color.White,
                    selectedContainerColor = MovieRed
                )
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MovieRed)
            }
        } else if (movies.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No movies found.", color = textColor, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.fetchMovies() },
                    colors = ButtonDefaults.buttonColors(containerColor = MovieRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Retry")
                }
                TextButton(onClick = { viewModel.loadMockData() }) {
                    Text("Use Mock Data", color = textColor.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(movies, key = { it.getStableId() }) { movie ->
                    MovieGridItem(movie, theme, onRentClick = { viewModel.rentMovie(movie) })
                }
            }
        }
    }
}

@Composable
fun MovieGridItem(movie: Movie, theme: AppTheme, onRentClick: () -> Unit) {
    val textColor = if (theme == AppTheme.LIGHT) Color.Black else Color.White
    var showDetail by remember { mutableStateOf(false) }

    if (showDetail) {
        MovieDetailDialog(movie = movie, theme = theme, onDismiss = { showDetail = false })
    }

    Card(
        modifier = Modifier.padding(8.dp).aspectRatio(0.65f).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = movie.fullPosterUrl,
                    placeholder = painterResource(R.drawable.movie_placeholder),
                    error = painterResource(R.drawable.image_error),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                RatingBadge(rating = movie.displayRating, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = movie.title ?: "",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onRentClick,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MovieRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Rent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showDetail = true },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Detail", color = textColor, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, null, tint = MovieGold, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = "$rating", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RatingDisplay(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val starColor = if (index < (rating / 2).toInt()) MovieGold else Color.Gray.copy(alpha = 0.5f)
            Icon(
                Icons.Default.Star, 
                contentDescription = null, 
                tint = starColor, 
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "$rating", color = MovieGold, fontSize = 12.sp)
    }
}

@Composable
fun MovieDetailDialog(movie: Movie, theme: AppTheme, onDismiss: () -> Unit) {
    val textColor = if (theme == AppTheme.LIGHT) Color.Black else Color.White
    val bgColor = if (theme == AppTheme.LIGHT) Color.White else Color(0xFF1A1A1A)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AsyncImage(
                        model = movie.fullPosterUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = movie.title ?: "", color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    RatingDisplay(rating = movie.displayRating)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Overview", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = movie.overview ?: "No description available.", color = textColor.copy(alpha = 0.8f), fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Cast", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(movie.cast) { member ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                                AsyncImage(
                                    model = member.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Text(member.name, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                                Text(member.role, color = textColor.copy(alpha = 0.6f), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("The Plot", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = "Experience the full journey of ${movie.title}. Discover the secrets behind the scenes and the narrative that defines this cinematic masterpiece.", 
                        color = textColor.copy(alpha = 0.8f), 
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Gallery", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(movie.galleryStills) { stillUrl ->
                            AsyncImage(
                                model = stillUrl,
                                contentDescription = null,
                                modifier = Modifier.size(200.dp, 130.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun RentalScreen(viewModel: MovieViewModel, theme: AppTheme) {
    val rentals by viewModel.rentals.collectAsState()
    val totalPrice = rentals.sumOf { it.days * 450.0 } 
    val textColor = if (theme == AppTheme.LIGHT) Color.Black else Color.White

    Column(modifier = Modifier.fillMaxSize()) {
        if (rentals.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No active rentals.", color = textColor)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rentals, key = { it.id }) { rental ->
                    RentalItem(
                        rental = rental,
                        theme = theme,
                        onIncrease = { viewModel.updateRentalDays(rental, true) },
                        onDecrease = { viewModel.updateRentalDays(rental, false) },
                        onRemove = { viewModel.removeRental(rental) }
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MovieRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Rental Price", color = Color.White, fontSize = 16.sp)
                Text("₹$totalPrice", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RentalItem(rental: Rental, theme: AppTheme, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    val textColor = if (theme == AppTheme.LIGHT) Color.Black else Color.White
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = rental.posterUrl,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rental.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecrease) { Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor, modifier = Modifier.size(20.dp)) }
                    Text(text = "${rental.days}", color = textColor, fontSize = 14.sp)
                    IconButton(onClick = onIncrease) { Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor, modifier = Modifier.size(18.dp)) }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = MovieRed, modifier = Modifier.size(24.dp))
            }
        }
    }
}
