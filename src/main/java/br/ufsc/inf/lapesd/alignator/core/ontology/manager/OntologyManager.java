package br.ufsc.inf.lapesd.alignator.core.ontology.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class OntologyManager {
    private static final Logger logger = LoggerFactory.getLogger(OntologyManager.class);

    private Map<String, String> mapAddedIndividuals = new HashMap<>();
    private Map<String, OntModel> mapPrefixOriginalOntology = new HashMap<>();
    private Map<String, OntModel> mapPrefixOntologyWithIndividuals = new HashMap<>();
    private int ontologyMaxIndividuals = 1000;

    /**
     * @param ontology
     *            is the string representation of an ontology
     * @return the registered ontology's base namespace
     */
    public String registerOntology(String ontology) {
        String baseNamespace = getBaseNamespace(ontology);

        if (mapPrefixOriginalOntology.containsKey(baseNamespace)) {
            return baseNamespace;
            // throw new OntologyAlreadyRegisteredException();
        }

        mapPrefixOriginalOntology.put(baseNamespace, loadOntology(ontology));
        mapPrefixOntologyWithIndividuals.put(baseNamespace, loadOntology(ontology));
        System.out.println(String.format("Ontology with base namespace <%s> has been sucessfully registered!", baseNamespace));
        return baseNamespace;
    }

    private OntModel loadOntology(String ontology) {
        OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);

        try (InputStream in = new ByteArrayInputStream(ontology.getBytes(StandardCharsets.UTF_8));) {
            ontoModel.read(in, null);
        } catch (Exception je) {
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
        if (this.mapAddedIndividuals.get(hashString(entity)) != null) {
            return null;
        }

        try {
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
                    // TODO take a look at the ontology to check if this
                    // property
                    // has range. Range can be used as @type
                    return null;
                }
                String individualType = jsonObject.get("@type").getAsString();

                OntModel ontModel = getOntology(individualType);
                if (ontModel == null) {
                    return null;
                }

                // Ontology size control
                int numberOfIndividuals = 0;
                ExtendedIterator<Individual> listIndividuals = ontModel.listIndividuals();
                if (listIndividuals != null) {
                    numberOfIndividuals = listIndividuals.toList().size();
                }

                if (numberOfIndividuals > this.ontologyMaxIndividuals) {
                    String baseName = ontModel.getNsPrefixURI("");
                    OntModel ontModelWithoutIndividuals = this.mapPrefixOriginalOntology.get(baseName);
                    ontModel.removeAll();
                    ontModel.add(ontModelWithoutIndividuals);
                }

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
        } catch (Exception e) {
            return null;
        }
        mapAddedIndividuals.put(hashString(entity), entity);
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
        OntModel model = loadOntology(ontology);

        List<String> nss = new ArrayList<>();
        /*
         * Alignator assumed xml:base was the ontology identifier. For backward
         * compatibility, this remains at the top priority. The following code
         * only works when xml:base is present and is not the last attribute of
         * rdf:RDF in RDF/XML
         */
        nss.add(model.getNsPrefixURI(""));
        /*
         * OWL2 XML does not require a named Ontology individual, neither
         * requires it to be the only one. It would also bre reasonable to state
         * a object of rdfs:isDefinedBy to be an owl:Ontology.
         */
        nss.add(model.listSubjectsWithProperty(RDF.type, OWL2.Ontology).toList().stream().filter(Resource::isURIResource).findFirst().map(Resource::getURI).map(u -> u.endsWith("#") ? u : u + "#")
                .orElse(null));
        /*
         * The most frequent namespace among subjects is LIKELY to be the
         * ontology prefix.
         */
        nss.add(getMostFrequentSubjectNs(model));

        String selected = nss.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (nss.stream().filter(Objects::nonNull).distinct().count() > 1) {
            logger.warn("Conflicting ontology prefix hints: {} (baseURI) {} (single ow:Ontology) " + "{} (most frequent subject NS). Selected {} as identifier",
                    new Object[] { nss.get(0), nss.get(1), nss.get(2), selected });
        }
        return selected;
    }

    private String getMostFrequentSubjectNs(OntModel model) {
        HashMap<String, Integer> histogram = new HashMap<>();
        ResIterator it = model.listSubjectsWithProperty(RDF.type);
        while (it.hasNext()) {
            Resource r = it.next();
            if (!r.isURIResource())
                continue;
            String ns = r.getNameSpace();
            histogram.put(ns, histogram.getOrDefault(ns, 0) + 1);
        }
        return histogram.entrySet().stream().max(Entry.comparingByValue()).map(Entry::getKey).orElse(null);
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

    public Set<String> getRegisteredONtologiesNamespaces() {
        return this.mapPrefixOntologyWithIndividuals.keySet();
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
        ontModel.close();
        return null;

    }

    public void setOntologyMaxIndividuals(int ontologyMaxIndividuals) {
        this.ontologyMaxIndividuals = ontologyMaxIndividuals;
    }

    private String hashString(String message) {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-1");
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
            return convertByteArrayToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    private String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

}
