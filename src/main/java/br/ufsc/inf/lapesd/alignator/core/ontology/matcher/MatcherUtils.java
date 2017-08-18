package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class MatcherUtils {

    static List<File> serialize(@Nonnull Collection<? extends Model> models, @Nonnull File parent,
                                @Nonnull Lang lang) {
        List<File> list = new ArrayList<>();
        int counter = 1;
        String suff = "." + lang.getFileExtensions().get(0);
        for (Model model: models) {
            File file = new File(parent, String.valueOf(++counter) + suff);
            try (FileOutputStream out = new FileOutputStream(file)) {
                RDFDataMgr.write(out, model, lang);
                list.add(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    static void incorporateAlignments(@Nonnull Model mergedModel,
                                      @Nonnull List<Alignment> allAlignments,
                                      @Nonnull List<Alignment> alignments) {
        allAlignments.addAll(alignments);
        for (Alignment alignment : alignments) {
            // System.out.println(alignment);
            Resource left = mergedModel.createResource(alignment.getUri1());
            Resource right = mergedModel.createResource(alignment.getUri2());
            if (left.hasProperty(RDF.type, OWL2.Class)) {
                left.addProperty(OWL2.equivalentClass, right);
                right.addProperty(OWL2.equivalentClass, left);
            } else {
                left.addProperty(OWL2.equivalentProperty, right);
                right.addProperty(OWL2.equivalentProperty, left);
            }
        }
    }
}
