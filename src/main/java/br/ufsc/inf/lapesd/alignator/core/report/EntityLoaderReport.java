package br.ufsc.inf.lapesd.alignator.core.report;

import java.util.ArrayList;
import java.util.List;

import br.ufsc.inf.lapesd.alignator.core.Alignment;

public class EntityLoaderReport {

    private int executionId;
    private int numberOfLoadedEntities;
    private int numberOfCharsLoadedEntities;
    private List<Alignment> alignments = new ArrayList<>();

    public int getExecutionId() {
        return executionId;
    }

    public void setExecutionId(int executionId) {
        this.executionId = executionId;
    }

    public int getNumberOfLoadedEntities() {
        return numberOfLoadedEntities;
    }

    public void setNumberOfLoadedEntities(int numberOfLoadedEntities) {
        this.numberOfLoadedEntities = numberOfLoadedEntities;
    }

    public int getNumberOfCharsLoadedEntities() {
        return numberOfCharsLoadedEntities;
    }

    public void setNumberOfCharsLoadedEntities(int numberOfCharsLoadedEntities) {
        this.numberOfCharsLoadedEntities = numberOfCharsLoadedEntities;
    }

    public List<Alignment> getAlignments() {
        return alignments;
    }

    public void setAlignments(List<Alignment> alignments) {
        this.alignments = alignments;
    }

    @Override
    public String toString() {
        return "Report [executionId=" + executionId + ", numberOfLoadedEntities=" + numberOfLoadedEntities + ", numberOfCharsLoadedEntities=" + numberOfCharsLoadedEntities + ", alignments="
                + alignments + "]";
    }

}
