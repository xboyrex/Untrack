package com.xboyrex.untrack

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

enum class CustomRulePhase(val label: String) {
    PRE_CLEAN("Before Built-in Cleaners"),
    POST_CLEAN("After Built-in Cleaners"),
    POST_CONVERSION("After Frontend Conversion")
}

sealed interface CustomRuleAction {
    val summary: String

    data object RemoveAllParams : CustomRuleAction {
        override val summary: String = "Remove all query parameters"
    }

    data class RemoveParams(val names: List<String>) : CustomRuleAction {
        override val summary: String = "Remove params: ${names.joinToString(", ")}"
    }

    data class KeepOnlyParams(val names: List<String>) : CustomRuleAction {
        override val summary: String = "Keep only: ${names.joinToString(", ")}"
    }

    data class RegexReplace(val pattern: String, val replacement: String) : CustomRuleAction {
        override val summary: String = "Regex: $pattern → $replacement"
    }

    data class ExtractRedirect(val paramName: String) : CustomRuleAction {
        override val summary: String = "Extract redirect from ?$paramName="
    }

    data class HostRewrite(val newHost: String) : CustomRuleAction {
        override val summary: String = "Rewrite host to $newHost"
    }
}

data class CustomUrlRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostPattern: String, // "*" for all hosts or domain/subdomain
    val phase: CustomRulePhase = CustomRulePhase.POST_CLEAN,
    val action: CustomRuleAction,
    val enabled: Boolean = true,
) {
    fun matches(url: ParsedUrl): Boolean {
        val p = hostPattern.trim().lowercase(Locale.ROOT)
        if (p.isEmpty() || p == "*") return true
        return ParsedUrl.hostMatchesDomain(url.asciiHost, p)
    }

    fun apply(url: ParsedUrl): ParsedUrl {
        return when (val act = action) {
            CustomRuleAction.RemoveAllParams -> url.copy(parameters = emptyList())
            is CustomRuleAction.RemoveParams -> {
                val lowerSet = act.names.map { it.lowercase(Locale.ROOT) }.toSet()
                url.copy(parameters = url.parameters.filterNot { it.name.lowercase(Locale.ROOT) in lowerSet })
            }
            is CustomRuleAction.KeepOnlyParams -> {
                val lowerSet = act.names.map { it.lowercase(Locale.ROOT) }.toSet()
                url.copy(parameters = url.parameters.filter { it.name.lowercase(Locale.ROOT) in lowerSet })
            }
            is CustomRuleAction.RegexReplace -> {
                val raw = url.toString()
                val replaced = runCatching { Regex(act.pattern).replace(raw, act.replacement) }.getOrDefault(raw)
                ParsedUrl.parse(replaced) ?: url
            }
            is CustomRuleAction.ExtractRedirect -> {
                val targetParam = url.parameters.firstOrNull {
                    it.name.equals(act.paramName, ignoreCase = true)
                }?.value ?: return url
                val decoded = ParsedUrl.percentDecodeOnce(targetParam)
                ParsedUrl.parse(decoded) ?: url
            }
            is CustomRuleAction.HostRewrite -> {
                url.copy(host = act.newHost.trim().lowercase(Locale.ROOT))
            }
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("hostPattern", hostPattern)
        put("phase", phase.name)
        put("enabled", enabled)
        when (val a = action) {
            CustomRuleAction.RemoveAllParams -> {
                put("actionType", "REMOVE_ALL")
            }
            is CustomRuleAction.RemoveParams -> {
                put("actionType", "REMOVE_PARAMS")
                put("names", JSONArray(a.names))
            }
            is CustomRuleAction.KeepOnlyParams -> {
                put("actionType", "KEEP_ONLY")
                put("names", JSONArray(a.names))
            }
            is CustomRuleAction.RegexReplace -> {
                put("actionType", "REGEX")
                put("pattern", a.pattern)
                put("replacement", a.replacement)
            }
            is CustomRuleAction.ExtractRedirect -> {
                put("actionType", "EXTRACT_REDIRECT")
                put("paramName", a.paramName)
            }
            is CustomRuleAction.HostRewrite -> {
                put("actionType", "HOST_REWRITE")
                put("newHost", a.newHost)
            }
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): CustomUrlRule? = runCatching {
            val id = obj.optString("id", UUID.randomUUID().toString())
            val name = obj.optString("name").ifBlank { return null }
            val hostPattern = obj.optString("hostPattern", "*")
            val phase = runCatching { CustomRulePhase.valueOf(obj.optString("phase", "POST_CLEAN")) }
                .getOrDefault(CustomRulePhase.POST_CLEAN)
            val enabled = obj.optBoolean("enabled", true)
            val action = when (obj.optString("actionType")) {
                "REMOVE_ALL" -> CustomRuleAction.RemoveAllParams
                "REMOVE_PARAMS" -> {
                    val arr = obj.optJSONArray("names") ?: JSONArray()
                    val list = (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }
                    CustomRuleAction.RemoveParams(list)
                }
                "KEEP_ONLY" -> {
                    val arr = obj.optJSONArray("names") ?: JSONArray()
                    val list = (0 until arr.length()).mapNotNull { arr.optString(it).takeIf(String::isNotBlank) }
                    CustomRuleAction.KeepOnlyParams(list)
                }
                "REGEX" -> CustomRuleAction.RegexReplace(
                    obj.optString("pattern"),
                    obj.optString("replacement")
                )
                "EXTRACT_REDIRECT" -> CustomRuleAction.ExtractRedirect(obj.optString("paramName"))
                "HOST_REWRITE" -> CustomRuleAction.HostRewrite(obj.optString("newHost"))
                else -> return null
            }
            CustomUrlRule(id = id, name = name, hostPattern = hostPattern, phase = phase, action = action, enabled = enabled)
        }.getOrNull()

        /**
         * Teach-by-Example Rule Inference:
         * Given a dirty "Before" URL and a desired "After" URL, automatically infers the safest CustomUrlRule!
         */
        fun inferFromExample(beforeRaw: String, desiredRaw: String): Result<CustomUrlRule> {
            val before = ParsedUrl.parse(beforeRaw)
                ?: return Result.failure(IllegalArgumentException("Before URL is not a valid HTTP(S) link"))
            val desired = ParsedUrl.parse(desiredRaw)
                ?: return Result.failure(IllegalArgumentException("Desired URL is not a valid HTTP(S) link"))

            if (before.toString() == desired.toString()) {
                return Result.failure(IllegalArgumentException("Both URLs are identical"))
            }

            // 1. Check if desired URL was embedded inside a query parameter of before URL (Redirect extraction)
            for (param in before.parameters) {
                val v = param.value ?: continue
                val decoded = ParsedUrl.percentDecodeOnce(v)
                if (decoded.equals(desired.toString(), ignoreCase = true)) {
                    return Result.success(
                        CustomUrlRule(
                            name = "Unwrap ${before.site} (?${param.name}=)",
                            hostPattern = before.site,
                            phase = CustomRulePhase.PRE_CLEAN,
                            action = CustomRuleAction.ExtractRedirect(param.name)
                        )
                    )
                }
            }

            // 2. Check if host was rewritten while path stayed identical
            if (!before.asciiHost.equals(desired.asciiHost, ignoreCase = true) && before.path == desired.path) {
                return Result.success(
                    CustomUrlRule(
                        name = "${before.site} → ${desired.site}",
                        hostPattern = before.site,
                        phase = CustomRulePhase.POST_CONVERSION,
                        action = CustomRuleAction.HostRewrite(desired.asciiHost)
                    )
                )
            }

            // 3. Check if parameters were removed
            if (before.parameters.isNotEmpty() && desired.parameters.isEmpty()) {
                return Result.success(
                    CustomUrlRule(
                        name = "Clean all params on ${before.site}",
                        hostPattern = before.site,
                        phase = CustomRulePhase.POST_CLEAN,
                        action = CustomRuleAction.RemoveAllParams
                    )
                )
            }

            val desiredNames = desired.parameters.map { it.name.lowercase(Locale.ROOT) }.toSet()
            val removedNames = before.parameters
                .map { it.name }
                .filter { it.lowercase(Locale.ROOT) !in desiredNames }
                .distinct()

            if (removedNames.isNotEmpty()) {
                return Result.success(
                    CustomUrlRule(
                        name = "Clean ${removedNames.joinToString(", ")} on ${before.site}",
                        hostPattern = before.site,
                        phase = CustomRulePhase.POST_CLEAN,
                        action = CustomRuleAction.RemoveParams(removedNames)
                    )
                )
            }

            return Result.failure(
                IllegalArgumentException("Could not infer a simple parameter or redirect rule; use Regex Replace for complex path edits.")
            )
        }
    }
}
