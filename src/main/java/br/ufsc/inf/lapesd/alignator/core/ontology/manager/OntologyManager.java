package br.ufsc.inf.lapesd.alignator.core.ontology.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
            addIndividuals(entity);
        }
    }

    private void addIndividuals(String entity) {
        try {
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, IOUtils.toInputStream(entity, "UTF-8"), Lang.JSONLD);
            ResIterator it = model.listSubjectsWithProperty(RDF.type);
            while (it.hasNext()) {
                addIndividual(it.next());
            }

        } catch (IOException e) {
            logger.error("Exception when adding individuals", e);
        }
    }

    private static class OntologyMatch {
        final String base;
        final OntClass ontClass;

        private OntologyMatch(String base, OntClass ontClass) {
            this.base = base;
            this.ontClass = ontClass;
        }
    }

    private Resource addIndividual(Resource resource) {
        if (resource.hasProperty(RDF.type, OWL2.Class)
                || resource.hasProperty(RDF.type, RDF.Property)
                || resource.hasProperty(RDF.type, OWL2.DatatypeProperty)
                || resource.hasProperty(RDF.type, OWL2.ObjectProperty))
            return null;

        OntologyMatch match = findOntology(resource);
        if (match == null)
            return null;
        OntModel ontModel = this.getOntologyWithIndividuals(match.base);

        // Ontology size control
        if (ontModel.listIndividuals().toList().size() > this.ontologyMaxIndividuals) {
            ontModel.removeAll();
            ontModel.add(this.mapPrefixOriginalOntology.get(match.base));
        }

        Individual individual = match.ontClass.createIndividual();
        StmtIterator stmtIt = resource.listProperties();
        while (stmtIt.hasNext()) {
            Statement s = stmtIt.next();
            if (s.getObject().isLiteral()) {
                ontModel.add(individual, s.getPredicate(), s.getObject());
            } else {
                //TODO objects will always be bnodes, doesn't this affect alignment?
                Resource o = addIndividual(s.getResource());
                if (o != null)
                    ontModel.add(individual, s.getPredicate(), o);
            }
        }
        return individual;
    }

    private OntologyMatch findOntology(Resource resource) {
        //TODO: this relies solely on type statements, apply basic inference from properties ranges
        List<RDFNode> types = resource.getModel().listObjectsOfProperty(resource, RDF.type)
                .toList().stream().filter(RDFNode::isURIResource).collect(Collectors.toList());
        for (Entry<String, OntModel> e : mapPrefixOntologyWithIndividuals.entrySet()) {
            for (RDFNode type : types) {
                OntClass ontClass = e.getValue().getOntClass(type.asResource().getURI());
                if(ontClass != null)
                    return new OntologyMatch(e.getKey(), ontClass);
            }
        }
        return null;
    }

    private String getBaseNamespace(String ontology) {
        OntModel model = loadOntology(ontology);

        List<String> nss = new ArrayList<>();
        /* Alignator assumed xml:base was the ontology identifier. For backward compatibility,
         * this remains at the top priority. The following code only works when xml:base is
         * present and is not the last attribute of rdf:RDF in RDF/XML */
        nss.add(model.getNsPrefixURI(""));
        /* OWL2 XML does not require a named Ontology individual, neither requires it to be the
         * only one. It would also bre reasonable to state a object of rdfs:isDefinedBy to be an
         * owl:Ontology. */
        nss.add(model.listSubjectsWithProperty(RDF.type, OWL2.Ontology).toList()
                .stream().filter(Resource::isURIResource).findFirst().map(Resource::getURI)
                .map(u -> u.endsWith("#") ? u : u + "#")
                .orElse(null));
        /* The most frequent namespace among subjects is LIKELY to be the ontology prefix. */
        nss.add(getMostFrequentSubjectNs(model));

        String selected = nss.stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (nss.stream().filter(Objects::nonNull).distinct().count() > 1) {
            logger.warn("Conflicting ontology prefix hints: {} (baseURI) {} (single ow:Ontology) " +
                    "{} (most frequent subject NS). Selected {} as identifier",
                    new Object[]{nss.get(0), nss.get(1), nss.get(2), selected});
        }
        return selected;
    }

    private String getMostFrequentSubjectNs(OntModel model) {
        HashMap<String, Integer> histogram = new HashMap<>();
        ResIterator it = model.listSubjectsWithProperty(RDF.type);
        while (it.hasNext()) {
            Resource r = it.next();
            if (!r.isURIResource()) continue;
            String ns = r.getNameSpace();
            histogram.put(ns, histogram.getOrDefault(ns, 0)+1);
        }
        return histogram.entrySet().stream().max(Entry.comparingByValue())
                .map(Entry::getKey).orElse(null);
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

}
