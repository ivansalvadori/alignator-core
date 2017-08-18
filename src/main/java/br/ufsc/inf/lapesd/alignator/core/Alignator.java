package br.ufsc.inf.lapesd.alignator.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import br.ufsc.inf.lapesd.alignator.core.ontology.matcher.Combinations;
import br.ufsc.inf.lapesd.alignator.core.ontology.matcher.OntologyMatcher;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import br.ufsc.inf.lapesd.alignator.core.entity.loader.EntityLoader;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.ServiceDescription;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyManager;
import br.ufsc.inf.lapesd.alignator.core.report.EntityLoaderReport;
import br.ufsc.inf.lapesd.alignator.core.report.OntologyManagerReport;

@Component
@Scope("prototype")
public class Alignator {
    private final String MERGED_ONT_FILENAME = "alignator-merged-ontology.owl";
    private int executionCount = 0;
    private List<EntityLoaderReport> entityLoaderReportList = new ArrayList<>();
    private List<OntologyManagerReport> ontologyManagerReportList = new ArrayList<>();

    @Autowired
    private OntologyMatcher ontologyMatcher;

    @Autowired
    private OntologyManager ontologyManager = new OntologyManager();

    @Autowired
    private EntityLoader entityLoader = new EntityLoader();

    private final Model mergedModel;

    private Map<String, List<ServiceDescription>> mapOntologyBaseNamespaceServiceDesciptions = new HashMap<>();

    public Alignator() {
        mergedModel = ModelFactory.createDefaultModel();

        if (new File(MERGED_ONT_FILENAME).exists()) {
            try (FileInputStream in = new FileInputStream(MERGED_ONT_FILENAME)) {
                RDFDataMgr.read(mergedModel, in, Lang.RDFXML);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
        Model currentModel = ModelFactory.createDefaultModel();
        StringReader sr2 = new StringReader(ontology);
        currentModel.read(sr2, null, "RDF/XML");

        mergedModel.add(currentModel);
        try (FileOutputStream out = new FileOutputStream(MERGED_ONT_FILENAME)) {
            RDFDataMgr.write(out, mergedModel, Lang.RDFXML);
        } catch (IOException ex) {
            ex.printStackTrace();
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
                List<String> loadedEntities;
                loadedEntities = entityLoader.loadEntitiesFromServices(exampleOfEntity, semanticMicroserviceDescription);
                loadedEntities.add(exampleOfEntity);
                ontologyManager.addEntitiesToOntology(loadedEntities);
                totalLoadedEntities.addAll(loadedEntities);
            }
        }

        Collection<Model> ontologies = ontologyManager.getAllOntologiesWithEntities();

        List<Alignment> alignments = null;
        long matcherTime;

        Date matcherStart = new Date();
        try {
            alignments = ontologyMatcher.align(mergedModel, ontologies);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date matcherFinish = new Date();
        matcherTime = matcherFinish.getTime() - matcherStart.getTime();

        try (FileOutputStream out = new FileOutputStream(MERGED_ONT_FILENAME)) {
            RDFDataMgr.write(out, mergedModel, Lang.RDFXML);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Date alignatorFinish = new Date();        
        long alignatorTime = alignatorFinish.getTime() - alignatorStart.getTime();
        
        this.executionCount++;
        createEntityLoaderReport(totalLoadedEntities, alignments, matcherTime, alignatorTime);
        crateOntologyManagerReport();
    }

    private void crateOntologyManagerReport() {
        Set<String> namespaces = this.ontologyManager.getRegisteredONtologiesNamespaces();
        for (String namespace : namespaces) {
            Model ontModel = ontologyManager.getOntologyWithIndividuals(namespace);
            int numberOfindividuals = ontologyManager.countIndividuals(ontModel);
            int chars = ontModel.toString().length();

            OntologyManagerReport ontologyManagerReport = new OntologyManagerReport();
            ontologyManagerReport.setExecutionId(executionCount);
            ontologyManagerReport.setNumberOfCharsOntologyModel(chars);
            ontologyManagerReport.setNumberOfIndividuals(numberOfindividuals);
            ontologyManagerReport.setOntologyBaseUri(namespace);
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
