package com.xboyrex.untrack

import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.IDN
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Locale

data class ParsedParameter(
    val name: String,
    val value: String?,
) {
    override fun toString(): String = if (value == null) name else "$name=$value"
}

data class ParsedUrl(
    val scheme: String,
    val userInfo: String?,
    val host: String,
    val port: Int?,
    val path: String,
    val parameters: List<ParsedParameter>,
    val fragment: String?,
) {
    val site: String
        get() = host.removePrefix("www.").removePrefix("WWW.").lowercase(Locale.ROOT)

    val asciiHost: String
        get() = runCatching {
            IDN.toASCII(host.trim().removePrefix("[").removeSuffix("]").removeSuffix("."))
                .lowercase(Locale.ROOT)
        }.getOrDefault(host.lowercase(Locale.ROOT))

    override fun toString(): String = buildString {
        append(scheme).append("://")
        if (!userInfo.isNullOrEmpty()) {
            append(userInfo).append('@')
        }
        append(host)
        if (port != null) {
            append(':').append(port)
        }
        append(path)
        if (parameters.isNotEmpty()) {
            append('?')
            append(parameters.joinToString("&") { it.toString() })
        }
        if (fragment != null) {
            append('#').append(fragment)
        }
    }

    fun decoded(): ParsedUrl = copy(
        path = decodeComponent(path, "/?#"),
        parameters = parameters.map { param ->
            ParsedParameter(
                name = decodeComponent(param.name, "&=#+"),
                value = param.value?.let { decodeComponent(it, "&#+") }
            )
        },
        fragment = fragment?.let { decodeComponent(it, "#") }
    )

    companion object {
        private val URL_STRUCTURE_REGEX = Regex(
            "^(https?)://([^/?#]*)([^?#]*)(?:\\?([^#]*))?(?:#(.*))?$",
            RegexOption.IGNORE_CASE
        )
        val FIND_URLS_REGEX = Regex("https?://[^\\s<>\"'\\)\\]]+", RegexOption.IGNORE_CASE)

        fun parse(raw: String): ParsedUrl? {
            val trimmed = raw.trim().trimEnd('.', ',', ';', ')', ']', '>', '"', '\'')
            val match = URL_STRUCTURE_REGEX.matchEntire(trimmed) ?: return null
            val scheme = match.groupValues[1].lowercase(Locale.ROOT)
            val authority = match.groupValues[2]
            if (authority.isBlank()) return null

            val atIndex = authority.lastIndexOf('@')
            val userInfo = if (atIndex >= 0) authority.substring(0, atIndex) else null
            val hostPort = if (atIndex >= 0) authority.substring(atIndex + 1) else authority

            val colonIndex = if (hostPort.startsWith("[")) {
                val closeBracket = hostPort.indexOf(']')
                if (closeBracket >= 0 && closeBracket + 1 < hostPort.length && hostPort[closeBracket + 1] == ':') {
                    closeBracket + 1
                } else -1
            } else {
                hostPort.lastIndexOf(':')
            }

            val host = if (colonIndex > 0) hostPort.substring(0, colonIndex) else hostPort
            if (host.isBlank() || !host.contains('.')) return null
            val port = if (colonIndex > 0) hostPort.substring(colonIndex + 1).toIntOrNull() else null

            val path = match.groupValues[3]
            val queryRaw = match.groups[4]?.value
            val fragment = match.groups[5]?.value

            val parameters = if (queryRaw.isNullOrEmpty()) {
                emptyList()
            } else {
                queryRaw.split('&').filter { it.isNotEmpty() }.map { token ->
                    val cleanedToken = token.removePrefix("amp;")
                    val eqIndex = cleanedToken.indexOf('=')
                    if (eqIndex < 0) {
                        ParsedParameter(cleanedToken, null)
                    } else {
                        ParsedParameter(
                            cleanedToken.substring(0, eqIndex),
                            cleanedToken.substring(eqIndex + 1)
                        )
                    }
                }
            }

            return ParsedUrl(
                scheme = scheme,
                userInfo = userInfo,
                host = host,
                port = port,
                path = path,
                parameters = parameters,
                fragment = fragment,
            )
        }

        fun hostMatchesDomain(host: String?, domain: String): Boolean {
            if (host.isNullOrBlank()) return false
            val h = host.trim().lowercase(Locale.ROOT).removeSuffix(".")
            val d = domain.trim().lowercase(Locale.ROOT).removeSuffix(".")
            return h == d || h.endsWith(".$d")
        }

        fun percentDecodeOnce(value: String): String {
            val out = ByteArrayOutputStream(value.length)
            var i = 0
            while (i < value.length) {
                if (value[i] == '%' && i + 2 < value.length) {
                    val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
                    if (hex != null) {
                        out.write(hex)
                        i += 3
                        continue
                    }
                }
                val bytes = value[i].toString().toByteArray(StandardCharsets.UTF_8)
                out.write(bytes, 0, bytes.size)
                i++
            }
            return runCatching {
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(out.toByteArray()))
                    .toString()
            }.getOrDefault(value)
        }

        private fun decodeComponent(input: String, reserved: String): String {
            val sb = StringBuilder(input.length)
            var i = 0
            while (i < input.length) {
                if (input[i] == '%' && i + 2 < input.length) {
                    val byteVal = input.substring(i + 1, i + 3).toIntOrNull(16)
                    if (byteVal != null && byteVal < 0x80) {
                        val ch = byteVal.toChar()
                        if (byteVal <= 0x20 || ch == '%' || ch in reserved) {
                            sb.append(input.substring(i, i + 3))
                        } else {
                            sb.append(ch)
                        }
                        i += 3
                        continue
                    } else if (byteVal != null) {
                        val bytes = ByteArrayOutputStream()
                        var cursor = i
                        while (cursor + 2 < input.length && input[cursor] == '%') {
                            val b = input.substring(cursor + 1, cursor + 3).toIntOrNull(16) ?: break
                            bytes.write(b)
                            cursor += 3
                        }
                        val decodedStr = runCatching {
                            StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(bytes.toByteArray()))
                                .toString()
                        }.getOrNull()
                        if (decodedStr != null) {
                            sb.append(decodedStr)
                        } else {
                            sb.append(input.substring(i, cursor))
                        }
                        i = cursor
                        continue
                    }
                }
                sb.append(input[i])
                i++
            }
            return sb.toString()
        }

        fun decodeStep(value: String, step: DecodeStep): String? = runCatching {
            when (step) {
                is DecodeStep.Capture -> {
                    val match = Regex(step.pattern, RegexOption.IGNORE_CASE).find(value) ?: return null
                    if (step.replacement != null) {
                            var out: String = step.replacement.orEmpty()
                        match.groupValues.forEachIndexed { idx, groupVal ->
                            out = out.replace("$$idx", groupVal)
                        }
                        out
                    } else {
                        match.groupValues.getOrElse(1) { match.value }
                    }
                }
                DecodeStep.PercentDecode -> percentDecodeOnce(value)
                DecodeStep.FormDecode -> URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                DecodeStep.Base64Decode -> {
                    val padded = value + "=".repeat((4 - value.length % 4) % 4)
                    String(Base64.decode(padded, Base64.URL_SAFE or Base64.DEFAULT), StandardCharsets.UTF_8)
                }
                is DecodeStep.JsonField -> {
                    JSONObject(value).optString(step.key).takeIf { it.isNotBlank() }
                }
            }
        }.getOrNull()
    }
}

sealed interface DecodeStep {
    data class Capture(val pattern: String, val replacement: String? = null) : DecodeStep
    data object PercentDecode : DecodeStep
    data object FormDecode : DecodeStep
    data object Base64Decode : DecodeStep
    data class JsonField(val key: String) : DecodeStep
}
