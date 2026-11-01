package com.tvtheme.p3launcher

import android.content.Context
import android.net.Uri

object Sites {
    data class Site(val name: String, val url: String)

    fun load(c: Context): MutableList<Site> {
        val raw = c.getSharedPreferences("p3", 0).getString("sites", "") ?: ""
        return raw.split("\n").filter { it.contains("|") }.map {
            val i = it.indexOf("|")
            Site(it.substring(0, i), it.substring(i + 1))
        }.toMutableList()
    }

    fun save(c: Context, list: List<Site>) {
        c.getSharedPreferences("p3", 0).edit()
            .putString("sites", list.joinToString("\n") { it.name + "|" + it.url }).apply()
    }

    fun normalize(input: String): String? {
        var s = input.trim()
        if (s.isEmpty()) return null
        if (!s.contains("://")) s = "https://$s"
        return if (Uri.parse(s).host.isNullOrEmpty()) null else s
    }

    fun nameFor(url: String): String =
        (Uri.parse(url).host ?: "site").removePrefix("www.").substringBefore(".")
}

object AdBlock {
    private val hosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "2mdn.net", "adnxs.com", "adsrvr.org", "taboola.com", "outbrain.com", "criteo.com",
        "criteo.net", "rubiconproject.com", "pubmatic.com", "openx.net", "amazon-adsystem.com",
        "moatads.com", "scorecardresearch.com", "adsafeprotected.com", "popads.net",
        "propellerads.com", "exoclick.com", "juicyads.com", "trafficjunky.com", "mgid.com",
        "revcontent.com", "smartadserver.com", "casalemedia.com", "adform.net",
        "serving-sys.com", "yieldmo.com", "teads.tv", "zedo.com"
    )

    fun blocked(host: String?): Boolean {
        var h = host?.lowercase() ?: return false
        while (true) {
            if (h in hosts) return true
            val i = h.indexOf('.')
            if (i < 0) return false
            h = h.substring(i + 1)
        }
    }
}
