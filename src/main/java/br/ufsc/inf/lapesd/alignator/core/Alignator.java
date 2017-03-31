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

import br.ufsc.inf.lapesd.alignator.core.entity.loader.EntityLoader;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.ServiceDescription;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyAlreadyRegisteredException;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyManager;
import br.ufsc.inf.lapesd.alignator.core.ontology.matcher.AromaOntologyMatcher;
import br.ufsc.inf.lapesd.alignator.core.report.EntityLoaderReport;
import br.ufsc.inf.lapesd.alignator.core.report.OntologyManagerReport;

public class Alignator {

    private int executionCount = 0;
    private List<EntityLoaderReport> entityLoaderReportList = new ArrayList<>();
    private List<OntologyManagerReport> ontologyManagerReportList = new ArrayList<>();

    private AromaOntologyMatcher aromaOntologyMatcher = new AromaOntologyMatcher();
    private OntologyManager ontologyManager = new OntologyManager();
    private EntityLoader entityLoader = new EntityLoader();
    private Map<String, List<ServiceDescription>> mapOntologyBaseNamespaceServiceDesciptions = new HashMap<>();

    public void setEntityLoader(EntityLoader entityLoader) {
        this.entityLoader = entityLoader;
    }

    public void registerService(ServiceDescription semanticMicroserviceDescription, String ontology) {
        try {
            String baseNamespace = ontologyManager.registerOntology(ontology);

            List<ServiceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
            if (serviceDescriptions == null) {
                serviceDescriptions = new ArrayList<>();
            }
            serviceDescriptions.add(semanticMicroserviceDescription);
            mapOntologyBaseNamespaceServiceDesciptions.put(baseNamespace, serviceDescriptions);

            updateMergedOntology(ontology);
        } catch (OntologyAlreadyRegisteredException e) {
        }
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

        if (this.ontologyManager.getAllOntologiesWithEntities().size() == 1) {
            return;
        }

        List<String> totalLoadedEntities = new ArrayList<>();

        Set<String> ontologyBaseNamespaces = mapOntologyBaseNamespaceServiceDesciptions.keySet();
        for (String baseNamespace : ontologyBaseNamespaces) {
            List<ServiceDescription> serviceDescriptions = mapOntologyBaseNamespaceServiceDesciptions.get(baseNamespace);
            for (ServiceDescription semanticMicroserviceDescription : serviceDescriptions) {
                List<String> loadedEntities = entityLoader.loadEntitiesFromServices(exampleOfEntity, semanticMicroserviceDescription);
                loadedEntities.add(exampleOfEntity);
                ontologyManager.addEntitiesToOntology(loadedEntities);
                totalLoadedEntities.addAll(loadedEntities);
            }
        }

        Collection<String> allOntologiesWithEntities = ontologyManager.getAllStringOntologiesWithEntities();

        Date start = new Date();
        List<Alignment> alignments = aromaOntologyMatcher.align(allOntologiesWithEntities);
        Date finish = new Date();

        long matcherTime = finish.getTime() - start.getTime();

        this.executionCount++;
        createEntityLoaderReport(totalLoadedEntities, alignments, matcherTime);
        crateOntologyManagerReport();
    }

    private void crateOntologyManagerReport() {
        Collection<OntModel> allOntologiesWithEntities = this.ontologyManager.getAllOntologiesWithEntities();
        for (OntModel ontModel : allOntologiesWithEntities) {
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

    private void createEntityLoaderReport(List<String> loadedEntities, List<Alignment> alignments, long matcherTime) {
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

        this.entityLoaderReportList.add(report);

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
