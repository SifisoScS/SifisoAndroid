package com.sifiso.ai.agents

import android.content.Context
import com.sifiso.ai.privacy.TransparencyLogger
import com.sifiso.ai.sensors.SensorSnapshot
import kotlinx.coroutines.*

/**
 * Career Agent
 * Analyzes industry trends, identifies opportunities, and recommends skill development
 *
 * Features:
 * - Industry trend monitoring
 * - Job opportunity matching
 * - Skill gap analysis
 * - Learning resource recommendations
 * - Networking event suggestions
 */
class CareerAgent(
    context: Context,
    transparencyLogger: TransparencyLogger
) : Agent(context, transparencyLogger) {

    override val name = "Career Agent"
    override val category = AgentCategory.CAREER
    override val requiredPermissions = listOf<String>()  // No sensitive permissions needed

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false

    // User career profile (would be loaded from storage)
    private val userProfile = CareerProfile(
        industry = "Technology",
        currentRole = "Software Engineer",
        skills = listOf("Kotlin", "Android", "Machine Learning", "Python"),
        interests = listOf("AI", "Mobile Development", "Privacy Tech"),
        experienceYears = 5,
        careerGoals = listOf("AI Architect", "Tech Lead")
    )

    // Tracked trends and opportunities
    private val trackedTrends = mutableListOf<IndustryTrend>()
    private val opportunities = mutableListOf<CareerOpportunity>()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        isRunning = true
        scope.launch {
            // Run career analysis daily
            while (isRunning) {
                analyzeCareerTrends()
                delay(86400000) // 24 hours
            }
        }
    }

    override suspend fun onSensorData(snapshot: SensorSnapshot) {
        // Career agent doesn't need real-time sensor data
        // But could use context (e.g., location = at conference)
    }

    override suspend fun process() {
        while (isRunning) {
            delay(3600000) // Process every hour

            // Check for new opportunities
            findNewOpportunities()

            // Update skill recommendations
            analyzeSkillGaps()
        }
    }

    override fun stop() {
        isRunning = false
        scope.cancel()
    }

    /**
     * Analyze industry trends relevant to user's career
     */
    private suspend fun analyzeCareerTrends() {
        logDataAccess("career_profile", "Industry trend analysis")

        // Simulate fetching industry trends
        val trends = fetchIndustryTrends(userProfile.industry)

        // Match trends to user's profile
        val relevantTrends = matchTrendsToProfile(trends)

        // Store trends
        trackedTrends.clear()
        trackedTrends.addAll(relevantTrends)

        // Identify skill gaps
        val skillGaps = identifySkillGaps(relevantTrends)

        // Generate recommendations
        if (skillGaps.isNotEmpty() && relevantTrends.isNotEmpty()) {
            val topTrend = relevantTrends.first()
            val topSkillGap = skillGaps.first()

            logDecision(
                decision = "Career development recommendation generated",
                reasoning = "Trend '${topTrend.name}' growing at ${topTrend.growthRate}%, skill '${topSkillGap.skill}' is in high demand",
                confidence = topTrend.matchScore
            )

            sendCareerInsight(
                title = "Emerging Trend: ${topTrend.name}",
                message = "This field is growing ${(topTrend.growthRate * 100).toInt()}% annually. Consider developing skills in ${topSkillGap.skill}.",
                data = mapOf(
                    "trend" to topTrend,
                    "skill_gap" to topSkillGap
                )
            )
        }
    }

    private fun fetchIndustryTrends(industry: String): List<IndustryTrend> {
        // In production: Fetch from APIs (LinkedIn, Indeed, GitHub trends, etc.)
        // For prototype: Return simulated data
        return listOf(
            IndustryTrend(
                name = "On-Device AI",
                growthRate = 0.45f,
                relevance = 0.95f,
                requiredSkills = listOf("Machine Learning", "Optimization", "Embedded Systems", "TensorFlow Lite"),
                jobPostingsGrowth = 0.35f,
                averageSalary = 140000
            ),
            IndustryTrend(
                name = "Edge Computing",
                growthRate = 0.38f,
                relevance = 0.85f,
                requiredSkills = listOf("Distributed Systems", "Networking", "Security", "Cloud"),
                jobPostingsGrowth = 0.28f,
                averageSalary = 135000
            ),
            IndustryTrend(
                name = "Privacy-First Technology",
                growthRate = 0.42f,
                relevance = 0.88f,
                requiredSkills = listOf("Cryptography", "Privacy Engineering", "GDPR", "Security"),
                jobPostingsGrowth = 0.31f,
                averageSalary = 145000
            ),
            IndustryTrend(
                name = "Quantum Computing",
                growthRate = 0.52f,
                relevance = 0.60f,
                requiredSkills = listOf("Quantum Algorithms", "Linear Algebra", "Physics", "Python"),
                jobPostingsGrowth = 0.48f,
                averageSalary = 160000
            )
        )
    }

    private fun matchTrendsToProfile(trends: List<IndustryTrend>): List<IndustryTrend> {
        return trends.map { trend ->
            // Calculate match score based on skill overlap
            val skillOverlap = calculateSkillOverlap(trend.requiredSkills, userProfile.skills)
            val interestMatch = userProfile.interests.any { interest ->
                trend.name.contains(interest, ignoreCase = true)
            }

            val matchScore = (skillOverlap * 0.6f) + (if (interestMatch) 0.4f else 0f)

            trend.copy(matchScore = matchScore)
        }.sortedByDescending { it.matchScore }
    }

    private fun calculateSkillOverlap(requiredSkills: List<String>, userSkills: List<String>): Float {
        if (requiredSkills.isEmpty()) return 0f

        val userSkillsLower = userSkills.map { it.lowercase() }
        val matchingSkills = requiredSkills.count { required ->
            userSkillsLower.any { it.contains(required.lowercase()) || required.lowercase().contains(it) }
        }

        return matchingSkills.toFloat() / requiredSkills.size
    }

    private fun identifySkillGaps(trends: List<IndustryTrend>): List<SkillGap> {
        val allRequiredSkills = trends.flatMap { it.requiredSkills }.toSet()
        val userSkillsLower = userProfile.skills.map { it.lowercase() }

        val gaps = mutableListOf<SkillGap>()

        for (skill in allRequiredSkills) {
            val hasSkill = userSkillsLower.any {
                it.contains(skill.lowercase()) || skill.lowercase().contains(it)
            }

            if (!hasSkill) {
                // Calculate priority based on how many trends require it
                val demandCount = trends.count { trend ->
                    trend.requiredSkills.contains(skill)
                }
                val priority = demandCount.toFloat() / trends.size

                gaps.add(
                    SkillGap(
                        skill = skill,
                        priority = priority,
                        demandTrends = trends.filter { it.requiredSkills.contains(skill) }.map { it.name },
                        learningResources = getLearningResources(skill)
                    )
                )
            }
        }

        return gaps.sortedByDescending { it.priority }.take(5)
    }

    private fun getLearningResources(skill: String): List<LearningResource> {
        // In production: Fetch from Coursera, Udemy, YouTube APIs
        return listOf(
            LearningResource(
                title = "Master $skill",
                provider = "Coursera",
                type = "Online Course",
                durationHours = 20,
                cost = "Free",
                rating = 4.7f
            ),
            LearningResource(
                title = "$skill Fundamentals",
                provider = "YouTube",
                type = "Video Series",
                durationHours = 5,
                cost = "Free",
                rating = 4.5f
            )
        )
    }

    private suspend fun findNewOpportunities() {
        // Simulate job search based on user profile
        val newOpportunities = searchJobs(userProfile)

        // Filter for high-match opportunities
        val highMatchOpportunities = newOpportunities.filter { it.matchScore > 0.75f }

        if (highMatchOpportunities.isNotEmpty()) {
            opportunities.addAll(highMatchOpportunities)

            val topOpportunity = highMatchOpportunities.first()

            logDecision(
                decision = "High-match career opportunity identified",
                reasoning = "Job '${topOpportunity.title}' at ${topOpportunity.company} matches ${(topOpportunity.matchScore * 100).toInt()}% of profile",
                confidence = topOpportunity.matchScore
            )

            sendCareerInsight(
                title = "New Opportunity: ${topOpportunity.title}",
                message = "${topOpportunity.company} - ${topOpportunity.location}\n${(topOpportunity.matchScore * 100).toInt()}% match",
                data = mapOf("opportunity" to topOpportunity)
            )
        }
    }

    private fun searchJobs(profile: CareerProfile): List<CareerOpportunity> {
        // In production: Integrate with LinkedIn, Indeed, Glassdoor APIs
        // For prototype: Return simulated opportunities
        return listOf(
            CareerOpportunity(
                title = "Senior AI Engineer - On-Device ML",
                company = "TechCorp",
                location = "Remote",
                type = "Full-time",
                matchScore = 0.88f,
                skillsMatch = listOf("Machine Learning", "Python", "Android"),
                salary = "$140k - $180k",
                postedDaysAgo = 3,
                description = "Build cutting-edge on-device AI systems"
            ),
            CareerOpportunity(
                title = "Privacy Engineer",
                company = "SecureAI",
                location = "San Francisco",
                type = "Full-time",
                matchScore = 0.82f,
                skillsMatch = listOf("Python", "Security", "Privacy"),
                salary = "$150k - $190k",
                postedDaysAgo = 5,
                description = "Design privacy-preserving ML systems"
            )
        )
    }

    private suspend fun analyzeSkillGaps() {
        if (trackedTrends.isEmpty()) return

        val skillGaps = identifySkillGaps(trackedTrends)
        if (skillGaps.isNotEmpty()) {
            // Skill gap analysis complete - could send weekly summary
        }
    }

    private suspend fun sendCareerInsight(
        title: String,
        message: String,
        data: Map<String, Any> = emptyMap()
    ) {
        // Would send notification via AgentOrchestrator
    }

    // Data classes
    data class CareerProfile(
        val industry: String,
        val currentRole: String,
        val skills: List<String>,
        val interests: List<String>,
        val experienceYears: Int,
        val careerGoals: List<String>
    )

    data class IndustryTrend(
        val name: String,
        val growthRate: Float,           // Annual growth rate (0-1)
        val relevance: Float,            // Relevance to user (0-1)
        val requiredSkills: List<String>,
        val jobPostingsGrowth: Float,
        val averageSalary: Int,
        val matchScore: Float = 0f       // Match to user profile (0-1)
    )

    data class SkillGap(
        val skill: String,
        val priority: Float,             // Priority to learn (0-1)
        val demandTrends: List<String>,
        val learningResources: List<LearningResource>
    )

    data class LearningResource(
        val title: String,
        val provider: String,
        val type: String,
        val durationHours: Int,
        val cost: String,
        val rating: Float
    )

    data class CareerOpportunity(
        val title: String,
        val company: String,
        val location: String,
        val type: String,
        val matchScore: Float,
        val skillsMatch: List<String>,
        val salary: String,
        val postedDaysAgo: Int,
        val description: String
    )
}
