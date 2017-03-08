package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.aroma.AROMA;

public class AromaOntologyMatcher {

    public List<Alignment> align(Collection<String> ontologies) {
        final OntModel mergedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        List<String> ontologyFiles = new ArrayList<>();

        for (String ontology : ontologies) {
            String ontologyFilename = "tempOntologies/" + UUID.randomUUID().toString();
            ontologyFiles.add(ontologyFilename);

            StringReader sr = new StringReader(ontology);
            final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            model.read(sr, null, "N3");

            try (FileWriter out = new FileWriter(ontologyFilename)) {
                model.write(out, "RDF/XML");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            mergedModel.add(model);

            try (FileWriter out = new FileWriter("alignator-merged-ontology.owl")) {
                model.write(out, "RDF/XML");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        Combination combination = new Combination();
        if (ontologies.size() < 2) {
            return null;
        }
        List<List<String>> combinationOfOntologies = combination.combine(ontologyFiles, 2);
        for (List<String> aCombination : combinationOfOntologies) {
            String ontologyFile0 = aCombination.get(0);
            String ontologyFile1 = aCombination.get(1);
            List<Alignment> alignments = align(ontologyFile0, ontologyFile1);
            for (Alignment alignment : alignments) {
                System.out.println(alignment);
                mergedModel.getOntProperty(alignment.getUri1()).addEquivalentProperty(mergedModel.getOntProperty(alignment.getUri2()));
                mergedModel.getOntProperty(alignment.getUri2()).addEquivalentProperty(mergedModel.getOntProperty(alignment.getUri1()));
            }
        }

        try (FileWriter out = new FileWriter("alignator-merged-ontology.owl")) {
            mergedModel.write(out, "RDF/XML");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

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
