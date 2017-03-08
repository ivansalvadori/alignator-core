package br.ufsc.inf.lapesd.alignator.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
