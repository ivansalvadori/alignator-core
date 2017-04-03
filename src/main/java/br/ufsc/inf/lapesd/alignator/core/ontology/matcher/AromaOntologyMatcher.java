package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.springframework.stereotype.Component;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.aroma.AROMA;

@Component
public class AromaOntologyMatcher {

    public List<Alignment> align(Collection<OntModel> ontologies) {
        List<Alignment> allAlignments = new ArrayList<>();

        final OntModel mergedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        String mergedOntology;
        try {
            mergedOntology = new String(Files.readAllBytes(Paths.get("alignator-merged-ontology.owl")));
            StringReader sr = new StringReader(mergedOntology);
            mergedModel.read(sr, null, "RDF/XML");
            sr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> ontologyFiles = new ArrayList<>();

        File directory = new File(String.valueOf("tempOntologies"));
        if (!directory.exists()) {
            directory.mkdir();
        }

        int x = 0;
        for (OntModel ontology : ontologies) {
            // String ontologyFilename = "tempOntologies/" +
            // UUID.randomUUID().toString();
            String ontologyFilename = "tempOntologies/" + x++;

            ontologyFiles.add(ontologyFilename);

            try (FileWriter out = new FileWriter(ontologyFilename)) {
                ontology.write(out, "RDF/XML");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (ontologies.size() < 2) {
            return null;
        }

        Combinations<String> combinations = new Combinations<>(ontologyFiles, 2);

        /*
         * List<List<String>> combinationOfOntologies = new ArrayList<>();
         * List<String> a = new ArrayList<>(); a.add("tempOntologies/0");
         * a.add("tempOntologies/1");
         * 
         * List<String> b = new ArrayList<>(); b.add("tempOntologies/0");
         * b.add("tempOntologies/2");
         * 
         * List<String> c = new ArrayList<>(); c.add("tempOntologies/1");
         * c.add("tempOntologies/2");
         * 
         * combinationOfOntologies.add(a); combinationOfOntologies.add(b);
         * combinationOfOntologies.add(c);
         */

        for (List<String> aCombination : combinations) {
            String ontologyFile0 = aCombination.get(0);
            String ontologyFile1 = aCombination.get(1);
            List<Alignment> alignments = align(ontologyFile0, ontologyFile1);
            allAlignments.addAll(alignments);
            for (Alignment alignment : alignments) {
                // System.out.println(alignment);
                mergedModel.getOntProperty(alignment.getUri1()).addEquivalentProperty(mergedModel.getOntProperty(alignment.getUri2()));
                mergedModel.getOntProperty(alignment.getUri2()).addEquivalentProperty(mergedModel.getOntProperty(alignment.getUri1()));
            }
        }

        deleteTempFile();

        try (FileWriter out = new FileWriter("alignator-merged-ontology.owl")) {
            mergedModel.write(out, "RDF/XML");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mergedModel.close();

        return allAlignments;
    }

    public List<Alignment> align(String pathToOntology1, String pathToOntology2) {

        List<Alignment> alignments = new ArrayList<>();

        try {
            Properties p = new Properties();
            // p.setProperty("lexicalSim", "true");

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

    private void deleteTempFile() {
        Path rootPath = Paths.get("tempOntologies");
        try {
            Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
