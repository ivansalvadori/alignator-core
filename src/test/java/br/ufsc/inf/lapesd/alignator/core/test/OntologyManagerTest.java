package br.ufsc.inf.lapesd.alignator.core.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyAlreadyRegisteredException;
import br.ufsc.inf.lapesd.alignator.core.ontology.manager.OntologyManager;

public class OntologyManagerTest {

    @Test(expected = OntologyAlreadyRegisteredException.class)
    public void mustPreventRegisteringTheSameOntologyTwice() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/ontologyManager/registry/domainOntology.owl"), "UTF-8");
        OntologyManager ontologyManager = new OntologyManager();
        ontologyManager.registerOntology(ontology);
        ontologyManager.registerOntology(ontology);

    }

}
