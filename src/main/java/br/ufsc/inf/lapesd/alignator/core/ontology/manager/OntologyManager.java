package br.ufsc.inf.lapesd.alignator.core.ontology.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.JenaException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OntologyManager {

    private Map<String, OntModel> mapPrefixOriginalOntology = new HashMap<>();
    private Map<String, OntModel> mapPrefixOntologyWithIndividuals = new HashMap<>();

    /**
     * @param ontology
     *            is the string representation of an ontology
     * @return the registered ontology's base namespace
     */
    public String registerOntology(String ontology) {
        String baseNamespace = getBaseNamespace(ontology);

        if (mapPrefixOriginalOntology.containsKey(baseNamespace)) {
            throw new OntologyAlreadyRegisteredException();
        }

        mapPrefixOriginalOntology.put(baseNamespace, loadOntology(ontology));
        mapPrefixOntologyWithIndividuals.put(baseNamespace, loadOntology(ontology));
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

    public void addEntitiesToOntology(List<String> entities) {
        for (String entity : entities) {
            createAndAddIndividual(entity);
        }
    }

    private Individual createAndAddIndividual(String entity) {
        Individual individual = null;

        JsonElement parsedEntity = new JsonParser().parse(entity);
        if (parsedEntity.isJsonObject()) {
            JsonObject jsonObject = parsedEntity.getAsJsonObject();
            Set<Entry<String, JsonElement>> propertiesAndValues = jsonObject.entrySet();
            boolean hasType = false;
            for (Entry<String, JsonElement> entry : propertiesAndValues) {
                if (entry.getKey().equals("@type")) {
                    hasType = true;
                }
            }
            if (!hasType) {
                // TODO take a look at the ontology to check if this property
                // has range. Range can be used as @type
                return null;
            }
            String individualType = jsonObject.get("@type").getAsString();

            OntModel ontModel = getOntology(individualType);
            OntClass classOfIndividual = ontModel.getOntClass(individualType);
            individual = classOfIndividual.createIndividual();

            for (Entry<String, JsonElement> entityPropertyAndValue : propertiesAndValues) {
                String propertyKey = entityPropertyAndValue.getKey();
                if (propertyKey.equals("@id") || propertyKey.equals("@type")) {
                    continue;
                }
                JsonElement value = entityPropertyAndValue.getValue();
                if (value.isJsonObject()) {
                    Individual innerIndividual = createAndAddIndividual(value.toString());
                    if (innerIndividual != null) {
                        individual.addProperty(ontModel.getProperty(propertyKey), innerIndividual);
                    }
                }

                if (value.isJsonPrimitive()) {
                    String propertyValue = value.getAsString();
                    propertyValue = StringEscapeUtils.escapeXml11(propertyValue);
                    individual.addProperty(ontModel.getProperty(propertyKey), propertyValue);
                }
            }
        }
        return individual;
    }

    private OntModel getOntology(String entityType) {
        Collection<OntModel> registeredOntologies = mapPrefixOntologyWithIndividuals.values();
        for (OntModel registeredOntology : registeredOntologies) {
            OntClass ontClass = registeredOntology.getOntClass(entityType);
            if (ontClass != null) {
                return registeredOntology;
            }
        }

        return null;
    }

    private String getBaseNamespace(String ontology) {
        OntModel ontoModel = loadOntology(ontology);
        return ontoModel.getNsPrefixURI("");
    }

    public List<String> getAllStringOntologiesWithEntities() {
        List<String> ontologiesWithEntities = new ArrayList<>();

        Collection<OntModel> ontologies = this.mapPrefixOntologyWithIndividuals.values();
        for (OntModel ontModel : ontologies) {
            try (StringWriter stringWriter = new StringWriter()) {
                ontModel.write(stringWriter, "N3");
                String writtenText = stringWriter.toString();
                ontologiesWithEntities.add(writtenText);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return ontologiesWithEntities;
    }

    public Collection<OntModel> getAllOntologiesWithEntities() {
        return this.mapPrefixOntologyWithIndividuals.values();
    }

    public OntModel getOntologyWithIndividuals(String baseNamespace) {
        return this.mapPrefixOntologyWithIndividuals.get(baseNamespace);
    }

    public String getStringOntologyWithIndividuals(String baseNamespace) {
        OntModel ontModel = this.mapPrefixOntologyWithIndividuals.get(baseNamespace);
        try (StringWriter stringWriter = new StringWriter()) {
            ontModel.write(stringWriter, "RDF/XML");
            String writtenText = stringWriter.toString();
            return writtenText;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;

    }

}
