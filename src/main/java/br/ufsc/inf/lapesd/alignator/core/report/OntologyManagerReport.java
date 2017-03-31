package br.ufsc.inf.lapesd.alignator.core.report;

public class OntologyManagerReport {
    private int executionId;
    private String ontologyBaseUri;
    private int numberOfIndividuals;
    private int numberOfCharsOntologyModel;

    public void setExecutionId(int executionId) {
        this.executionId = executionId;
    }

    public int getExecutionId() {
        return executionId;
    }

    public String getOntologyBaseUri() {
        return ontologyBaseUri;
    }

    public void setOntologyBaseUri(String ontologyBaseUri) {
        this.ontologyBaseUri = ontologyBaseUri;
    }

    public int getNumberOfIndividuals() {
        return numberOfIndividuals;
    }

    public void setNumberOfIndividuals(int numberOfIndividuals) {
        this.numberOfIndividuals = numberOfIndividuals;
    }

    public int getNumberOfCharsOntologyModel() {
        return numberOfCharsOntologyModel;
    }

    public void setNumberOfCharsOntologyModel(int numberOfCharsOntologyModel) {
        this.numberOfCharsOntologyModel = numberOfCharsOntologyModel;
    }

}
