package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;


public class ParisOntologyMatcherTest extends OntologyMatcherTestBase {
    @DataProvider
    public static Object[][] equivalences() {
        return new Object[][] {
                {"cit", new String[]{"1.jsonld", "ontology0.owl", "2.jsonld", "ontology1.owl"}},
//                {"prefix", new String[]{"1.ttl", "foaf.rdf", "2.ttl", "foaf-2.rdf",
//                        "3.ttl", "foaf-3.rdf"}},
//                {"instance", new String[]{"1.ttl", "foaf.rdf", "2.ttl", "adua.rdf",
//                        "3.ttl", "faof.rdf"}},
        };
    }

    @Test(dataProvider = "equivalences")
    public void testEquivalences(String dir, String[] files) throws Exception {
        containsEquivalences(dir, files);
    }

    @Override
    protected OntologyMatcher createMatcher() {
        return new ParisOntologyMatcher();
    }
}