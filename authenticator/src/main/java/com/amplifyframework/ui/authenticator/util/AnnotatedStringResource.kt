/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator.util

import android.content.Context
import android.graphics.Typeface
import android.text.Spanned
import android.text.SpannedString
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT

/**
 * Reads an HTML string from resources and turns it into an [AnnotatedString]
 */
@Composable
internal fun annotatedStringResource(
    @StringRes id: Int
): AnnotatedString {
    val context = LocalContext.current
    return remember(id) {
        context.getText(id).toAnnotatedString()
    }
}

@Composable
internal fun annotatedStringResource(
    @StringRes id: Int,
    vararg formatArgs: Any
): AnnotatedString {
    val context = LocalContext.current
    return remember(id, formatArgs) {
        context.getText(id, *formatArgs).toAnnotatedString()
    }
}

private fun Context.getText(
    @StringRes id: Int,
    vararg formatArgs: Any
): CharSequence {
    val resource = SpannedString(getText(id))
    val html = HtmlCompat.toHtml(resource, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    val string = String.format(html, *formatArgs)
    return HtmlCompat.fromHtml(string, FROM_HTML_MODE_COMPACT)
}

/**
 * Converts certain style spans to their AnnotatedString equivalent
 */
private fun CharSequence.toAnnotatedString(): AnnotatedString {
    return if (this is Spanned) {
        val spanned = this
        buildAnnotatedString {
            append(spanned.toString())
            getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = getSpanStart(span)
                val end = getSpanEnd(span)
                when (span) {
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        Typeface.BOLD_ITALIC -> addStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold
                            ),
                            start,
                            end
                        )
                    }
                    is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                }
            }
        }
    } else {
        AnnotatedString(this.toString())
    }
}
