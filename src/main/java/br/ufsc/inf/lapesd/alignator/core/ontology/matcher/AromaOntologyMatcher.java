package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.springframework.stereotype.Component;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.aroma.AROMA;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;

import static br.ufsc.inf.lapesd.alignator.core.ontology.matcher.MatcherUtils.incorporateAlignments;
import static br.ufsc.inf.lapesd.alignator.core.ontology.matcher.MatcherUtils.serialize;

@Component
public class AromaOntologyMatcher implements OntologyMatcher {

    @Override
    public List<Alignment> align(@Nonnull Model mergedModel,
                                 @Nonnull Collection<Model> ontologies) throws IOException {
        Preconditions.checkArgument(ontologies.size() >= 2);
        List<Alignment> allAlignments = new ArrayList<>();

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tempOntologies").toFile();

            List<File> ontologyFiles = serialize(ontologies, tempDir, Lang.RDFXML);
            for (List<File> c : new Combinations<>(ontologyFiles, 2)) {
                List<Alignment> alignments = align(c.get(0), c.get(1));
                incorporateAlignments(mergedModel, allAlignments, alignments);
            }
        } finally {
            if (tempDir != null) FileUtils.deleteDirectory(tempDir);
        }

        return allAlignments;
    }

    @PreDestroy
    @Override
    public void close() {
        /* nothing */
    }

    private List<Alignment> align(File pathToOntology1, File pathToOntology2) {
        List<Alignment> alignments = new ArrayList<>();

        try {
            Properties p = new Properties();

            AROMA align = new AROMA();
            align.skos = "true".equals(p.getProperty("skos"));
            URI ontoURI1 = (pathToOntology1).toURI();
            URI ontoURI2 = (pathToOntology2).toURI();
            align.init(ontoURI1, ontoURI2);
            align.align(new ObjectAlignment(), p);
            Enumeration<Cell> elements = align.getElements();
            while (elements.hasMoreElements()) {
                Cell nextElement = elements.nextElement();
                Alignment alignment = new Alignment();
                alignment.setRelation(nextElement.getRelation().getRelation());
                alignment.setStrength(nextElement.getStrength());
                alignment.setUri1(nextElement.getObject1AsURI().toString());
                alignment.setUri2(nextElement.getObject2AsURI().toString());
                alignments.add(alignment);
            }
        } catch (AlignmentException e) {
            e.printStackTrace();
        }

        return alignments;
    }
}
