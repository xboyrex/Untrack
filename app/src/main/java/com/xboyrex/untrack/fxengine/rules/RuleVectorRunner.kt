package com.xboyrex.untrack.fxengine.rules

import com.xboyrex.untrack.fxengine.processing.ProcessingProfile
import javax.inject.Inject
import javax.inject.Singleton

data class RuleVectorResult(
    val vector: RuleTestVector,
    val output: String,
    val passed: Boolean
)

data class RuleVectorRunResult(
    val results: List<RuleVectorResult>
) {
    val passedCount: Int
        get() = results.count(RuleVectorResult::passed)
    val failingCount: Int
        get() = results.size - passedCount
    val allPassed: Boolean
        get() = failingCount == 0
}

class RuleActivationBlockedException(
    val failingVectorCount: Int
) : IllegalStateException("Rule has $failingVectorCount failing test vectors")

/**
 * Runs saved rule vectors against one enabled copy of their rule.
 *
 * This deliberately calls the rule engine directly instead of the processing
 * pipeline. Vector checks neither read preferences nor interact with history
 * or the cleaner cache, and compare the engine's raw output byte-for-byte.
 */
@Singleton
class RuleVectorRunner @Inject constructor(
    private val compiler: RuleCompiler,
    private val engine: CustomRuleEngine
) {
    fun run(rule: CustomUrlRule): RuleVectorRunResult {
        val executable = rule.copy(enabled = true)
        val compiled = compiler.compile(executable)
        val profile = ProcessingProfile.entries.first { it in executable.contexts }
        val snapshot = RuleSnapshot(listOf(compiled), revision = 0)

        return RuleVectorRunResult(
            executable.testVectors.map { vector ->
                val output = engine.applyPhase(
                    url = vector.input,
                    phase = executable.phase,
                    profile = profile,
                    snapshot = snapshot,
                    traceEnabled = true
                ).url
                RuleVectorResult(
                    vector = vector,
                    output = output,
                    passed = output == vector.expected
                )
            }
        )
    }
}
