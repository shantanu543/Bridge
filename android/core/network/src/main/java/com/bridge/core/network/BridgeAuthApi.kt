package com.bridge.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Header

@Serializable
data class GoogleChallengeDto(
    @SerialName("challenge_id") val challengeId: String,
    val nonce: String,
)

@Serializable
data class GoogleExchangeRequest(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("id_token") val idToken: String,
    @SerialName("device_label") val deviceLabel: String,
)

@Serializable
data class SessionDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean,
    @SerialName("local_demo") val localDemo: Boolean = false,
)

@Serializable
data class RefreshDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class EmailRegistrationDto(
    val email: String,
    val password: String,
)

@Serializable
data class EmailLoginDto(
    val email: String,
    val password: String,
    @SerialName("device_label") val deviceLabel: String,
)

@Serializable
data class EmailVerificationDto(
    val email: String,
    val code: String,
    @SerialName("device_label") val deviceLabel: String,
)

@Serializable
data class PasswordResetDto(val email: String)

@Serializable
data class PasswordResetConfirmDto(
    val email: String,
    val code: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class AuthNoticeDto(
    val message: String,
    @SerialName("verification_required") val verificationRequired: Boolean = false,
)

@Serializable
data class CurrentUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean,
    @SerialName("account_status") val accountStatus: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WorkExperienceDto(
    val role: String,
    val company: String,
    val location: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_current") val isCurrent: Boolean = false,
    val description: String? = null,
)

@Serializable
data class EducationDto(
    val institution: String,
    val qualification: String? = null,
    @SerialName("field_of_study") val fieldOfStudy: String? = null,
    @SerialName("start_year") val startYear: Int? = null,
    @SerialName("end_year") val endYear: Int? = null,
)

@Serializable
data class PortfolioLinkDto(val label: String, val url: String)

@Serializable
data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String? = null,
    val headline: String? = null,
    @SerialName("current_role") val currentRole: String? = null,
    @SerialName("current_company") val currentCompany: String? = null,
    val summary: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    val location: String? = null,
    val timezone: String? = null,
    val visibility: String = "private",
    @SerialName("work_history") val workHistory: List<WorkExperienceDto> = emptyList(),
    val education: List<EducationDto> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<Map<String, String>> = emptyList(),
    val achievements: List<Map<String, String>> = emptyList(),
    val certifications: List<Map<String, String>> = emptyList(),
    @SerialName("portfolio_links") val portfolioLinks: List<PortfolioLinkDto> = emptyList(),
)

@Serializable
data class ProfileFieldSourceDto(
    @SerialName("field_key") val fieldKey: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("imported_at") val importedAt: String? = null,
    @SerialName("user_confirmed") val userConfirmed: Boolean,
    val visibility: String,
)

@Serializable
data class ProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    val headline: String? = null,
    @SerialName("current_role") val currentRole: String? = null,
    @SerialName("current_company") val currentCompany: String? = null,
    val summary: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    val location: String? = null,
    val timezone: String? = null,
    @SerialName("work_history") val workHistory: List<WorkExperienceDto> = emptyList(),
    val education: List<EducationDto> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<Map<String, String>> = emptyList(),
    val achievements: List<Map<String, String>> = emptyList(),
    val certifications: List<Map<String, String>> = emptyList(),
    @SerialName("portfolio_links") val portfolioLinks: List<PortfolioLinkDto> = emptyList(),
    val visibility: String,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    val provenance: List<ProfileFieldSourceDto> = emptyList(),
) {
    fun toUpdateRequest() = ProfileUpdateDto(
        fullName = fullName,
        headline = headline,
        currentRole = currentRole,
        currentCompany = currentCompany,
        summary = summary,
        yearsExperience = yearsExperience,
        location = location,
        timezone = timezone,
        visibility = visibility,
        workHistory = workHistory,
        education = education,
        skills = skills,
        projects = projects,
        achievements = achievements,
        certifications = certifications,
        portfolioLinks = portfolioLinks,
    )
}

@Serializable
data class OnboardingStatusDto(
    @SerialName("account_created") val accountCreated: Boolean,
    @SerialName("required_consents_recorded") val requiredConsentsRecorded: Boolean,
    @SerialName("profile_started") val profileStarted: Boolean,
    @SerialName("profile_confirmed") val profileConfirmed: Boolean,
    @SerialName("profile_published") val profilePublished: Boolean,
    @SerialName("connected_sources") val connectedSources: List<String>,
)

@Serializable
data class ConsentPolicyDto(
    @SerialName("policy_type") val policyType: String,
    val version: String,
    val url: String,
)

@Serializable
data class ConsentUpdateDto(
    @SerialName("accept_terms") val acceptTerms: Boolean,
    @SerialName("accept_privacy") val acceptPrivacy: Boolean,
    @SerialName("provider_data") val providerData: List<String> = emptyList(),
    @SerialName("resume_processing") val resumeProcessing: Boolean = false,
)

@Serializable
data class ConsentReceiptDto(
    @SerialName("policy_type") val policyType: String,
    @SerialName("policy_version") val policyVersion: String,
    @SerialName("accepted_at") val acceptedAt: String,
    val provider: String? = null,
)

@Serializable
data class BridgeScoreDto(
    val state: String,
    val score: Int? = null,
    @SerialName("calculated_at") val calculatedAt: String? = null,
    val suggestions: List<String> = emptyList(),
    @SerialName("verification_state") val verificationState: String,
)

@Serializable
data class PassportDto(
    @SerialName("full_name") val fullName: String? = null,
    val headline: String? = null,
    @SerialName("current_role") val currentRole: String? = null,
    @SerialName("current_company") val currentCompany: String? = null,
    val summary: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    val skills: List<String> = emptyList(),
    @SerialName("work_history") val workHistory: List<WorkExperienceDto> = emptyList(),
    val education: List<EducationDto> = emptyList(),
    val projects: List<Map<String, String>> = emptyList(),
    val achievements: List<Map<String, String>> = emptyList(),
    @SerialName("portfolio_links") val portfolioLinks: List<PortfolioLinkDto> = emptyList(),
    val score: BridgeScoreDto,
    @SerialName("is_private_preview") val isPrivatePreview: Boolean = true,
)

@Serializable
data class IntentDto(val id: String, val slug: String, val label: String, val selected: Boolean = false)

@Serializable
data class IntentSelectionDto(@SerialName("intent_ids") val intentIds: List<String>)

@Serializable
data class MemberCardDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    val headline: String? = null,
    val location: String? = null,
    val skills: List<String> = emptyList(),
    val intents: List<String> = emptyList(),
    val relationship: String? = null,
)

@Serializable
data class ConnectionRequestDto(@SerialName("recipient_id") val recipientId: String, val note: String? = null)

@Serializable
data class ConnectionStatusDto(val id: String, val status: String)

@Serializable
data class ConnectionItemDto(
    val id: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("full_name") val fullName: String,
    val status: String,
    val incoming: Boolean,
)

@Serializable
data class ConnectionDecisionDto(val decision: String)

@Serializable
data class CohortDto(val id: String, val name: String, val topic: String, val description: String, val members: Int, val joined: Boolean)

@Serializable
data class CohortCreateDto(val name: String, val topic: String, val description: String)

@Serializable
data class CohortPostCreateDto(val body: String)

@Serializable
data class CohortPostDto(val id: String, @SerialName("author_id") val authorId: String, val body: String)

@Serializable
data class PodDto(
    val id: String, val name: String, val goal: String, val status: String,
    @SerialName("expires_at") val expiresAt: String, val members: Int,
    @SerialName("is_member") val isMember: Boolean, val discoverable: Boolean,
)

@Serializable
data class PodCreateDto(
    val name: String, val goal: String, val guidelines: String? = null,
    val discoverable: Boolean = false, @SerialName("duration_weeks") val durationWeeks: Int = 4,
)

@Serializable
data class MessageCreateDto(val body: String)

@Serializable
data class MessageItemDto(
    val id: String, @SerialName("sender_id") val senderId: String,
    val body: String, @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ConversationDto(
    val id: String, @SerialName("member_id") val memberId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("last_message") val lastMessage: String? = null,
)

@Serializable
data class ReferralQuotaDto(val used: Int, val limit: Int, val remaining: Int)

@Serializable
data class ReferralCreateDto(
    @SerialName("recipient_id") val recipientId: String, val company: String,
    val role: String, val message: String,
)

@Serializable
data class ReferralDto(
    val id: String, @SerialName("requester_id") val requesterId: String,
    @SerialName("recipient_id") val recipientId: String,
    val company: String, val role: String, val message: String, val status: String,
)

@Serializable
data class ReferralDecisionDto(val decision: String)

@Serializable
data class MemberReportDto(val reason: String, val details: String? = null)

@Serializable
data class NotificationDto(
    val id: String, val kind: String, val title: String, val body: String,
    @SerialName("read_at") val readAt: String? = null,
)

interface BridgeAuthApi {
    @POST("api/v1/auth/dev/session")
    suspend fun createDevDemoSession(): SessionDto

    @POST("api/v1/auth/google/challenge")
    suspend fun googleChallenge(): GoogleChallengeDto

    @POST("api/v1/auth/google/exchange")
    suspend fun googleExchange(@Body request: GoogleExchangeRequest): SessionDto

    @POST("api/v1/auth/email/register")
    suspend fun registerEmail(@Body request: EmailRegistrationDto): AuthNoticeDto

    @POST("api/v1/auth/email/verify")
    suspend fun verifyEmail(@Body request: EmailVerificationDto): SessionDto

    @POST("api/v1/auth/email/login")
    suspend fun loginEmail(@Body request: EmailLoginDto): SessionDto

    @POST("api/v1/auth/password-reset")
    suspend fun requestPasswordReset(@Body request: PasswordResetDto): AuthNoticeDto

    @POST("api/v1/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body request: PasswordResetConfirmDto): AuthNoticeDto

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshDto): SessionDto

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: RefreshDto): AuthNoticeDto

    @GET("api/v1/auth/me")
    suspend fun currentUser(@Header("Authorization") authorization: String): CurrentUserDto

    @GET("api/v1/me/onboarding")
    suspend fun onboardingStatus(@Header("Authorization") authorization: String): OnboardingStatusDto

    @GET("api/v1/me/profile")
    suspend fun profile(@Header("Authorization") authorization: String): ProfileDto

    @PUT("api/v1/me/profile")
    suspend fun saveProfile(
        @Header("Authorization") authorization: String,
        @Body request: ProfileUpdateDto,
    ): ProfileDto

    @POST("api/v1/me/profile/confirm")
    suspend fun confirmProfile(@Header("Authorization") authorization: String): ProfileDto

    @GET("api/v1/me/consent-policies")
    suspend fun consentPolicies(@Header("Authorization") authorization: String): List<ConsentPolicyDto>

    @POST("api/v1/me/consents")
    suspend fun recordConsents(
        @Header("Authorization") authorization: String,
        @Body request: ConsentUpdateDto,
    ): List<ConsentReceiptDto>

    @GET("api/v1/me/bridge-score")
    suspend fun bridgeScore(@Header("Authorization") authorization: String): BridgeScoreDto

    @GET("api/v1/me/passport")
    suspend fun passport(@Header("Authorization") authorization: String): PassportDto

    @GET("api/v1/me/intents")
    suspend fun intents(@Header("Authorization") authorization: String): List<IntentDto>

    @PUT("api/v1/me/intents")
    suspend fun saveIntents(@Header("Authorization") authorization: String, @Body request: IntentSelectionDto): List<IntentDto>

    @GET("api/v1/discover")
    suspend fun discover(@Header("Authorization") authorization: String, @retrofit2.http.Query("q") query: String? = null, @retrofit2.http.Query("skill") skill: String? = null): List<MemberCardDto>

    @GET("api/v1/connections")
    suspend fun connections(@Header("Authorization") authorization: String): List<ConnectionItemDto>

    @POST("api/v1/connections")
    suspend fun requestConnection(@Header("Authorization") authorization: String, @Body request: ConnectionRequestDto): ConnectionStatusDto

    @POST("api/v1/connections/{request_id}/decision")
    suspend fun decideConnection(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("request_id") requestId: String,
        @Body decision: ConnectionDecisionDto,
    ): ConnectionStatusDto

    @GET("api/v1/cohorts")
    suspend fun cohorts(@Header("Authorization") authorization: String): List<CohortDto>

    @POST("api/v1/cohorts")
    suspend fun createCohort(@Header("Authorization") authorization: String, @Body request: CohortCreateDto): CohortDto

    @POST("api/v1/cohorts/{cohort_id}/join")
    suspend fun joinCohort(@Header("Authorization") authorization: String, @retrofit2.http.Path("cohort_id") cohortId: String): Map<String, Boolean>

    @GET("api/v1/cohorts/{cohort_id}/posts")
    suspend fun cohortPosts(@Header("Authorization") authorization: String, @retrofit2.http.Path("cohort_id") cohortId: String): List<CohortPostDto>

    @POST("api/v1/cohorts/{cohort_id}/posts")
    suspend fun createCohortPost(@Header("Authorization") authorization: String, @retrofit2.http.Path("cohort_id") cohortId: String, @Body body: CohortPostCreateDto): CohortPostDto

    @GET("api/v1/pods")
    suspend fun pods(@Header("Authorization") authorization: String): List<PodDto>

    @POST("api/v1/pods")
    suspend fun createPod(@Header("Authorization") authorization: String, @Body request: PodCreateDto): PodDto

    @POST("api/v1/pods/{pod_id}/join")
    suspend fun joinPod(@Header("Authorization") authorization: String, @retrofit2.http.Path("pod_id") podId: String): Map<String, Boolean>

    @GET("api/v1/pods/{pod_id}/messages")
    suspend fun podMessages(@Header("Authorization") authorization: String, @retrofit2.http.Path("pod_id") podId: String): List<MessageItemDto>

    @POST("api/v1/pods/{pod_id}/messages")
    suspend fun sendPodMessage(@Header("Authorization") authorization: String, @retrofit2.http.Path("pod_id") podId: String, @Body body: MessageCreateDto): MessageItemDto

    @GET("api/v1/conversations")
    suspend fun conversations(@Header("Authorization") authorization: String): List<ConversationDto>

    @GET("api/v1/conversations/{conversation_id}/messages")
    suspend fun conversationMessages(@Header("Authorization") authorization: String, @retrofit2.http.Path("conversation_id") conversationId: String): List<MessageItemDto>

    @POST("api/v1/members/{member_id}/messages")
    suspend fun sendDirectMessage(@Header("Authorization") authorization: String, @retrofit2.http.Path("member_id") memberId: String, @Body body: MessageCreateDto): MessageItemDto

    @GET("api/v1/referrals/quota")
    suspend fun referralQuota(@Header("Authorization") authorization: String): ReferralQuotaDto

    @GET("api/v1/referrals")
    suspend fun referrals(@Header("Authorization") authorization: String): List<ReferralDto>

    @POST("api/v1/referrals")
    suspend fun createReferral(@Header("Authorization") authorization: String, @Body request: ReferralCreateDto): Map<String, String>

    @POST("api/v1/referrals/{request_id}/decision")
    suspend fun decideReferral(@Header("Authorization") authorization: String, @retrofit2.http.Path("request_id") requestId: String, @Body decision: ReferralDecisionDto): Map<String, String>

    @POST("api/v1/members/{user_id}/block")
    suspend fun blockMember(@Header("Authorization") authorization: String, @retrofit2.http.Path("user_id") userId: String)

    @POST("api/v1/members/{user_id}/reports")
    suspend fun reportMember(@Header("Authorization") authorization: String, @retrofit2.http.Path("user_id") userId: String, @Body report: MemberReportDto): Map<String, String>

    @GET("api/v1/notifications")
    suspend fun notifications(@Header("Authorization") authorization: String): List<NotificationDto>

    @POST("api/v1/notifications/{notification_id}/read")
    suspend fun markNotificationRead(@Header("Authorization") authorization: String, @retrofit2.http.Path("notification_id") id: String): Map<String, Boolean>
}
