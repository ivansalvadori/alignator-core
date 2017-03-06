package br.ufsc.inf.lapesd.alignator.core.ontology.manager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.JenaException;

public class OntologyManager {

    private Map<String, OntModel> mapPrefixOriginalOntology = new HashMap<>();
    private Map<String, OntModel> mapPrefixOntologyWithIndividuals = new HashMap<>();

    /**
     * @param ontology
     *            is the string representation of an ontology
     * @return the ontology's base namespace
     */
    public String registerOntology(String ontology) {
        String baseNamespace = getBaseNamespace(ontology);

        if (mapPrefixOriginalOntology.containsKey(baseNamespace)) {
            throw new OntologyAlreadyRegisteredException();
        }

        mapPrefixOriginalOntology.put(baseNamespace, loadOntology(ontology));
        System.out.println(String.format("Ontology with base namespace <%s> has been sucessfully registered!", baseNamespace));
        return baseNamespace;
    }

    private OntModel loadOntology(String ontology) {
        OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        try {
            InputStream in = new ByteArrayInputStream(ontology.getBytes(StandardCharsets.UTF_8));

            try {
                ontoModel.read(in, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (JenaException je) {
            System.err.println("ERROR" + je.getMessage());
            je.printStackTrace();
        }

        return ontoModel;
    }

    public void addEntitiesToOntology(List<String> entities, String ontologyBaseNamespace) {

    }

    private String getBaseNamespace(String ontology) {
        OntModel ontoModel = loadOntology(ontology);
        return ontoModel.getNsPrefixURI("");
    }

    public List<String> getAllOntologiesWithEntities() {
        return null;
    }

}
