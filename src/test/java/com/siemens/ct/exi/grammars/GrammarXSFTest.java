package com.siemens.ct.exi.grammars;

import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.fail;

/**
 * A test that verifies if a Java assertion is thrown when creating grammar from a set of XSDs.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class GrammarXSFTest
{
    @Before
    public void beforeMethod() {
        // For this test to be useful, assertions needs to be enabled (run this test with the '-ea' flag)
        Assume.assumeTrue(EXIContentModelBuilder.class.desiredAssertionStatus());
    }

    /**
     * Tests if creating grammars from an XSD causes a Java assertion to be thrown.
     */
    @Test
    public void testAssertionNotThrown() throws Exception
    {
        // Setup test fixture.
        final File defaultSchema = new File("src/test/resources/xsf/defaultSchema.xsd");
        if (!defaultSchema.exists()) {
            throw new IllegalStateException("Unit test implementation has a bug.");
        }

        final GrammarFactory grammarFactory = GrammarFactory.newInstance();
        final String xsdLocation = defaultSchema.getAbsolutePath();
        final XMLEntityResolver xmlEntityResolverResolver = new SchemaResolver();

        // Execute system under test.
        try
        {
            grammarFactory.createGrammars(xsdLocation, xmlEntityResolverResolver);
        }
        catch (AssertionError e)
        {
            // Verify results.
            e.printStackTrace();
            fail("Java assertion was thrown (stacktrace was printed on the standard error stream).");
        }
    }

    /**
     * Resolves entities from XSD files provided as part in the test resources.
     */
    public static class SchemaResolver implements XMLEntityResolver
    {
        private final HashMap<String, String> namespaceToPath;

        public SchemaResolver() throws ParserConfigurationException, IOException, SAXException
        {
            namespaceToPath = new HashMap<String, String>();

            // Iterate over all files to record a namespace-to-path mapping.
            final File folder = new File("src/test/resources/xsf/");
            if (!folder.isDirectory()) {
                throw new IllegalStateException("Unit test implementation has a bug.");
            }
            final File[] listOfFiles = folder.listFiles();
            if (listOfFiles == null) {
                throw new IllegalStateException("Unit test implementation has a bug.");
            }

            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            for (final File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".xsd")) {
                    final String fileLocation = file.getAbsolutePath();

                    final Document doc = builder.parse(file);
                    doc.getDocumentElement().normalize();
                    final String namespace = doc.getDocumentElement().getAttribute("targetNamespace");

                    System.out.println("Found namespace '" + namespace + "' in file: " + fileLocation);
                    this.namespaceToPath.put(namespace, file.getCanonicalPath());
                }
            }

//            // Add DTDs
//            this.namespaceToPath.put("-//W3C//DTD XMLSCHEMA 200102//EN", new File("src/test/resources/XMLSchema.dtd").getAbsolutePath());
//            this.namespaceToPath.put("datatypes", new File("src/test/resources/datatypes.dtd").getAbsolutePath());
        }

        public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException
        {
            String needle = resourceIdentifier.getNamespace();
            if (needle == null) {
                needle = resourceIdentifier.getPublicId();
            }
            XMLInputSource result = null;
            if (needle != null) {
                if (this.namespaceToPath.containsKey(needle)) {
                    String location = this.namespaceToPath.get(needle);
                    result = new XMLInputSource(resourceIdentifier.getPublicId(), location, resourceIdentifier.getBaseSystemId());
                    System.out.println("Resolved namespace: '" + needle + "' to: " + result.getSystemId());
                } else {
                    System.err.println("Unable to resolved namespace: '" + needle + "'");
                }
            } else {
                System.out.println("Skipping no-namespace lookup for resource identifier: " + resourceIdentifier);
            }
            return result;
        }
    }
}
