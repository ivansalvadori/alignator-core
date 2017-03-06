package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.aroma.AROMA;

public class AromaOntologyMatcher {

    public List<Alignment> align(List<String> ontologies) {
        return null;
    }

    public List<Alignment> align(String pathToOntology1, String pathToOntology2) {

        List<Alignment> alignments = new ArrayList<>();

        try {
            Properties p = new Properties();
            p.setProperty("lexicalSim", "true");

            AROMA align = new AROMA();
            align.skos = "true".equals(p.getProperty("skos"));
            URI ontoURI1 = (new File(pathToOntology1)).toURI();
            URI ontoURI2 = (new File(pathToOntology2)).toURI();
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
        }

        catch (AlignmentException e) {
            e.printStackTrace();
        }

        return alignments;
    }

}
