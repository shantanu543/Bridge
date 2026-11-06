package com.bridge.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import android.view.View
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import com.bridge.core.designsystem.BridgePalette
import com.bridge.core.designsystem.BridgeLogoMark

private val destinations = listOf("Discover", "Cohorts", "Pods", "Chat", "Profile")

@Composable
fun BridgeHomeScreen(
    viewModel: CommunityViewModel,
    isLocalDemo: Boolean = false,
    onPassport: () -> Unit,
    onSignOut: () -> Unit,
) {
    val systemView = LocalView.current
    SideEffect {
        (systemView.context as? Activity)?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.rgb(246, 247, 252)
            window.navigationBarColor = android.graphics.Color.rgb(255, 255, 255)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    val state by viewModel.state.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    val overlayOpen = state.selectedConversation != null || state.selectedPod != null || state.selectedCohort != null || selected >= 5
    BackHandler(enabled = overlayOpen) {
        when {
            state.selectedConversation != null -> viewModel.closeConversation()
            state.selectedPod != null -> viewModel.closePod()
            state.selectedCohort != null -> viewModel.closeCohort()
            selected >= 5 -> selected = 0
        }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = BridgePalette.paper,
        bottomBar = {
            Column {
            HorizontalDivider(color = BridgePalette.line, thickness = 1.dp)
            NavigationBar(containerColor = BridgePalette.white, tonalElevation = 0.dp) {
                destinations.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index; if (index == 4) viewModel.refreshConnections() },
                        icon = { Icon(listOf(Icons.Outlined.Search, Icons.Outlined.Groups, Icons.Outlined.Layers, Icons.Outlined.Forum, Icons.Outlined.PersonOutline)[index], contentDescription = null) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BridgePalette.violet,
                            selectedTextColor = BridgePalette.violet,
                            indicatorColor = BridgePalette.lavender,
                            unselectedIconColor = BridgePalette.muted,
                            unselectedTextColor = BridgePalette.muted,
                        ),
                    )
                }
            }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                BridgeLogoMark(Modifier.size(width = 36.dp, height = 30.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bridge", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BridgePalette.navy)
                }
                IconButton(onClick = { selected = 5; viewModel.refreshConnections() }) {
                    Icon(Icons.Outlined.PeopleAlt, contentDescription = "Connections", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { selected = 6; viewModel.loadNotifications() }) {
                    BadgedBox(badge = { if (state.notifications.any { it.readAt == null }) Badge(containerColor = BridgePalette.violetBright) }) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (isLocalDemo) {
                Surface(shape = MaterialTheme.shapes.small, color = BridgePalette.lavender, modifier = Modifier.padding(top = 4.dp)) {
                    Text("LOCAL DEMO  ·  Synthetic data", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = BridgePalette.violet, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            when {
                state.loading -> CircularProgressIndicator()
                selected == 0 -> DiscoverContent(state, viewModel)
                selected == 1 -> CohortsContent(state, viewModel)
                selected == 2 -> PodsContent(state, viewModel)
                selected == 3 -> ChatContent(state, viewModel)
                selected == 4 -> ProfileContent(state, viewModel, onPassport, onSignOut)
                selected == 5 -> ConnectionsContent(state, viewModel)
                selected == 6 -> NotificationsContent(state, viewModel)
                else -> DiscoverContent(state, viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.DiscoverContent(state: CommunityState, viewModel: CommunityViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("For you") }
    val topics = listOf("For you", "System design", "Job switch")
    FeatureHeader("Discover", "Find the right people for what matters now.")
    Text("What brings you to Bridge?", style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
    Text("Select up to 3 intents to find the right people.", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.intents.sortedByDescending { it.selected }.forEach { intent ->
            val label = if (intent.label.startsWith("#")) intent.label else "#${intent.label.replace(" ", "")}"
            BridgeChoiceChip(label, intent.selected) { viewModel.toggleIntent(intent) }
        }
    }
    Text(
        "Selected ${state.intents.count { it.selected }}/3",
        style = MaterialTheme.typography.labelMedium,
        color = BridgePalette.violet,
        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        topics.forEach { item ->
            BridgeChoiceChip(item, topic == item) { topic = item }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        query,
        { query = it },
        placeholder = { Text("People, skills or interests") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = BridgePalette.muted) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { viewModel.searchDiscover(query) }),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Recommended for you", style = MaterialTheme.typography.titleLarge, color = BridgePalette.navy, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    if (state.members.isEmpty()) {
        Card(Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.bridge.core.designsystem.BridgePalette.lavender)) {
            Column(Modifier.padding(18.dp)) {
                Text("Your network is taking shape", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("We’ll show discoverable member profiles that match your interests here.")
            }
        }
    } else {
        val visibleMembers = state.members.filter { member ->
            if (topic == "For you") true
            else {
                val needle = topic.lowercase()
                member.intents.any { it.contains(needle, ignoreCase = true) } ||
                    member.skills.any { it.contains(needle, ignoreCase = true) } ||
                    (member.headline?.contains(needle, ignoreCase = true) == true)
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visibleMembers, key = { it.userId }) { member ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
                    border = BorderStroke(1.dp, BridgePalette.line),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    var menuExpanded by remember(member.userId) { mutableStateOf(false) }
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(member.fullName, 52.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(displayName(member.fullName), style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
                                member.headline?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted) }
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More profile actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("Report profile") }, onClick = { menuExpanded = false; viewModel.report(member) })
                                    DropdownMenuItem(text = { Text("Block profile") }, onClick = { menuExpanded = false; viewModel.block(member) })
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = BridgePalette.muted, modifier = Modifier.size(15.dp))
                            Text(member.location ?: "Bridge member", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (member.skills.isNotEmpty()) {
                                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(member.skills.take(2).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        if (member.intents.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            member.intents.forEach { intent ->
                                Surface(shape = CircleShape, color = BridgePalette.lavender, modifier = Modifier.padding(top = 8.dp, end = 2.dp)) {
                                    Text(intent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = BridgePalette.violet)
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(top = 14.dp, bottom = 12.dp), color = BridgePalette.line)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            when (member.relationship) {
                                "CONNECTED" -> Surface(shape = MaterialTheme.shapes.small, color = BridgePalette.successSoft) { Text("Connected", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, color = BridgePalette.success) }
                                "REQUEST_SENT" -> Surface(shape = MaterialTheme.shapes.small, color = BridgePalette.lavender) { Text("Request sent", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, color = BridgePalette.violet) }
                                "REQUEST_RECEIVED" -> Surface(shape = MaterialTheme.shapes.small, color = BridgePalette.peach) { Text("Incoming request", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, color = BridgePalette.ink) }
                                else -> Button(enabled = !state.saving, onClick = { viewModel.connect(member) }, shape = MaterialTheme.shapes.small) {
                                    Icon(Icons.Outlined.PeopleAlt, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Connect")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHeader(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = BridgePalette.navy)
        Spacer(Modifier.height(3.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = BridgePalette.muted)
    }
}

@Composable
private fun ColumnScope.ConnectionsContent(state: CommunityState, viewModel: CommunityViewModel) {
    FeatureHeader("Connections", "Build meaningful one-to-one relationships.")
    if (state.connections.isEmpty()) {
        Text("No connections yet. Send a request from Discover to start a meaningful conversation.", color = BridgePalette.muted)
    } else {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.connections, key = { it.id }) { request ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
                    border = BorderStroke(1.dp, BridgePalette.line),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(request.fullName, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(displayName(request.fullName), style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
                                Text(if (request.incoming) "Incoming request" else "Request sent", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted)
                            }
                        }
                        if (request.incoming && request.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = !state.saving, onClick = { viewModel.decide(request, true) }) { Text("Accept") }
                                OutlinedButton(enabled = !state.saving, onClick = { viewModel.decide(request, false) }) { Text("Decline") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.CohortsContent(state: CommunityState, viewModel: CommunityViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    state.selectedCohort?.let { cohort ->
        OutlinedButton(onClick = viewModel::closeCohort) { Text("← All Cohorts") }
        Text(cohort.name, style = MaterialTheme.typography.titleLarge)
        Text("${cohort.topic} · ${cohort.members} members")
        Text(cohort.description)
        state.cohortPosts.forEach { Text(it.body, modifier = Modifier.padding(vertical = 6.dp)) }
        var post by rememberSaveable(cohort.id) { mutableStateOf("") }
        Row {
            OutlinedTextField(post, { post = it }, label = { Text("Share with Cohort") }, modifier = Modifier.weight(1f))
            Button(enabled = post.length >= 1, onClick = { viewModel.postToCohort(cohort, post); post = "" }) { Text("Post") }
        }
        return
    }
    FeatureHeader("Cohorts", "Larger communities built around shared interests.")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { createOpen = true }) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Create cohort")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = true, onClick = {}, label = { Text("Discover") })
        FilterChip(selected = false, onClick = {}, label = { Text("My Cohorts") })
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.cohorts, key = { it.id }) { cohort ->
            Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
                    border = BorderStroke(1.dp, BridgePalette.line),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), shape = MaterialTheme.shapes.medium, color = BridgePalette.peach) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Icon(Icons.Outlined.Groups, contentDescription = null, tint = BridgePalette.violet)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cohort.name.removePrefix("Demo · "), style = MaterialTheme.typography.titleMedium, color = BridgePalette.ink)
                            Text("${cohort.topic}  ·  ${cohort.members} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(cohort.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (!cohort.joined) TextButton(enabled = !state.saving, onClick = { viewModel.joinCohort(cohort) }) { Text("Join cohort"); Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        else TextButton(onClick = { viewModel.selectCohort(cohort) }) { Text("Open discussions"); Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
    if (createOpen) AlertDialog(
        onDismissRequest = { createOpen = false },
        title = { Text("Create a Cohort") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(topic, { topic = it }, label = { Text("Topic") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = !state.saving && name.length >= 3 && topic.length >= 2 && description.length >= 10, onClick = {
                viewModel.createCohort(name, topic, description)
                name = ""; topic = ""; description = ""; createOpen = false
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { createOpen = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ColumnScope.PodsContent(state: CommunityState, viewModel: CommunityViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var discoverable by rememberSaveable { mutableStateOf(false) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    val openPod = state.selectedPod
    if (openPod != null) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = viewModel::closePod) { Text("Back") }
            Column {
                Text(openPod.name.removePrefix("Demo · "), style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
                Text("${openPod.members}/10 members", style = MaterialTheme.typography.labelSmall, color = BridgePalette.muted)
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages, key = { it.id }) { message ->
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = BridgePalette.white,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(message.body, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium, color = BridgePalette.ink)
                }
            }
        }
        var message by rememberSaveable(openPod.id) { mutableStateOf("") }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                message,
                { message = it },
                placeholder = { Text("Pod message") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = message.isNotBlank(),
                onClick = { viewModel.sendPod(openPod, message); message = "" },
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Icon(Icons.Outlined.ArrowForward, contentDescription = "Send") }
        }
        return
    }
    FeatureHeader("Pods", "Small, focused groups that move together.")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { createOpen = true }) { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Create Pod") }
    }
    AssistChip(onClick = {}, label = { Text("My Pods  ·  4-week cycles") })
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.pods, key = { it.id }) { pod ->
            Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
                    border = BorderStroke(1.dp, BridgePalette.line),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), shape = MaterialTheme.shapes.medium, color = BridgePalette.lavender) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Icon(Icons.Outlined.Layers, contentDescription = null, tint = BridgePalette.violet) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pod.name.removePrefix("Demo · "), style = MaterialTheme.typography.titleMedium, color = BridgePalette.ink)
                            Text("${pod.members}/10 members  ·  4-week cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(pod.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (!pod.isMember && pod.discoverable) TextButton(enabled = !state.saving, onClick = { viewModel.joinPod(pod) }) { Text("Join Pod") }
                        if (pod.isMember) TextButton(onClick = { viewModel.selectPod(pod) }) { Text("Open Pod chat"); Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
    if (createOpen) AlertDialog(
        onDismissRequest = { createOpen = false },
        title = { Text("Create a Pod") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Pod name") }, singleLine = true)
                OutlinedTextField(goal, { goal = it }, label = { Text("Shared goal") }, minLines = 2)
                FilterChip(selected = discoverable, onClick = { discoverable = !discoverable }, label = { Text(if (discoverable) "Request to join" else "Invite only") })
            }
        },
        confirmButton = {
            TextButton(enabled = !state.saving && name.length >= 3 && goal.length >= 10, onClick = {
                viewModel.createPod(name, goal, discoverable); name = ""; goal = ""; createOpen = false
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { createOpen = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ColumnScope.ChatContent(state: CommunityState, viewModel: CommunityViewModel) {
    val conversation = state.selectedConversation
    if (conversation == null) {
        FeatureHeader("Chat", "Start a 1:1 conversation after a connection is accepted.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.conversations, key = { it.id }) { item ->
                Card(
                    onClick = { viewModel.selectConversation(item) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
                    border = BorderStroke(1.dp, BridgePalette.line),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(item.fullName, 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(displayName(item.fullName), style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
                            Text(item.lastMessage ?: "Start a conversation", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted, maxLines = 2)
                        }
                    }
                }
            }
        }
        if (state.conversations.isEmpty()) Text("Chat opens after a connection request is accepted.", color = BridgePalette.muted, modifier = Modifier.padding(top = 12.dp))
        return
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        TextButton(onClick = viewModel::closeConversation) { Text("Back") }
        Column {
            Text(displayName(conversation.fullName), style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
            Text("1:1", style = MaterialTheme.typography.labelSmall, color = BridgePalette.muted)
        }
    }
    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.messages, key = { it.id }) { message ->
            val mine = message.senderId != conversation.memberId
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (mine) 18.dp else 4.dp,
                        bottomEnd = if (mine) 4.dp else 18.dp,
                    ),
                    color = if (mine) BridgePalette.violet else BridgePalette.white,
                    modifier = Modifier.fillMaxWidth(0.82f),
                ) {
                    Text(
                        message.body,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = if (mine) BridgePalette.white else BridgePalette.ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
    var draft by rememberSaveable(conversation.id) { mutableStateOf("") }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            draft,
            { draft = it },
            placeholder = { Text("Type a message...") },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            singleLine = true,
        )
        Spacer(Modifier.width(8.dp))
        Button(
            enabled = draft.isNotBlank(),
            onClick = { viewModel.sendDirect(conversation, draft); draft = "" },
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.size(48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) { Icon(Icons.Outlined.ArrowForward, contentDescription = "Send") }
    }
}

@Composable
private fun ColumnScope.ProfileContent(state: CommunityState, viewModel: CommunityViewModel, onPassport: () -> Unit, onSignOut: () -> Unit) {
    var referralOpen by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
    FeatureHeader("Profile", "Your Bridge profile, score and referral requests.")
    Spacer(Modifier.height(8.dp))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BridgePalette.white),
        border = BorderStroke(1.dp, BridgePalette.line),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(56.dp), shape = CircleShape, color = BridgePalette.lavender) {
                    Box(contentAlignment = Alignment.Center) {
                        BridgeLogoMark(Modifier.size(28.dp), contentDescription = "Bridge")
                    }
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Your Bridge Passport", style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
                    Text("Private profile · score and professional evidence", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onPassport, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("View Passport") }
        }
    }
    Spacer(Modifier.height(10.dp))
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BridgePalette.lavender),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Ask for a referral", style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy)
            Text("A thoughtful ask can open the right door.", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted)
            Spacer(Modifier.height(8.dp))
            Text("${state.referralQuota?.remaining ?: 10} of 10 lifetime requests remaining", style = MaterialTheme.typography.labelLarge, color = BridgePalette.violet)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { referralOpen = !referralOpen }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text(if (referralOpen) "Hide referral form" else "Request a referral")
            }
        }
    }
    if (state.referralQuota?.remaining == 0) Text("You have reached the 10-request lifetime limit.")
    if (referralOpen) state.connections.filter { it.status == "ACCEPTED" }.forEach { connection ->
        var company by rememberSaveable(connection.id) { mutableStateOf("") }
        var role by rememberSaveable(connection.id) { mutableStateOf("") }
        var message by rememberSaveable(connection.id) { mutableStateOf("") }
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Ask ${connection.fullName} for a referral", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(company, { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(role, { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(message, { message = it }, label = { Text("Personal message") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !state.saving && (state.referralQuota?.remaining ?: 10) > 0 && company.length >= 2 && role.length >= 2 && message.length >= 20, onClick = { viewModel.createReferral(connection, company, role, message); company = ""; role = ""; message = "" }) { Text("Send request") }
            }
        }
    }
    Text("Referral activity", style = MaterialTheme.typography.titleMedium, color = BridgePalette.navy, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
    if (state.referrals.isEmpty()) {
        Text("Sent, accepted and declined requests will show up here.", style = MaterialTheme.typography.bodySmall, color = BridgePalette.muted)
    }
    state.referrals.forEach { referral ->
        Text("${referral.company} · ${referral.role} · ${referral.status}", style = MaterialTheme.typography.bodyMedium, color = BridgePalette.ink, modifier = Modifier.padding(vertical = 4.dp))
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Sign out") }
    Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BridgeChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(50),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BridgePalette.line,
            selectedBorderColor = BridgePalette.violet,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BridgePalette.white,
            labelColor = BridgePalette.ink,
            selectedContainerColor = BridgePalette.violet,
            selectedLabelColor = BridgePalette.white,
            selectedLeadingIconColor = BridgePalette.white,
        ),
    )
}

@Composable
private fun InitialsAvatar(name: String, size: androidx.compose.ui.unit.Dp) {
    Surface(Modifier.size(size), shape = CircleShape, color = BridgePalette.lavender) {
        Box(contentAlignment = Alignment.Center) {
            Text(initials(name), style = MaterialTheme.typography.titleMedium, color = BridgePalette.violet, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = BridgePalette.white,
    focusedContainerColor = BridgePalette.white,
    unfocusedBorderColor = BridgePalette.line,
    focusedBorderColor = BridgePalette.violet,
    cursorColor = BridgePalette.violet,
)

private fun displayName(name: String) = name.removePrefix("Demo ").removePrefix("Demo · ")

private fun initials(name: String): String =
    displayName(name).split(' ', '·').map { it.trim() }.filter { it.isNotEmpty() }.map { it.first().uppercase() }.take(2).joinToString("")

@Composable
private fun ColumnScope.NotificationsContent(state: CommunityState, viewModel: CommunityViewModel) {
    Text("Notifications", style = MaterialTheme.typography.headlineMedium)
    if (state.notifications.isEmpty()) {
        Text("You’re all caught up.", modifier = Modifier.padding(top = 12.dp))
        return
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.notifications, key = { it.id }) { notification ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium)
                    Text(notification.body)
                    if (notification.readAt == null) OutlinedButton(onClick = { viewModel.markNotificationRead(notification.id) }) { Text("Mark as read") }
                }
            }
        }
    }
}
