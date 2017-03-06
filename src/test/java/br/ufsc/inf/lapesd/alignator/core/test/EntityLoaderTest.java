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
import br.ufsc.inf.lapesd.alignator.core.entity.loader.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.alignator.core.entity.loader.SemanticResource;

public class EntityLoaderTest {

    private Alignator alignator;

    @Before
    public void configure() throws IOException {
        alignator = new Alignator();

        String ontology1 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/ontology1.owl"), "UTF-8");
        String serviceSrecription1String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/serviceDescription1.jsonld"), "UTF-8");
        SemanticMicroserviceDescription serviceDescription1 = new Gson().fromJson(serviceSrecription1String, SemanticMicroserviceDescription.class);
        serviceDescription1.setUriBase("service1");
        serviceDescription1.setServerPort("8081");
        serviceDescription1.setIpAddress("10.1.1.1");
        alignator.registerService(serviceDescription1, ontology1);

        String ontology2 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/ontology2.owl"), "UTF-8");
        String serviceSrecription2String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/serviceDescription2.jsonld"), "UTF-8");
        SemanticMicroserviceDescription serviceDescription2 = new Gson().fromJson(serviceSrecription2String, SemanticMicroserviceDescription.class);
        serviceDescription2.setUriBase("service2");
        serviceDescription2.setServerPort("8082");
        serviceDescription2.setIpAddress("10.1.1.1");
        alignator.registerService(serviceDescription2, ontology2);

        String ontology3 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/ontology3.owl"), "UTF-8");
        String serviceSrecription3String = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/serviceDescription3.jsonld"), "UTF-8");
        SemanticMicroserviceDescription serviceDescription3 = new Gson().fromJson(serviceSrecription3String, SemanticMicroserviceDescription.class);
        serviceDescription3.setUriBase("service3");
        serviceDescription3.setServerPort("8083");
        serviceDescription3.setIpAddress("10.1.1.1");
        alignator.registerService(serviceDescription3, ontology3);

    }

    @Test
    public void mustCreateLinksToloadEntities() throws IOException {
        TestableEntityLoader entityLoader = new TestableEntityLoader();
        alignator.setEntityLoader(entityLoader);
        String entity2 = IOUtils.toString(this.getClass().getResourceAsStream("/entityLoader/entity2.jsonld"), "UTF-8");
        alignator.loadEntitiesAndAlignOntologies(entity2);

        List<String> expectedLinks = new ArrayList<>();

        expectedLinks.add("http://10.1.1.1:8083/service3/service3?x=value1&y=value3");
        expectedLinks.add("http://10.1.1.1:8083/service3/service3?x=value3&y=value4");
        expectedLinks.add("http://10.1.1.1:8083/service3/service3?x=value4&y=value3");
        expectedLinks.add("http://10.1.1.1:8083/service3/service3?x=value1&y=value4");
        expectedLinks.add("http://10.1.1.1:8083/service3/service3?x=value3&y=value1");
        expectedLinks.add("http://10.1.1.1:8083/service3/service3?x=value4&y=value1");
        expectedLinks.add("http://10.1.1.1:8082/service2/service2?x=value1&y=value3");
        expectedLinks.add("http://10.1.1.1:8082/service2/service2?x=value3&y=value4");
        expectedLinks.add("http://10.1.1.1:8082/service2/service2?x=value4&y=value3");
        expectedLinks.add("http://10.1.1.1:8082/service2/service2?x=value1&y=value4");
        expectedLinks.add("http://10.1.1.1:8082/service2/service2?x=value3&y=value1");
        expectedLinks.add("http://10.1.1.1:8082/service2/service2?x=value4&y=value1");
        expectedLinks.add("http://10.1.1.1:8081/service1/service1?x=value1&y=value3");
        expectedLinks.add("http://10.1.1.1:8081/service1/service1?x=value3&y=value4");
        expectedLinks.add("http://10.1.1.1:8081/service1/service1?x=value4&y=value3");
        expectedLinks.add("http://10.1.1.1:8081/service1/service1?x=value1&y=value4");
        expectedLinks.add("http://10.1.1.1:8081/service1/service1?x=value3&y=value1");
        expectedLinks.add("http://10.1.1.1:8081/service1/service1?x=value4&y=value1");

        List<String> createdLinks = entityLoader.getCreatedLinks();

        for (String expectedLink : expectedLinks) {
            Assert.assertTrue(createdLinks.contains(expectedLink));
        }

        for (String createdLink : createdLinks) {
            System.out.println(createdLink);
            Assert.assertTrue(expectedLinks.contains(createdLink));
        }

    }

    class TestableEntityLoader extends EntityLoader {
        private List<String> createdLinks = new ArrayList<>();

        @Override
        public List<String> loadEntitiesFromServices(String exampleOfEntity, SemanticMicroserviceDescription semanticMicroserviceDescription) {

            List<String> extractedValues = new ArrayList<>(extractValues(exampleOfEntity));

            List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
            createdLinks.addAll(createLinks(extractedValues, semanticResources));

            return null;
        }

        public List<String> getCreatedLinks() {
            return createdLinks;
        }

    }

}
