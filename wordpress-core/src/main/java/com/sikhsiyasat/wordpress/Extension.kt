package com.sikhsiyasat.wordpress

import android.net.Uri
import androidx.core.text.isDigitsOnly


val String.extractPostSlug: String?
    get() = Uri.parse(this).pathSegments.firstOrNull { !it.isDigitsOnly() }

val String.extractWebsiteUrl: String
    get() = Uri.parse(this)
            .let { it.scheme + "://" + it.host }