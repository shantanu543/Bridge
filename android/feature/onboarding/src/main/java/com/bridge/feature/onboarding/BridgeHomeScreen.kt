package com.bridge.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
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
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = BridgePalette.paper,
        bottomBar = {
            NavigationBar(containerColor = BridgePalette.white, tonalElevation = 0.dp) {
                destinations.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index; if (index == 4) viewModel.refreshConnections() },
                        icon = { Icon(listOf(Icons.Outlined.Search, Icons.Outlined.Groups, Icons.Outlined.Layers, Icons.Outlined.Forum, Icons.Outlined.PersonOutline)[index], contentDescription = null) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium) },
                    )
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
                    Text("RIGHT PEOPLE · REAL INTENT · NO NOISE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.25.sp)
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
private fun DiscoverContent(state: CommunityState, viewModel: CommunityViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    FeatureHeader("Discover", "Find people moving in your direction.")
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = true, onClick = {}, label = { Text("For you") })
        FilterChip(selected = false, onClick = {}, label = { Text("Intent matches") })
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text("Set your intent", style = MaterialTheme.typography.titleLarge)
        Text(
            "Selected ${state.intents.count { it.selected }}/3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    state.intents.sortedByDescending { it.selected }.forEach { intent ->
            FilterChip(selected = intent.selected, onClick = { viewModel.toggleIntent(intent) }, label = { Text(intent.label) })
        }
    }
    Text("Choose up to 3 · Better matches start with intent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            query,
            { query = it },
            placeholder = { Text("Search people or skills") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = { viewModel.searchDiscover(query) }, shape = MaterialTheme.shapes.medium) { Text("Search") }
    }
    Text("Recommended for you", style = MaterialTheme.typography.titleLarge, color = BridgePalette.navy, modifier = Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(8.dp))
    if (state.members.isEmpty()) {
        Card(Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.bridge.core.designsystem.BridgePalette.lavender)) {
            Column(Modifier.padding(18.dp)) {
                Text("Your network is taking shape", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("We’ll show discoverable member profiles that match your interests here.")
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.members, key = { it.userId }) { member ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgePalette.white), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    var menuExpanded by remember(member.userId) { mutableStateOf(false) }
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Surface(Modifier.size(50.dp), shape = CircleShape, color = BridgePalette.lavenderStrong) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Text(member.fullName.split(' ').mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(member.fullName.removePrefix("Demo "), style = MaterialTheme.typography.titleMedium, color = BridgePalette.ink)
                                member.headline?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConnectionsContent(state: CommunityState, viewModel: CommunityViewModel) {
    Text("Connections", style = MaterialTheme.typography.headlineMedium)
    Text("Build meaningful one-to-one relationships.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    if (state.connections.isEmpty()) {
        Text("No connections yet. Send a request from Discover to start a meaningful conversation.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.connections, key = { it.id }) { request ->
                Card(Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.bridge.core.designsystem.BridgePalette.lavender)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(request.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(if (request.incoming) "Incoming request" else "Request sent")
                        Text(request.status.lowercase().replaceFirstChar { it.uppercase() })
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
private fun CohortsContent(state: CommunityState, viewModel: CommunityViewModel) {
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
    FeatureHeader("Cohorts", "Join topic-based communities around your craft.")
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.cohorts, key = { it.id }) { cohort ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgePalette.white), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
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
private fun PodsContent(state: CommunityState, viewModel: CommunityViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var discoverable by rememberSaveable { mutableStateOf(false) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    FeatureHeader("Pods", "Private, focused groups with up to 10 people.")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { createOpen = true }) { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Create Pod") }
    }
    AssistChip(onClick = {}, label = { Text("My Pods  ·  4-week cycles") })
    state.selectedPod?.let { pod ->
        Text("${pod.name} messages", style = MaterialTheme.typography.titleMedium)
        state.messages.forEach { Text(it.body, modifier = Modifier.padding(vertical = 3.dp)) }
        var message by rememberSaveable(pod.id) { mutableStateOf("") }
        Row {
            OutlinedTextField(message, { message = it }, label = { Text("Pod message") }, modifier = Modifier.weight(1f))
            Button(enabled = message.isNotBlank(), onClick = { viewModel.sendPod(pod, message); message = "" }) { Text("Send") }
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.pods, key = { it.id }) { pod ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BridgePalette.white), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
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
private fun ChatContent(state: CommunityState, viewModel: CommunityViewModel) {
    FeatureHeader("Connect & chat", "Continue a conversation with your connections.")
    state.selectedConversation?.let { conversation ->
        Text(conversation.fullName, style = MaterialTheme.typography.titleMedium)
        state.messages.forEach { message ->
            val mine = message.senderId != conversation.memberId
            Card(
                Modifier.fillMaxWidth(if (mine) 0.86f else 0.92f).padding(vertical = 3.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = if (mine) com.bridge.core.designsystem.BridgePalette.lavenderStrong else com.bridge.core.designsystem.BridgePalette.white,
                ),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(if (mine) "You" else conversation.fullName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(message.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        var draft by rememberSaveable(conversation.id) { mutableStateOf("") }
        Row {
            OutlinedTextField(draft, { draft = it }, label = { Text("Message") }, modifier = Modifier.weight(1f))
            Button(enabled = draft.isNotBlank(), onClick = { viewModel.sendDirect(conversation, draft); draft = "" }) { Text("Send") }
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.conversations, key = { it.id }) { conversation ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(conversation.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(conversation.lastMessage ?: "Start a conversation")
                    OutlinedButton(onClick = { viewModel.selectConversation(conversation) }) { Text("Open chat") }
                }
            }
        }
    }
    if (state.conversations.isEmpty()) Text("Chat opens after a connection request is accepted.", modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ProfileContent(state: CommunityState, viewModel: CommunityViewModel, onPassport: () -> Unit, onSignOut: () -> Unit) {
    var referralOpen by rememberSaveable { mutableStateOf(false) }
    FeatureHeader("Profile & referrals", "Manage your Bridge Passport and grow through accepted connections.")
    Spacer(Modifier.height(8.dp))
    Card(Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.bridge.core.designsystem.BridgePalette.lavender)) {
        Column(Modifier.padding(16.dp)) {
            Text("Your Bridge Passport", style = MaterialTheme.typography.titleMedium)
            Text("Private profile · Bridge Score and professional evidence", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPassport) { Text("View Passport") }
        }
    }
    Spacer(Modifier.height(10.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Ask for Referral", style = MaterialTheme.typography.titleMedium)
            Text("${state.referralQuota?.remaining ?: 10} of 10 lifetime requests remaining", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { referralOpen = !referralOpen }) { Text(if (referralOpen) "Hide referral form" else "Request a referral") }
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
    Text("Referral activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
    state.referrals.forEach { Text("${it.company} · ${it.role} · ${it.status}") }
    Spacer(Modifier.height(8.dp))
    AssistChip(onClick = onSignOut, label = { Text("Sign out") })
}

@Composable
private fun NotificationsContent(state: CommunityState, viewModel: CommunityViewModel) {
    Text("Notifications", style = MaterialTheme.typography.headlineMedium)
    if (state.notifications.isEmpty()) {
        Text("You’re all caught up.", modifier = Modifier.padding(top = 12.dp))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
