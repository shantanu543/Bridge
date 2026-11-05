package com.bridge.feature.onboarding

import com.bridge.core.datastore.SecureSessionStore
import com.bridge.core.datastore.StoredBridgeSession
import com.bridge.core.network.BridgeAuthApi
import com.bridge.core.network.BridgeScoreDto
import com.bridge.core.network.ConsentPolicyDto
import com.bridge.core.network.ConsentUpdateDto
import com.bridge.core.network.OnboardingStatusDto
import com.bridge.core.network.ProfileDto
import com.bridge.core.network.PassportDto
import com.bridge.core.network.RefreshDto
import com.bridge.core.network.IntentDto
import com.bridge.core.network.IntentSelectionDto
import com.bridge.core.network.MemberCardDto
import com.bridge.core.network.ConnectionItemDto
import com.bridge.core.network.ConnectionRequestDto
import com.bridge.core.network.ConnectionDecisionDto
import com.bridge.core.network.CohortDto
import com.bridge.core.network.CohortCreateDto
import com.bridge.core.network.CohortPostDto
import com.bridge.core.network.CohortPostCreateDto
import com.bridge.core.network.PodDto
import com.bridge.core.network.PodCreateDto
import com.bridge.core.network.MessageItemDto
import com.bridge.core.network.MessageCreateDto
import com.bridge.core.network.ConversationDto
import com.bridge.core.network.ReferralQuotaDto
import com.bridge.core.network.ReferralDto
import com.bridge.core.network.ReferralCreateDto
import com.bridge.core.network.ReferralDecisionDto
import com.bridge.core.network.MemberReportDto
import com.bridge.core.network.NotificationDto
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class OnboardingRepository @Inject constructor(
    private val api: BridgeAuthApi,
    private val sessionStore: SecureSessionStore,
) {
    suspend fun loadProfile(): ProfileDto = authorized(api::profile)

    suspend fun loadStatus(): OnboardingStatusDto = authorized(api::onboardingStatus)

    suspend fun loadPolicies(): List<ConsentPolicyDto> = authorized(api::consentPolicies)

    suspend fun saveProfile(profile: ProfileDto): ProfileDto =
        authorized { api.saveProfile(it, profile.toUpdateRequest()) }

    suspend fun confirmProfile(): ProfileDto = authorized(api::confirmProfile)

    suspend fun loadPassport(): PassportDto = authorized(api::passport)

    suspend fun loadBridgeScore(): BridgeScoreDto = authorized(api::bridgeScore)

    suspend fun loadIntents(): List<IntentDto> = authorized(api::intents)

    suspend fun saveIntents(ids: List<String>): List<IntentDto> =
        authorized { api.saveIntents(it, IntentSelectionDto(ids)) }

    suspend fun discover(query: String? = null, skill: String? = null): List<MemberCardDto> = authorized { api.discover(it, query, skill) }

    suspend fun loadConnections(): List<ConnectionItemDto> = authorized(api::connections)

    suspend fun requestConnection(memberId: String) = authorized {
        api.requestConnection(it, ConnectionRequestDto(memberId))
    }

    suspend fun decideConnection(requestId: String, accepted: Boolean) = authorized {
        api.decideConnection(it, requestId, ConnectionDecisionDto(if (accepted) "ACCEPTED" else "DECLINED"))
    }

    suspend fun loadCohorts(): List<CohortDto> = authorized(api::cohorts)
    suspend fun createCohort(name: String, topic: String, description: String) = authorized {
        api.createCohort(it, CohortCreateDto(name, topic, description))
    }
    suspend fun joinCohort(id: String) = authorized { api.joinCohort(it, id) }
    suspend fun loadCohortPosts(id: String): List<CohortPostDto> = authorized { api.cohortPosts(it, id) }
    suspend fun createCohortPost(id: String, body: String): CohortPostDto = authorized { api.createCohortPost(it, id, CohortPostCreateDto(body)) }
    suspend fun loadPods(): List<PodDto> = authorized(api::pods)
    suspend fun createPod(name: String, goal: String, discoverable: Boolean) = authorized {
        api.createPod(it, PodCreateDto(name = name, goal = goal, discoverable = discoverable))
    }
    suspend fun joinPod(id: String) = authorized { api.joinPod(it, id) }
    suspend fun loadPodMessages(id: String): List<MessageItemDto> = authorized { api.podMessages(it, id) }
    suspend fun sendPodMessage(id: String, body: String): MessageItemDto = authorized { api.sendPodMessage(it, id, MessageCreateDto(body)) }
    suspend fun loadConversations(): List<ConversationDto> = authorized(api::conversations)
    suspend fun loadConversationMessages(id: String): List<MessageItemDto> = authorized { api.conversationMessages(it, id) }
    suspend fun sendDirectMessage(memberId: String, body: String): MessageItemDto = authorized { api.sendDirectMessage(it, memberId, MessageCreateDto(body)) }
    suspend fun loadReferralQuota(): ReferralQuotaDto = authorized(api::referralQuota)
    suspend fun loadReferrals(): List<ReferralDto> = authorized(api::referrals)
    suspend fun createReferral(recipientId: String, company: String, role: String, message: String) = authorized {
        api.createReferral(it, ReferralCreateDto(recipientId, company, role, message))
    }
    suspend fun decideReferral(id: String, accepted: Boolean) = authorized {
        api.decideReferral(it, id, ReferralDecisionDto(if (accepted) "ACCEPTED" else "DECLINED"))
    }
    suspend fun blockMember(id: String) = authorized { api.blockMember(it, id) }
    suspend fun reportMember(id: String, reason: String, details: String?) = authorized { api.reportMember(it, id, MemberReportDto(reason, details)) }
    suspend fun loadNotifications(): List<NotificationDto> = authorized(api::notifications)
    suspend fun markNotificationRead(id: String) = authorized { api.markNotificationRead(it, id) }

    suspend fun recordConsents(
        acceptTerms: Boolean,
        acceptPrivacy: Boolean,
        providers: List<String>,
        resumeProcessing: Boolean,
    ) = authorized {
        api.recordConsents(
            it,
            ConsentUpdateDto(
                acceptTerms = acceptTerms,
                acceptPrivacy = acceptPrivacy,
                providerData = providers,
                resumeProcessing = resumeProcessing,
            ),
        )
    }

    private suspend fun <T> authorized(call: suspend (authorization: String) -> T): T {
        val current = sessionStore.read() ?: error("Sign in to continue.")
        return try {
            call("Bearer ${current.accessToken}")
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val refreshed = api.refresh(RefreshDto(current.refreshToken))
            val saved = persist(refreshed)
            call("Bearer ${saved.accessToken}")
        }
    }

    private suspend fun persist(session: com.bridge.core.network.SessionDto): StoredBridgeSession {
        sessionStore.save(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            expiresInSeconds = session.expiresIn,
            userId = session.userId,
            onboardingComplete = session.onboardingComplete,
            localDemo = session.localDemo,
        )
        return sessionStore.read() ?: error("Could not persist the Bridge session securely.")
    }
}
