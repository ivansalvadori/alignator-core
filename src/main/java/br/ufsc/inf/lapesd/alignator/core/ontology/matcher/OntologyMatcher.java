package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
public interface OntologyMatcher {
    List<Alignment> align(@Nonnull Model mergedModel, @Nonnull Collection<Model> ontologies) throws Exception;
}
