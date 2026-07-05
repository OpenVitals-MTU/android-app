package tech.mmarca.openvitals.features.imports.applehealth

import java.io.FilterInputStream
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes

/** Hardened SAX factory shared by the export and workout-route parsers (no external entities/DTDs). */
internal fun secureSaxParserFactory(): SAXParserFactory =
    SAXParserFactory.newInstance().apply {
        isNamespaceAware = false
        setFeatureIfSupported("http://xml.org/sax/features/external-general-entities", false)
        setFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities", false)
        setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    }

private fun SAXParserFactory.setFeatureIfSupported(feature: String, enabled: Boolean) {
    runCatching { setFeature(feature, enabled) }
}

internal fun Attributes.value(name: String): String? = getValue(name)?.takeIf { it.isNotBlank() }

/** Prevents the SAX parser from closing the underlying ZipInputStream when streaming an entry. */
internal class NonClosingInputStream(delegate: InputStream) : FilterInputStream(delegate) {
    override fun close() = Unit
}
