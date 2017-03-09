package br.ufsc.inf.lapesd.alignator.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import br.ufsc.inf.lapesd.alignator.core.entity.loader.EntityLoader;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.ServiceDescription;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyManager;
import br.ufsc.inf.lapesd.alignator.core.ontology.matcher.AromaOntologyMatcher;

public class Alignator {

    private AromaOntologyMatcher aromaOntologyMatcher = new AromaOntologyMatcher();
    private OntologyManager ontologyManager = new OntologyManager();
    private EntityLoader entityLoader = new EntityLoader();
    private Map<String, List<ServiceDescription>> mapOntologyBaseNamespaceServiceDesciptions = new HashMap<>();

    public void setEntityLoader(EntityLoader entityLoader) {
        this.entityLoader = entityLoader;
    }

    public void registerService(ServiceDescription semanticMicroserviceDescription, String ontology) {
        String baseNamespace = ontologyManager.registerOntology(ontology);

        List<ServiceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
        if (serviceDescriptions == null) {
            serviceDescriptions = new ArrayList<>();
        }
        serviceDescriptions.add(semanticMicroserviceDescription);
        mapOntologyBaseNamespaceServiceDesciptions.put(baseNamespace, serviceDescriptions);

        updateMergedOntology(ontology);
    }

    private void updateMergedOntology(String ontology) {
        
        OntModel currentModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        StringReader sr2 = new StringReader(ontology);
        currentModel.read(sr2, null, "RDF/XML");
        
        OntModel mergedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        try {
            String mergedOntology = new String(Files.readAllBytes(Paths.get("alignator-merged-ontology.owl")));
            StringReader sr = new StringReader(mergedOntology);
            mergedModel.read(sr, null, "RDF/XML");
            mergedModel.add(currentModel);
            try (FileWriter out = new FileWriter("alignator-merged-ontology.owl")) {
                mergedModel.write(out, "RDF/XML");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException e) {
            try (FileWriter out = new FileWriter("alignator-merged-ontology.owl")) {
                currentModel.write(out, "RDF/XML");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void loadEntitiesAndAlignOntologies(String exampleOfEntity) {
        Set<String> ontologyBaseNamespaces = mapOntologyBaseNamespaceServiceDesciptions.keySet();
        for (String baseNamespace : ontologyBaseNamespaces) {
            List<ServiceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
            for (ServiceDescription semanticMicroserviceDescription : serviceDescriptions) {
                List<String> loadedEntities = entityLoader.loadEntitiesFromServices(exampleOfEntity, semanticMicroserviceDescription);
                loadedEntities.add(exampleOfEntity);
                ontologyManager.addEntitiesToOntology(loadedEntities);
            }
        }

        Collection<String> allOntologiesWithEntities = ontologyManager.getAllStringOntologiesWithEntities();
        aromaOntologyMatcher.align(allOntologiesWithEntities);
    }

    public OntologyManager getOntologyManager() {
        return ontologyManager;
    }
}
