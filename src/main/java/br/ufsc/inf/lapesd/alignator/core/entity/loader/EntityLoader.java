package br.ufsc.inf.lapesd.alignator.core.entity.loader;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.apache.commons.io.IOUtils.toInputStream;

@Component
public class EntityLoader {
    private static final Logger logger = LoggerFactory.getLogger(EntityLoader.class);

    public List<String> loadEntitiesFromServices(String exampleOfEntity, ServiceDescription semanticMicroserviceDescription) {
        List<String> extractedValues = new ArrayList<>(extractValues(exampleOfEntity));
        List<String> linksToVisit = createLinks(extractedValues, semanticMicroserviceDescription);
        List<String> loadedEntities = visitLinksAndGetEntities(linksToVisit);
        return loadedEntities;
    }

    protected List<String> createLinks(List<String> extractedValues, ServiceDescription semanticMicroserviceDescription) {
        List<String> listOfLinksToVisit = new ArrayList<>();

        for (SemanticResource semanticResource : semanticMicroserviceDescription.getSemanticResources()) {
            List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
            for (UriTemplate uriTemplate : uriTemplates) {

                Map<String, String> parameters = uriTemplate.getParameters();
                List<String> propertyKey = new ArrayList<>(parameters.keySet());

                if (parameters.size() > extractedValues.size()) {
                    continue;
                }

                Permutations<String> permutations = new Permutations<>();
                List<List<String>> permutedValues = new ArrayList<>(permutations.permute(extractedValues, parameters.keySet().size()));

                for (List<String> aPermutation : permutedValues) {
                    Map<String, Object> resolvedParameters = new HashMap<>();
                    for (int i = 0; i < propertyKey.size(); i++) {
                        resolvedParameters.put(propertyKey.get(i), aPermutation.get(i));
                    }

                    UriBuilder builder = UriBuilder.fromPath(semanticMicroserviceDescription.getMicroserviceFullPath()).path(uriTemplate.getUri());
                    builder.resolveTemplates(resolvedParameters);
                    URI uri = builder.build();
                    String link = uri.toASCIIString();
                    listOfLinksToVisit.add(link);
                }

            }
        }
        return listOfLinksToVisit;
    }


    protected Set<String> extractValues(String exampleOfEntity) {
        Set<String> entityValues = new HashSet<>();

        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, toInputStream(exampleOfEntity, "UTF-8"), Lang.JSONLD);
        } catch (IOException|RiotException e ) {
            logger.error("Problem parsing JSONLD", e);
            return Collections.emptySet();
        }

        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement s = it.next();
            if (!s.getObject().isLiteral()) continue;
            String lexicalForm = s.getLiteral().getLexicalForm();
            if (!lexicalForm.isEmpty()) entityValues.add(lexicalForm);
        }
        return entityValues;
    }

    protected List<String> visitLinksAndGetEntities(List<String> linksToVisit) {
        List<String> loadedEntities = new ArrayList<>();
        Client client = ClientBuilder.newClient();
        for (String link : linksToVisit) {
            WebTarget webTarget = client.target(link).queryParam("linkedatorOptions", "linkVerify");
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON, "application/ld+json");
            try {
                Response response = invocationBuilder.get();

                int status = response.getStatus();
                if (status == 200) {
                    String loadedEntity = response.readEntity(String.class);
                    loadedEntities.add(loadedEntity);
                }
            } catch (Exception e) {
            }
        }
        return loadedEntities;
    }

}
