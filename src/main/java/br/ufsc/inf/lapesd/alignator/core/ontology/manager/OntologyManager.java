package br.ufsc.inf.lapesd.alignator.core.ontology.manager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OntologyManager {
    private static final Logger logger = LoggerFactory.getLogger(OntologyManager.class);

    private Map<String, String> mapAddedIndividuals = new HashMap<>();
    private Map<String, Model> mapPrefixOriginalOntology = new HashMap<>();
    private Map<String, Model> mapPrefixOntologyWithIndividuals = new HashMap<>();
    /* weak keys as countIndividuals() is public and <bold>could</bold> receive alien models */
    private WeakHashMap<Model, Integer> individualsCount = new WeakHashMap<>();
    private int ontologyMaxIndividuals = 1000;

    private static final Set<Resource> notIndividualClasses;

    static {
        notIndividualClasses = new HashSet<>();
        notIndividualClasses.addAll(Arrays.asList(OWL2.Class, RDFS.Class));
    }

    /**
     * @param ontologyString
     *            is the string representation of an ontology
     * @return the registered ontology's base namespace
     */
    public String registerOntology(String ontologyString) {
        Model ontology = loadOntology(ontologyString);
        String baseNamespace = getBaseNamespace(ontology);

        if (mapPrefixOriginalOntology.containsKey(baseNamespace)) {
            return baseNamespace;
            // throw new OntologyAlreadyRegisteredException();
        }

        mapPrefixOriginalOntology.put(baseNamespace, ontology);
        Model model = ModelFactory.createDefaultModel();
        model.add(ontology);
        mapPrefixOntologyWithIndividuals.put(baseNamespace, model);
        System.out.println(String.format("Ontology with base namespace <%s> has been sucessfully registered!", baseNamespace));
        return baseNamespace;
    }

    private Model loadOntology(String ontology) {
        Model ontoModel = ModelFactory.createDefaultModel();

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
        if (this.mapAddedIndividuals.get(hashString(entity)) != null)
            return;

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
        mapAddedIndividuals.put(hashString(entity), entity);
    }

    private static class OntologyMatch {
        final String base;
        final Resource ontClass;

        private OntologyMatch(String base, Resource ontClass) {
            Preconditions.checkArgument(ontClass.getModel() != null);
            this.base = base;
            this.ontClass = ontClass;
        }
    }

    public Resource createIndividual(OntologyMatch match) {
        Model model = match.ontClass.getModel();
        Integer old = individualsCount.get(model);
        if (old != null)
            individualsCount.put(model, old+1);
        return model.createResource(match.ontClass);
    }

    public int countIndividuals(Model model) {
        Integer cached = individualsCount.get(model);
        if (cached != null) return cached;

        int count = 0;
        Set<Resource> visited = new HashSet<>();
        StmtIterator it = model.listStatements(null, RDF.type, (RDFNode) null);
        while (it.hasNext()) {
            Statement s = it.next();
            if (visited.contains(s.getSubject()) || !s.getObject().isResource()) continue;

            visited.add(s.getSubject());
            Resource clazz = s.getResource();
            if (!notIndividualClasses.contains(clazz)) ++count;
        }

        individualsCount.put(model, count);
        return count;
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
        Model ontModel = this.getOntologyWithIndividuals(match.base);

        // Ontology size control
        if (countIndividuals(ontModel) > this.ontologyMaxIndividuals) {
            ontModel.removeAll();
            individualsCount.remove(ontModel);
            ontModel.add(this.mapPrefixOriginalOntology.get(match.base));
        }

        Resource individual = createIndividual(match);
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
        for (Entry<String, Model> e : mapPrefixOntologyWithIndividuals.entrySet()) {
            for (RDFNode type : types) {
                Resource ontClass = e.getValue().createResource(type.asResource().getURI());
                if(ontClass != null)
                    return new OntologyMatch(e.getKey(), ontClass);
            }
        }
        return null;
    }

    private String getBaseNamespace(Model model) {
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

    private String getMostFrequentSubjectNs(Model model) {
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

        Collection<Model> ontologies = this.mapPrefixOntologyWithIndividuals.values();
        for (Model ontModel : ontologies) {
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

    public Collection<Model> getAllOntologiesWithEntities() {
        return this.mapPrefixOntologyWithIndividuals.values();
    }

    public Model getOntologyWithIndividuals(String baseNamespace) {
        return this.mapPrefixOntologyWithIndividuals.get(baseNamespace);
    }

    public String getStringOntologyWithIndividuals(String baseNamespace) {
        Model ontModel = this.mapPrefixOntologyWithIndividuals.get(baseNamespace);
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
