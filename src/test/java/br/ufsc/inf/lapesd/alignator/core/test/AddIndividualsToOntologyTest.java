package br.ufsc.inf.lapesd.alignator.core.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.alignator.core.Alignator;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.EntityLoader;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.ServiceDescription;

public class AddIndividualsToOntologyTest {

    private Alignator alignator;

    @Before
    public void configure() throws IOException {
        alignator = new Alignator();

        String ontology1 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/ontology1.owl"), "UTF-8");
        String serviceSrecription1String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/serviceDescription1.jsonld"), "UTF-8");
        ServiceDescription serviceDescription1 = new Gson().fromJson(serviceSrecription1String, ServiceDescription.class);
        serviceDescription1.setUriBase("service1");
        serviceDescription1.setServerPort("8081");
        serviceDescription1.setIpAddress("10.1.1.1");
        alignator.registerService(serviceDescription1, ontology1);

        String ontology2 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/ontology2.owl"), "UTF-8");
        String serviceSrecription2String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/serviceDescription2.jsonld"), "UTF-8");
        ServiceDescription serviceDescription2 = new Gson().fromJson(serviceSrecription2String, ServiceDescription.class);
        serviceDescription2.setUriBase("service2");
        serviceDescription2.setServerPort("8082");
        serviceDescription2.setIpAddress("10.1.1.1");
        alignator.registerService(serviceDescription2, ontology2);

        String ontology3 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/ontology3.owl"), "UTF-8");
        String serviceSrecription3String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/serviceDescription3.jsonld"), "UTF-8");
        ServiceDescription serviceDescription3 = new Gson().fromJson(serviceSrecription3String, ServiceDescription.class);
        serviceDescription3.setUriBase("service3");
        serviceDescription3.setServerPort("8083");
        serviceDescription3.setIpAddress("10.1.1.1");
        alignator.registerService(serviceDescription3, ontology3);

    }

    @Test
    public void mustAddAnIndividualToOntologyTypeDefined() throws IOException {
        TestableEntityLoader entityLoader = new TestableEntityLoader();
        alignator.setEntityLoader(entityLoader);
        String entity2 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/entity2.jsonld"), "UTF-8");
        alignator.loadEntitiesAndAlignOntologies(entity2);

        String expectedOnotology1String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/expectedEntityOntology1.owl"), "UTF-8");
        String expectedOnotology2String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/expectedEntity1Ontology2.owl"), "UTF-8");
        String expectedOnotology22String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/expectedEntity2Ontology2.owl"), "UTF-8");

        String ontology1WithIndividuals = alignator.getOntologyManager().getStringOntologyWithIndividuals("http://ontology1#");
        String ontology2WithIndividuals = alignator.getOntologyManager().getStringOntologyWithIndividuals("http://ontology2#");

        boolean ontology1EqualExpected = ontology1WithIndividuals.trim().contains(expectedOnotology1String.trim());
        boolean ontology2EqualExpected = ontology2WithIndividuals.trim().contains(expectedOnotology2String.trim());
        boolean ontology22EqualExpected = ontology2WithIndividuals.trim().contains(expectedOnotology22String.trim());

        Assert.assertTrue(ontology1EqualExpected);
        Assert.assertTrue(ontology2EqualExpected);
        Assert.assertTrue(ontology22EqualExpected);

    }

    @Test
    public void mustAddAnIndividualToOntologyTypeNotDefined() throws IOException {
        // TODO
    }

    class TestableEntityLoader extends EntityLoader {

        @Override
        public List<String> loadEntitiesFromServices(String exampleOfEntity, ServiceDescription semanticMicroserviceDescription) {
            List<String> entities = new ArrayList<>();

            try {
                String entity1 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/entity1.jsonld"), "UTF-8");
                String entity2 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/entity2.jsonld"), "UTF-8");

                if (semanticMicroserviceDescription.getServerPort().equals("8081")) {
                    entities.add(entity1);
                }
                if (semanticMicroserviceDescription.getServerPort().equals("8082")) {
                    entities.add(entity2);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return entities;
        }
    }
}
