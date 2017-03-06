package br.ufsc.inf.lapesd.alignator.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.ufsc.inf.lapesd.alignator.core.entity.loader.EntityLoader;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyManager;
import br.ufsc.inf.lapesd.alignator.core.ontology.matcher.AromaOntologyMatcher;

public class Alignator {

    private AromaOntologyMatcher aromaOntologyMatcher = new AromaOntologyMatcher();
    private OntologyManager ontologyManager = new OntologyManager();
    private EntityLoader entityLoader = new EntityLoader();
    private Map<String, List<SemanticMicroserviceDescription>> mapOntologyBaseNamespaceServiceDesciptions = new HashMap<>();

    public void setEntityLoader(EntityLoader entityLoader) {
        this.entityLoader = entityLoader;
    }

    public void registerService(SemanticMicroserviceDescription semanticMicroserviceDescription, String ontology) {
        String baseNamespace = ontologyManager.registerOntology(ontology);

        List<SemanticMicroserviceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
        if (serviceDescriptions == null) {
            serviceDescriptions = new ArrayList<>();
        }
        serviceDescriptions.add(semanticMicroserviceDescription);
        mapOntologyBaseNamespaceServiceDesciptions.put(baseNamespace, serviceDescriptions);
    }

    public void loadEntitiesAndAlignOntologies(String exampleOfEntity) {
        Set<String> ontologyBaseNamespaces = mapOntologyBaseNamespaceServiceDesciptions.keySet();
        for (String baseNamespace : ontologyBaseNamespaces) {
            List<SemanticMicroserviceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
            for (SemanticMicroserviceDescription semanticMicroserviceDescription : serviceDescriptions) {
                List<String> loadedEntities = entityLoader.loadEntitiesFromServices(exampleOfEntity, semanticMicroserviceDescription);
                ontologyManager.addEntitiesToOntology(loadedEntities, baseNamespace);
            }
        }

        List<String> allOntologiesWithEntities = ontologyManager.getAllOntologiesWithEntities();
        aromaOntologyMatcher.align(allOntologiesWithEntities);
    }
}
