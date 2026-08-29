package tech.mmarca.openvitals.features.activity

import io.mockk.every
import io.mockk.mockk
import java.io.OutputStream
import org.xmlpull.v1.XmlSerializer

/**
 * A minimal XML writer standing in for `android.util.Xml.newSerializer()`,
 * which is a throwing stub in a JVM unit test. It only implements the calls
 * the exporters make; the assertions are about the document that comes out,
 * so the structure under test is the exporter's, not this.
 */
internal fun recordingXmlSerializer(): XmlSerializer {
    val serializer = mockk<XmlSerializer>(relaxed = true)
    var destination: OutputStream? = null
    val document = StringBuilder()
    var openTag: String? = null
    val attributes = StringBuilder()

    fun closeOpenTag() {
        openTag?.let { name ->
            document.append('<').append(name).append(attributes).append('>')
            attributes.setLength(0)
            openTag = null
        }
    }

    every { serializer.setOutput(any<OutputStream>(), any()) } answers {
        destination = firstArg()
    }
    every { serializer.startTag(any(), any()) } answers {
        closeOpenTag()
        openTag = secondArg()
        serializer
    }
    every { serializer.attribute(any(), any(), any()) } answers {
        attributes.append(' ').append(secondArg<String>())
            .append("=\"").append(thirdArg<String>().xmlEscaped()).append('"')
        serializer
    }
    every { serializer.text(any<String>()) } answers {
        closeOpenTag()
        document.append(firstArg<String>().xmlEscaped())
        serializer
    }
    every { serializer.endTag(any(), any()) } answers {
        closeOpenTag()
        document.append("</").append(secondArg<String>()).append('>')
        serializer
    }
    every { serializer.endDocument() } answers { closeOpenTag() }
    every { serializer.flush() } answers {
        destination?.write(document.toString().toByteArray(Charsets.UTF_8))
        destination?.flush()
        document.setLength(0)
    }
    return serializer
}

private fun String.xmlEscaped(): String = buildString(length) {
    this@xmlEscaped.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(char)
        }
    }
}
