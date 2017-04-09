package br.ufsc.inf.lapesd.alignator.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import br.ufsc.inf.lapesd.alignator.core.entity.loader.EntityLoader;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.ServiceDescription;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyManager;
import br.ufsc.inf.lapesd.alignator.core.ontology.matcher.AromaOntologyMatcher;
import br.ufsc.inf.lapesd.alignator.core.report.EntityLoaderReport;
import br.ufsc.inf.lapesd.alignator.core.report.OntologyManagerReport;

@Component
@Scope("prototype")
public class Alignator {

    private int executionCount = 0;
    private List<EntityLoaderReport> entityLoaderReportList = new ArrayList<>();
    private List<OntologyManagerReport> ontologyManagerReportList = new ArrayList<>();

    @Autowired
    private AromaOntologyMatcher aromaOntologyMatcher = new AromaOntologyMatcher();

    @Autowired
    private OntologyManager ontologyManager = new OntologyManager();

    @Autowired
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
                mergedModel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException e) {
            try (FileWriter out = new FileWriter("alignator-merged-ontology.owl")) {
                currentModel.write(out, "RDF/XML");
                currentModel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void loadEntitiesAndAlignOntologies(String exampleOfEntity) {
        Date alignatorStart = new Date();

        if (this.ontologyManager.getAllOntologiesWithEntities().size() == 1) {
            return;
        }

        List<String> totalLoadedEntities = new ArrayList<>();

        Set<String> ontologyBaseNamespaces = mapOntologyBaseNamespaceServiceDesciptions.keySet();
        for (String baseNamespace : ontologyBaseNamespaces) {
            List<ServiceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
            for (ServiceDescription semanticMicroserviceDescription : serviceDescriptions) {
                List<String> loadedEntities = new ArrayList<>();
                loadedEntities = entityLoader.loadEntitiesFromServices(exampleOfEntity, semanticMicroserviceDescription);
                loadedEntities.add(exampleOfEntity);
                ontologyManager.addEntitiesToOntology(loadedEntities);
                totalLoadedEntities.addAll(loadedEntities);
            }
        }

        Collection<OntModel> allOntologiesWithEntities = ontologyManager.getAllOntologiesWithEntities();

        List<Alignment> alignments = null;
        long matcherTime = 0;

        Date matcherStart = new Date();
        alignments = aromaOntologyMatcher.align(allOntologiesWithEntities);
        Date matcherFinish = new Date();
        matcherTime = matcherFinish.getTime() - matcherStart.getTime();

        Date alignatorFinish = new Date();        
        long alignatorTime = alignatorFinish.getTime() - alignatorStart.getTime();
        
        this.executionCount++;
        createEntityLoaderReport(totalLoadedEntities, alignments, matcherTime, alignatorTime);
        crateOntologyManagerReport();
    }

    private void crateOntologyManagerReport() {
        Set<String> namespaces = this.ontologyManager.getRegisteredONtologiesNamespaces();
        for (String namespace : namespaces) {
            OntModel ontModel = ontologyManager.getOntologyWithIndividuals(namespace);
            String ontologyBaseUri = ontModel.getNsPrefixURI("");
            int numberOfindividuals = ontModel.listIndividuals().toList().size();
            int chars = ontModel.toString().length();

            OntologyManagerReport ontologyManagerReport = new OntologyManagerReport();
            ontologyManagerReport.setExecutionId(executionCount);
            ontologyManagerReport.setNumberOfCharsOntologyModel(chars);
            ontologyManagerReport.setNumberOfIndividuals(numberOfindividuals);
            ontologyManagerReport.setOntologyBaseUri(ontologyBaseUri);
            this.ontologyManagerReportList.add(ontologyManagerReport);
        }
    }

    private EntityLoaderReport createEntityLoaderReport(List<String> loadedEntities, List<Alignment> alignments, long matcherTime, long alignatorTime) {
        EntityLoaderReport report = new EntityLoaderReport();
        report.setExecutionId(this.executionCount);

        report.setNumberOfLoadedEntities(loadedEntities.size());

        int totalChars = 0;
        for (String entity : loadedEntities) {
            totalChars = totalChars + entity.length();
        }

        report.setNumberOfCharsLoadedEntities(totalChars);
        report.setAlignments(alignments);
        report.setMatcherElapsedTime(matcherTime);
        report.setAlignatorElapsedTime(alignatorTime);

        this.entityLoaderReportList.add(report);
        return report;

    }

    public OntologyManager getOntologyManager() {
        return ontologyManager;
    }

    public List<EntityLoaderReport> getEntityLoaderReportList() {
        return entityLoaderReportList;
    }

    public List<OntologyManagerReport> getOntologyManagerReportList() {
        return ontologyManagerReportList;
    }
}
