package com.bridge.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bridge.core.network.ConnectionItemDto
import com.bridge.core.network.IntentDto
import com.bridge.core.network.MemberCardDto
import com.bridge.core.network.CohortDto
import com.bridge.core.network.CohortPostDto
import com.bridge.core.network.PodDto
import com.bridge.core.network.ConversationDto
import com.bridge.core.network.MessageItemDto
import com.bridge.core.network.ReferralDto
import com.bridge.core.network.ReferralQuotaDto
import com.bridge.core.network.NotificationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommunityState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val intents: List<IntentDto> = emptyList(),
    val members: List<MemberCardDto> = emptyList(),
    val connections: List<ConnectionItemDto> = emptyList(),
    val cohorts: List<CohortDto> = emptyList(),
    val selectedCohort: CohortDto? = null,
    val cohortPosts: List<CohortPostDto> = emptyList(),
    val pods: List<PodDto> = emptyList(),
    val conversations: List<ConversationDto> = emptyList(),
    val selectedConversation: ConversationDto? = null,
    val selectedPod: PodDto? = null,
    val messages: List<MessageItemDto> = emptyList(),
    val referrals: List<ReferralDto> = emptyList(),
    val referralQuota: ReferralQuotaDto? = null,
    val notifications: List<NotificationDto> = emptyList(),
    val notice: String? = null,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(private val repository: OnboardingRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(CommunityState())
    val state = mutableState.asStateFlow()

    fun load() {
        if (mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            runCatching {
                val intents = repository.loadIntents()
                val members = repository.discover()
                val connections = repository.loadConnections()
                val cohorts = repository.loadCohorts()
                val pods = repository.loadPods()
                val conversations = repository.loadConversations()
                val referrals = repository.loadReferrals()
                val quota = repository.loadReferralQuota()
                CommunityState(intents = intents, members = members, connections = connections, cohorts = cohorts, pods = pods, conversations = conversations, referrals = referrals, referralQuota = quota)
            }.onSuccess { loaded ->
                mutableState.value = loaded
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(loading = false, error = error.message ?: "Bridge could not load right now.")
            }
        }
    }

    fun toggleIntent(intent: IntentDto) {
        val selected = mutableState.value.intents.filter { it.selected }.map { it.id }.toMutableSet()
        if (intent.selected) selected.remove(intent.id) else if (selected.size < 3) selected.add(intent.id)
        else {
            mutableState.value = mutableState.value.copy(notice = "Choose up to three intents.")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, notice = null)
            runCatching { repository.saveIntents(selected.toList()) }
                .onSuccess { mutableState.value = mutableState.value.copy(saving = false, intents = it, notice = "Your intents are saved.") }
                .onFailure { mutableState.value = mutableState.value.copy(saving = false, error = it.message ?: "Could not save intents.") }
        }
    }

    fun connect(member: MemberCardDto) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, notice = null)
            runCatching { repository.requestConnection(member.userId) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(saving = false, notice = "Connection request sent.")
                    loadConnections()
                }
                .onFailure { mutableState.value = mutableState.value.copy(saving = false, error = it.message ?: "Could not send request.") }
        }
    }

    fun searchDiscover(query: String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            runCatching { repository.discover(query = query) }
                .onSuccess { mutableState.value = mutableState.value.copy(loading = false, members = it) }
                .onFailure { mutableState.value = mutableState.value.copy(loading = false, error = it.message ?: "Search is unavailable.") }
        }
    }

    fun block(member: MemberCardDto) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null)
            runCatching { repository.blockMember(member.userId) }
                .onSuccess { mutableState.value = mutableState.value.copy(saving = false, members = mutableState.value.members.filterNot { it.userId == member.userId }, notice = "Member blocked.") }
                .onFailure { mutableState.value = mutableState.value.copy(saving = false, error = it.message ?: "Could not block member.") }
        }
    }

    fun report(member: MemberCardDto) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null)
            runCatching { repository.reportMember(member.userId, "OTHER", "Reported from Bridge Discover") }
                .onSuccess { mutableState.value = mutableState.value.copy(saving = false, notice = "Report submitted for review.") }
                .onFailure { mutableState.value = mutableState.value.copy(saving = false, error = it.message ?: "Could not send report.") }
        }
    }

    fun refreshConnections() = loadConnections()

    fun loadNotifications() {
        viewModelScope.launch { runCatching { repository.loadNotifications() }.onSuccess { mutableState.value = mutableState.value.copy(notifications = it, error = null) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Could not load notifications.") } }
    }
    fun markNotificationRead(id: String) {
        viewModelScope.launch { runCatching { repository.markNotificationRead(id); repository.loadNotifications() }.onSuccess { mutableState.value = mutableState.value.copy(notifications = it) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Could not update notification.") } }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            runCatching { repository.loadConnections() }
                .onSuccess { mutableState.value = mutableState.value.copy(connections = it) }
                .onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Could not load connections.") }
        }
    }

    fun decide(request: ConnectionItemDto, accept: Boolean) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, notice = null)
            runCatching { repository.decideConnection(request.id, accept) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(saving = false, notice = if (accept) "You are connected." else "Request declined.")
                    loadConnections()
                }
                .onFailure { mutableState.value = mutableState.value.copy(saving = false, error = it.message ?: "Could not update request.") }
        }
    }

    fun createCohort(name: String, topic: String, description: String) = mutate("Cohort created.") { repository.createCohort(name, topic, description); repository.loadCohorts() to { s, v -> s.copy(cohorts = v) } }
    fun joinCohort(cohort: CohortDto) = mutate("You joined ${cohort.name}.") { repository.joinCohort(cohort.id); repository.loadCohorts() to { s, v -> s.copy(cohorts = v) } }
    fun selectCohort(cohort: CohortDto) {
        mutableState.value = mutableState.value.copy(selectedCohort = cohort, cohortPosts = emptyList(), error = null)
        viewModelScope.launch { runCatching { repository.loadCohortPosts(cohort.id) }.onSuccess { mutableState.value = mutableState.value.copy(cohortPosts = it) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }
    fun closeCohort() { mutableState.value = mutableState.value.copy(selectedCohort = null, cohortPosts = emptyList()) }
    fun postToCohort(cohort: CohortDto, body: String) {
        viewModelScope.launch { runCatching { repository.createCohortPost(cohort.id, body) }.onSuccess { selectCohort(cohort) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }
    fun createPod(name: String, goal: String, discoverable: Boolean) = mutate("Your Pod is ready.") { repository.createPod(name, goal, discoverable); repository.loadPods() to { s, v -> s.copy(pods = v) } }
    fun joinPod(pod: PodDto) = mutate("You joined ${pod.name}.") { repository.joinPod(pod.id); repository.loadPods() to { s, v -> s.copy(pods = v) } }
    fun closeConversation() {
        mutableState.value = mutableState.value.copy(selectedConversation = null, messages = emptyList())
    }
    fun selectConversation(conversation: ConversationDto) {
        mutableState.value = mutableState.value.copy(selectedConversation = conversation, messages = emptyList(), error = null)
        viewModelScope.launch { runCatching { repository.loadConversationMessages(conversation.id) }.onSuccess { mutableState.value = mutableState.value.copy(messages = it) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }
    fun sendDirect(conversation: ConversationDto, body: String) {
        viewModelScope.launch { runCatching { repository.sendDirectMessage(conversation.memberId, body) }.onSuccess { selectConversation(conversation); loadConversations() }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }
    fun closePod() {
        mutableState.value = mutableState.value.copy(selectedPod = null, messages = emptyList())
    }
    fun selectPod(pod: PodDto) {
        mutableState.value = mutableState.value.copy(selectedPod = pod, messages = emptyList(), error = null)
        viewModelScope.launch { runCatching { repository.loadPodMessages(pod.id) }.onSuccess { mutableState.value = mutableState.value.copy(messages = it) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }
    fun sendPod(pod: PodDto, body: String) {
        viewModelScope.launch { runCatching { repository.sendPodMessage(pod.id, body) }.onSuccess { selectPod(pod) }.onFailure { mutableState.value = mutableState.value.copy(error = it.message) } }
    }
    fun createReferral(recipient: ConnectionItemDto, company: String, role: String, message: String) = mutate("Referral request sent.") { repository.createReferral(recipient.memberId, company, role, message); val referrals = repository.loadReferrals(); val quota = repository.loadReferralQuota(); (referrals to quota) to { s, v -> s.copy(referrals = v.first, referralQuota = v.second) } }

    private fun loadConversations() { viewModelScope.launch { runCatching { repository.loadConversations() }.onSuccess { mutableState.value = mutableState.value.copy(conversations = it) } } }

    private fun <T> mutate(notice: String, work: suspend () -> Pair<T, (CommunityState, T) -> CommunityState>) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, notice = null)
            runCatching { work() }.onSuccess { (value, update) -> mutableState.value = update(mutableState.value, value).copy(saving = false, notice = notice) }
                .onFailure { mutableState.value = mutableState.value.copy(saving = false, error = it.message ?: "Bridge could not save this change.") }
        }
    }
}
