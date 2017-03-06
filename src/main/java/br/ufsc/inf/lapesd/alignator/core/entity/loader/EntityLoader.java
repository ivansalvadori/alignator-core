package br.ufsc.inf.lapesd.alignator.core.entity.loader;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EntityLoader {

    public List<String> loadEntitiesFromServices(String exampleOfEntity, SemanticMicroserviceDescription semanticMicroserviceDescription) {

        List<String> extractedValues = new ArrayList<>(extractValues(exampleOfEntity));

        List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
        createLinks(extractedValues, semanticResources);

        return null;
    }

    protected List<String> createLinks(List<String> extractedValues, List<SemanticResource> semanticResources) {
        List<String> listOfLinksToVisit = new ArrayList<>();
        for (SemanticResource semanticResource : semanticResources) {
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

                    UriBuilder builder = UriBuilder.fromPath(semanticResource.getSemanticMicroserviceDescription().getMicroserviceFullPath()).path(uriTemplate.getUri());
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

        JsonElement parsedEntity = new JsonParser().parse(exampleOfEntity);
        if (parsedEntity.isJsonObject()) {

            JsonObject jsonObject = parsedEntity.getAsJsonObject();
            Set<Entry<String, JsonElement>> keys = jsonObject.entrySet();
            for (Entry<String, JsonElement> key : keys) {
                if (key.getKey().equals("@id") || key.getKey().equals("@type")) {
                    continue;
                }
                JsonElement value = key.getValue();
                if (value.isJsonPrimitive()) {
                    entityValues.add(value.getAsString());
                } else {
                    String innerObject = value.toString();
                    entityValues.addAll(extractValues(innerObject));
                }
            }

        }

        return entityValues;
    }

}