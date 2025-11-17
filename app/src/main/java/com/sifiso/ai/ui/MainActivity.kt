package com.sifiso.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sifiso.ai.SifisoApplication
import com.sifiso.ai.privacy.TransparencyLogger
import kotlinx.coroutines.launch

/**
 * Main Activity for SIFISO Android Prototype
 * Demonstrates AI agents, sensor monitoring, and transparency logging
 */
class MainActivity : ComponentActivity() {

    private lateinit var app: SifisoApplication
    private val requiredPermissions = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeAISystem()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = application as SifisoApplication

        // Check permissions
        if (hasRequiredPermissions()) {
            initializeAISystem()
        } else {
            requestPermissions()
        }

        setContent {
            SifisoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(app)
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun initializeAISystem() {
        // AI system is already initialized in SifisoApplication
        // Start any additional services here if needed
    }
}

@Composable
fun SifisoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(app: SifisoApplication) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Wellness", "Career", "Transparency")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIFISO AI Phone") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DashboardTab(app)
                1 -> WellnessTab(app)
                2 -> CareerTab(app)
                3 -> TransparencyTab(app)
            }
        }
    }
}

@Composable
fun DashboardTab(app: SifisoApplication) {
    val sensorData by app.sensorFusion.sensorData.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SIFISO AI Phone - Prototype", style = MaterialTheme.typography.headlineMedium)

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Real-time Sensor Data", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Heart Rate: ${sensorData.biosensors.heartRate} bpm")
                Text("Stress Score: ${"%.2f".format(sensorData.biosensors.stressScore)}")
                Text("Activity: ${sensorData.motion.currentActivity}")
                Text("Ambient Light: ${sensorData.environmental.ambientLight} lux")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Agents Status", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("✓ Wellness Agent - Active")
                Text("✓ Career Agent - Active")
                Text("○ Travel Agent - Not implemented")
                Text("○ Finance Agent - Not implemented")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        app.sensorFusion.simulateStressEvent()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Simulate Stress")
            }

            Button(
                onClick = {
                    scope.launch {
                        app.sensorFusion.simulateCalm()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Simulate Calm")
            }
        }

        Button(
            onClick = {
                scope.launch {
                    app.agentOrchestrator.orchestrateRoutine("morning_routine", emptyMap())
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Morning Routine")
        }
    }
}

@Composable
fun WellnessTab(app: SifisoApplication) {
    val sensorData by app.sensorFusion.sensorData.collectAsState()
    val biosensors = sensorData.biosensors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Wellness Monitoring", style = MaterialTheme.typography.headlineMedium)

        WellnessCard("Stress Level", "${(biosensors.stressScore * 100).toInt()}%")
        WellnessCard("Heart Rate", "${biosensors.heartRate} bpm")
        WellnessCard("HRV (RMSSD)", "${biosensors.hrvRmssd.toInt()} ms")
        WellnessCard("Blood Oxygen", "${biosensors.bloodOxygen}%")
        WellnessCard("Hydration", "${(biosensors.hydrationLevel * 100).toInt()}%")
    }
}

@Composable
fun CareerTab(app: SifisoApplication) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Career Development", style = MaterialTheme.typography.headlineMedium)

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Top Trends", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• On-Device AI (45% growth)")
                Text("• Privacy-First Technology (42% growth)")
                Text("• Edge Computing (38% growth)")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Skill Recommendations", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Optimization")
                Text("• Embedded Systems")
                Text("• Privacy Engineering")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent Opportunities", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Senior AI Engineer - TechCorp (88% match)")
                Text("Privacy Engineer - SecureAI (82% match)")
            }
        }
    }
}

@Composable
fun TransparencyTab(app: SifisoApplication) {
    val logs by app.transparencyLogger.recentLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Transparency Log", style = MaterialTheme.typography.headlineMedium)
        Text("All AI decisions and data access", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                LogEntryCard(log)
            }
        }
    }
}

@Composable
fun WellnessCard(label: String, value: String) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun LogEntryCard(log: TransparencyLogger.LogEntry) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(log.agentName, style = MaterialTheme.typography.labelMedium)
                Text(log.type.toString(), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(log.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
