package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import br.ufsc.inf.lapesd.alignator.core.Utils;
import com.google.common.base.Preconditions;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL2;
import org.testng.Assert;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;

public abstract class OntologyMatcherTestBase {

    public void containsEquivalences(String dir, String[] files) throws Exception {
        Preconditions.checkArgument(files.length % 2 == 0);
        List<String> ontologyPaths = new ArrayList<>();
        List<String> dataPaths = new ArrayList<>();
        String equivalencePath = "alignments/" + dir + "/equivalences.ttl";
        for (int i = 0; i < files.length; i+=2) {
            dataPaths.add("alignments/"+dir+"/"+files[i]);
            ontologyPaths.add("alignments/"+files[i+1]);
        }
        containsEquivalences(ontologyPaths, dataPaths, equivalencePath);
    }

    public void containsEquivalences(@Nonnull List<String> ontologyPaths,
                                     @Nonnull List<String> dataPaths,
                                     @Nonnull String equivalencesPath) throws Exception {
        Preconditions.checkArgument(ontologyPaths.size() == dataPaths.size());
        Preconditions.checkArgument(ontologyPaths.size() >= 2);

        Model merged = Utils.load(ontologyPaths.toArray(new String[0]));
        List<Model> ontologies = new ArrayList<>(ontologyPaths.size());
        for (int i = 0; i < ontologyPaths.size(); i++)
            ontologies.add(Utils.load(ontologyPaths.get(i), dataPaths.get(i)));

        Model eqs = Utils.load(equivalencesPath);
        Set<Statement> expected = eqs.listStatements().toSet();

        List<Alignment> alignments = createMatcher().align(merged, ontologies);

        Set<Statement> hits, misses;
        hits = expected.stream().filter(merged::contains).collect(Collectors.toSet());
        misses = expected.stream().filter(s -> !merged.contains(s)).collect(Collectors.toSet());
        Assert.assertEquals(misses, Collections.emptySet(),
                misses.size() + " misses, hits="+hits);

        for (Statement s : expected) {
            if (!s.getObject().isURIResource()) continue;
            String uri1 = s.getSubject().getURI();
            String uri2 = s.getResource().getURI();
            boolean ok = alignments.stream()
                    .anyMatch(a -> a.getUri1().equals(uri1) && a.getUri2().equals(uri2));
            ok |= alignments.stream()
                    .anyMatch(a -> a.getUri2().equals(uri1) && a.getUri1().equals(uri2));
            Assert.assertTrue(ok, "No Alignment for " + s);
        }
    }

    protected abstract OntologyMatcher createMatcher();
}
