package androidx.ui.port.engine.text

import android.app.Instrumentation
import android.graphics.Typeface
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.ui.engine.text.FontFallback
import androidx.ui.engine.text.Paragraph
import androidx.ui.engine.text.ParagraphConstraints
import androidx.ui.engine.text.ParagraphStyle
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class ParagraphTest {
    private lateinit var instrumentation: Instrumentation
    private lateinit var fontFallback: FontFallback

    @Before
    fun setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        val font = Typeface.createFromAsset(instrumentation.context.assets, "sample_font.ttf")!!
        assertThat(font, notNullValue())
        fontFallback = FontFallback(font)
    }

    @Test
    fun empty_string() {
        val fontSize = 50.0
        val text = StringBuilder("")
        val paragraph = Paragraph(
                text = text,
                textStyles = listOf(),
                paragraphStyle = ParagraphStyle(
                        fontFamily = fontFallback,
                        fontSize = fontSize
                )
        )

        paragraph.layout(ParagraphConstraints(width = 100.0))

        assertThat(paragraph.width, equalTo(100.0))
        assertThat(paragraph.height, equalTo(fontSize))
        // defined in sample_font
        assertThat(paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
        assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0))
        assertThat(paragraph.minIntrinsicWidth, equalTo(0.0))
        assertThat(paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        // TODO(Migration/siyamed): no baseline query per line?
        // TODO(Migration/siyamed): no line count?
    }

    @Test
    fun single_line_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("xyz", "\u05D0\u05D1\u05D2")) {
            val paragraph = Paragraph(
                    text = StringBuilder(text),
                    textStyles = listOf(),
                    paragraphStyle = ParagraphStyle(
                            fontFamily = fontFallback,
                            fontSize = fontSize
                    )
            )

            // width greater than text width - 150
            paragraph.layout(ParagraphConstraints(width = 200.0))

            assertThat(text, paragraph.width, equalTo(200.0))
            assertThat(text, paragraph.height, equalTo(fontSize))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.length))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun line_break_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abcdef", "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5")) {
            val paragraph = Paragraph(
                    text = StringBuilder(text),
                    textStyles = listOf(),
                    paragraphStyle = ParagraphStyle(
                            fontFamily = fontFallback,
                            fontSize = fontSize
                    )
            )

            // 3 chars width
            paragraph.layout(ParagraphConstraints(width = 3 * fontSize))

            // 3 chars
            assertThat(text, paragraph.width, equalTo(3 * fontSize))
            // 2 lines, 1 line gap
            assertThat(text, paragraph.height, equalTo(2 * fontSize + fontSize / 5.0))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.length))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun newline_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = Paragraph(
                    text = StringBuilder(text),
                    textStyles = listOf(),
                    paragraphStyle = ParagraphStyle(
                            fontFamily = fontFallback,
                            fontSize = fontSize
                    )
            )

            // 3 chars width
            paragraph.layout(ParagraphConstraints(width = 3 * fontSize))

            // 3 chars
            assertThat(text, paragraph.width, equalTo(3 * fontSize))
            // 2 lines, 1 line gap
            assertThat(text, paragraph.height, equalTo(2 * fontSize + fontSize / 5.0))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.indexOf("\n")))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    @Test
    fun newline_and_line_break_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = Paragraph(
                    text = StringBuilder(text),
                    textStyles = listOf(),
                    paragraphStyle = ParagraphStyle(
                            fontFamily = fontFallback,
                            fontSize = fontSize
                    )
            )

            // 2 chars width
            paragraph.layout(ParagraphConstraints(width = 2 * fontSize))

            // 2 chars
            assertThat(text, paragraph.width, equalTo(2 * fontSize))
            // 4 lines, 3 line gaps
            assertThat(text, paragraph.height, equalTo(4 * fontSize + 3 * fontSize / 5.0))
            // defined in sample_font
            assertThat(text, paragraph.alphabeticBaseline, equalTo(fontSize * 0.8))
            assertThat(text, paragraph.maxIntrinsicWidth, equalTo(fontSize * text.indexOf("\n")))
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0))
            assertThat(text, paragraph.ideographicBaseline, equalTo(Double.MAX_VALUE))
        }
    }

    // TODO(migration/siyamed) add getPositionForOffset test
}