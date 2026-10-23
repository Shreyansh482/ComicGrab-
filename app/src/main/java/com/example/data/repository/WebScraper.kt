package com.example.data.repository

import com.example.data.model.DiscoveredChapter
import com.example.data.model.DiscoveredComic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.net.ssl.SSLHandshakeException

class WebScraper {

    // In-memory cookie jar to maintain cookies across redirects
    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            list.removeAll { old -> cookies.any { it.name == old.name } }
            list.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(cookieJar)
        .build()

    companion object {
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.113 Mobile Safari/537.36"
    }

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val colonIdx = trimmed.indexOf("://")
        if (colonIdx != -1) {
            val scheme = trimmed.substring(0, colonIdx).lowercase()
            if (scheme != "http" && scheme != "https") {
                return ""
            }
            return trimmed
        }
        return "https://$trimmed"
    }

    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.contains(" ")) return false
        val normalized = normalizeUrl(trimmed)
        if (normalized.isBlank()) return false
        return try {
            val httpUrl = normalized.toHttpUrlOrNull()
            httpUrl != null && httpUrl.host.isNotBlank() &&
                    (httpUrl.host.contains(".") || httpUrl.host == "localhost") &&
                    (httpUrl.scheme == "http" || httpUrl.scheme == "https")
        } catch (_: Exception) {
            false
        }
    }

    suspend fun analyzeUrl(rawUrl: String): Result<DiscoveredComic> = withContext(Dispatchers.IO) {
        val url = normalizeUrl(rawUrl)
        if (!isValidUrl(url)) {
            DownloadDiagnostics.log("URL_VALIDATION", "Invalid URL format: '$rawUrl'", isError = true)
            return@withContext Result.failure(
                IllegalArgumentException("Invalid website URL. Please enter a valid address such as https://example.com/comic")
            )
        }

        DownloadDiagnostics.log("ANALYZE_REQUEST", "Requesting comic page: $url")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Ch-Ua-Mobile", "?1")
                .header("Sec-Ch-Ua-Platform", "\"Android\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Upgrade-Insecure-Requests", "1")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code

            DownloadDiagnostics.log("ANALYZE_RESPONSE", "HTTP $code from $url")

            if (!response.isSuccessful) {
                val errorMsg = mapHttpError(code, url)
                DownloadDiagnostics.log("ANALYZE_ERROR", errorMsg, isError = true)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) {
                val msg = "Website returned an empty response. The server may be blocking or redirecting requests."
                DownloadDiagnostics.log("ANALYZE_ERROR", msg, isError = true)
                return@withContext Result.failure(IOException(msg))
            }

            val doc = Jsoup.parse(html, url)

            // Extract comic title
            var comicTitle = doc.select(
                "h1, meta[property='og:title'], .post-title, .entry-title, .story-info-right h1, .novel-title"
            ).first()?.let {
                if (it.tagName().equals("meta", ignoreCase = true)) it.attr("content") else it.text()
            } ?: doc.title()

            comicTitle = sanitizeTitle(comicTitle)
            if (comicTitle.isBlank()) {
                comicTitle = "Discovered Comic"
            }

            // Extract cover image
            val coverUrl = doc.select("meta[property='og:image']").first()?.attr("content")
                ?: doc.select(".comic-cover img, .series-thumb img, .thumb img, img.cover, .post-thumb img, .info-image img").first()?.let {
                    extractImageUrlFromElement(it)
                }

            // Extract chapters using multi-framework comic selectors
            val chapters = mutableListOf<DiscoveredChapter>()
            val seenUrls = mutableSetOf<String>()

            val primarySelectors = listOf(
                "li.wp-manga-chapter a",
                ".listing-chapters_wrap a",
                ".sub-chap-list a",
                ".version-chap li a",
                "#chapterlist li a",
                ".eplister ul li a",
                ".clp li a",
                "div.chbox a",
                ".chapter-list .row span a",
                ".row-content-chapter li a",
                "ul.row-content-chapter a",
                "ul._episode_list li a",
                "#_listUl li a"
            )

            for (selector in primarySelectors) {
                val elements = doc.select(selector)
                if (elements.isNotEmpty()) {
                    for (el in elements) {
                        val chapterUrl = el.attr("abs:href")
                        if (chapterUrl.isBlank() || seenUrls.contains(chapterUrl)) continue
                        seenUrls.add(chapterUrl)

                        val rawText = el.text().trim()
                        val num = extractChapterNumber(rawText, chapterUrl) ?: (chapters.size + 1).toFloat()
                        val title = if (rawText.isNotBlank()) rawText else "Chapter ${num.toInt()}"

                        chapters.add(
                            DiscoveredChapter(
                                number = num,
                                title = title,
                                url = chapterUrl,
                                estimatedImages = 15
                            )
                        )
                    }
                    if (chapters.isNotEmpty()) break
                }
            }

            // Fallback: search for links containing chapter patterns
            if (chapters.isEmpty()) {
                val candidateElements = doc.select("a[href*='chapter'], a[href*='ch-'], a[href*='episode'], a[href*='/read/']")
                for (el in candidateElements) {
                    val chapterUrl = el.attr("abs:href")
                    if (chapterUrl.isBlank() || seenUrls.contains(chapterUrl)) continue

                    val text = el.text().trim()
                    val num = extractChapterNumber(text, chapterUrl)
                    if (num != null) {
                        seenUrls.add(chapterUrl)
                        chapters.add(
                            DiscoveredChapter(
                                number = num,
                                title = text.ifBlank { "Chapter ${num.toInt()}" },
                                url = chapterUrl,
                                estimatedImages = 15
                            )
                        )
                    }
                }
            }

            if (chapters.isEmpty()) {
                val msg = "No supported chapters found on this webpage. The website format may be unsupported, protected by Cloudflare/CAPTCHA, or requires JavaScript to load chapter links."
                DownloadDiagnostics.log("DISCOVERY_FAILED", msg, isError = true, details = "Title extracted: '$comicTitle', URL: $url")
                return@withContext Result.failure(Exception(msg))
            }

            // Sort chapters ascending by chapter number
            chapters.sortBy { it.number }

            DownloadDiagnostics.log("DISCOVERY_SUCCESS", "Found ${chapters.size} chapters for '$comicTitle'")

            Result.success(
                DiscoveredComic(
                    title = comicTitle,
                    sourceUrl = url,
                    coverUrl = coverUrl,
                    chapters = chapters
                )
            )
        } catch (e: UnknownHostException) {
            val msg = "Website could not be reached. The domain name could not be resolved. Please check your internet connection."
            DownloadDiagnostics.log("NETWORK_ERROR", msg, isError = true, details = e.message)
            Result.failure(IOException(msg, e))
        } catch (e: SocketTimeoutException) {
            val msg = "Request timed out while connecting to the website. The server took too long to respond."
            DownloadDiagnostics.log("NETWORK_ERROR", msg, isError = true, details = e.message)
            Result.failure(IOException(msg, e))
        } catch (e: SSLHandshakeException) {
            val msg = "Secure SSL/TLS connection failed with the website. The server certificate may be invalid."
            DownloadDiagnostics.log("NETWORK_ERROR", msg, isError = true, details = e.message)
            Result.failure(IOException(msg, e))
        } catch (e: Exception) {
            val msg = e.message ?: "An unexpected error occurred while analyzing the comic website."
            DownloadDiagnostics.log("ANALYZE_EXCEPTION", msg, isError = true, details = e.stackTraceToString())
            Result.failure(e)
        }
    }

    suspend fun extractChapterImageUrls(chapterUrl: String): List<String> = withContext(Dispatchers.IO) {
        val imageUrls = mutableListOf<String>()
        DownloadDiagnostics.log("IMAGE_EXTRACTION", "Extracting images from: $chapterUrl")

        try {
            val request = Request.Builder()
                .url(chapterUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Referer", chapterUrl)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                DownloadDiagnostics.log("IMAGE_EXTRACTION_FAIL", "HTTP ${response.code} when loading chapter: $chapterUrl", isError = true)
                return@withContext emptyList()
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) return@withContext emptyList()

            val doc = Jsoup.parse(html, chapterUrl)

            // 1. Selector-based reader images
            val readerSelectors = listOf(
                "#readerarea img",
                ".reading-content img",
                ".entry-content img",
                ".container-chapter-reader img",
                "#chapter-images img",
                ".vung-doc img",
                "div[class*='chapter-image'] img",
                ".page-break img",
                "div[id*='reader'] img",
                ".separator img"
            )

            for (selector in readerSelectors) {
                val elements = doc.select(selector)
                if (elements.isNotEmpty()) {
                    for (img in elements) {
                        val src = extractImageUrlFromElement(img)
                        if (src.isNotBlank() && isLikelyComicImage(src)) {
                            imageUrls.add(src)
                        }
                    }
                    if (imageUrls.isNotEmpty()) break
                }
            }

            // 2. Script-based image extraction (for sites that store images in JS variables)
            if (imageUrls.isEmpty()) {
                val scriptTags = doc.select("script")
                for (script in scriptTags) {
                    val scriptData = script.data()
                    if (scriptData.contains("images") || scriptData.contains("ts_reader") || scriptData.contains("chapter_images")) {
                        val extracted = parseImagesFromScript(scriptData, chapterUrl)
                        if (extracted.isNotEmpty()) {
                            imageUrls.addAll(extracted)
                            break
                        }
                    }
                }
            }

            // 3. Fallback: all images on the page
            if (imageUrls.isEmpty()) {
                val allImgs = doc.select("img")
                for (img in allImgs) {
                    val src = extractImageUrlFromElement(img)
                    if (src.isNotBlank() && isLikelyComicImage(src)) {
                        imageUrls.add(src)
                    }
                }
            }

            DownloadDiagnostics.log("IMAGE_EXTRACTION_RESULT", "Discovered ${imageUrls.size} images for $chapterUrl")
        } catch (e: Exception) {
            DownloadDiagnostics.log("IMAGE_EXTRACTION_ERROR", "Error extracting images: ${e.message}", isError = true)
        }

        imageUrls.distinct()
    }

    private fun extractImageUrlFromElement(element: org.jsoup.nodes.Element): String {
        val candidates = listOf(
            "abs:data-src",
            "abs:data-lazy-src",
            "abs:data-original",
            "abs:data-url",
            "abs:data-cdn",
            "abs:src"
        )
        for (attr in candidates) {
            val v = element.attr(attr).trim()
            if (v.isNotBlank() && !v.startsWith("data:image/svg") && !v.startsWith("data:image/gif;base64,R0lGODlhAQABA")) {
                return v
            }
        }
        val srcset = element.attr("srcset").trim()
        if (srcset.isNotBlank()) {
            val first = srcset.split(",").lastOrNull()?.trim()?.split(" ")?.firstOrNull()?.trim()
            if (!first.isNullOrBlank()) return first
        }
        return ""
    }

    private fun parseImagesFromScript(script: String, baseUrl: String): List<String> {
        val list = mutableListOf<String>()
        val arrayPattern = Pattern.compile("(?i)(?:images|chapter_images|sources|pages)\\s*=\\s*(\\[[^\\]]+\\])")
        val matcher = arrayPattern.matcher(script)
        if (matcher.find()) {
            try {
                val jsonArray = JSONArray(matcher.group(1))
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.opt(i)
                    val urlStr = when (item) {
                        is String -> item
                        is JSONObject -> item.optString("url", item.optString("src", ""))
                        else -> ""
                    }
                    if (urlStr.isNotBlank()) {
                        val resolved = resolveRelativeUrl(urlStr, baseUrl)
                        if (isLikelyComicImage(resolved)) {
                            list.add(resolved)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return list
    }

    private fun resolveRelativeUrl(url: String, baseUrl: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(url.trim()).toString()
        } catch (_: Exception) {
            url.trim()
        }
    }

    private fun isLikelyComicImage(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.startsWith("data:image/svg") || lower.contains("1x1.png") || lower.contains("blank.gif")) {
            return false
        }
        if (lower.contains("logo") || lower.contains("banner") || lower.contains("avatar") ||
            lower.contains("icon") || lower.contains("ad_") || lower.contains("advertisement") ||
            lower.contains("footer") || lower.contains("header")) {
            return false
        }
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".avif") || lower.endsWith(".gif") ||
                lower.contains("/chapters/") || lower.contains("/pages/") || lower.contains("/comic/") ||
                lower.contains("/uploads/manga/") || lower.contains("/wp-content/uploads/") ||
                lower.contains("blogger.googleusercontent.com") || lower.contains("bp.blogspot.com")
    }

    private fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*-\\s*(read manga|read comic|webtoon|chapter list|free online|all chapters).*"), "")
            .replace(Regex("(?i)\\s*\\|.*"), "")
            .trim()
    }

    private fun extractChapterNumber(text: String, url: String): Float? {
        val patterns = listOf(
            Pattern.compile("(?i)chapter\\s*(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(?i)ch\\.?\\s*(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(?i)episode\\s*(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(?i)[/-]c(?:hapter)?[-_]?(\\d+(?:\\.\\d+)?)"),
            Pattern.compile("(\\d+(?:\\.\\d+)?)")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val num = matcher.group(1)?.toFloatOrNull()
                if (num != null && num > 0) return num
            }
        }

        for (pattern in patterns) {
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                val num = matcher.group(1)?.toFloatOrNull()
                if (num != null && num > 0) return num
            }
        }

        return null
    }

    private fun mapHttpError(code: Int, url: String): String {
        return when (code) {
            401, 403 -> "Website denied access (HTTP $code Forbidden). The site may require a user login, CAPTCHA verification, or restricts automated downloaders."
            404 -> "Comic page not found (HTTP 404). Please verify that the webpage address is correct and currently accessible."
            429 -> "Website rate limit exceeded (HTTP 429). The server is temporarily throttling requests. Please wait a moment."
            in 500..599 -> "Website server error (HTTP $code). The comic website server is temporarily failing or undergoing maintenance."
            else -> "Website request failed with HTTP Error $code."
        }
    }
}
