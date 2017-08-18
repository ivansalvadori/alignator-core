package br.ufsc.inf.lapesd.alignator.core;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static Model load(@Nonnull String... paths) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        return load(model, paths);
    }

    public static Model load(@Nonnull Model model, @Nonnull String... paths) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        for (String path : paths) {
            Matcher matcher = Pattern.compile(".*\\.(.*)$").matcher(path);
            if (!matcher.matches())
                throw new IllegalArgumentException();
            Lang lang = RDFLanguages.fileExtToLang(matcher.group(1));
            if (lang == null)
                throw new IllegalArgumentException();

            try (InputStream in = cl.getResourceAsStream(path)) {
                RDFDataMgr.read(model, in, lang);
            } catch (RuntimeException e) {
                throw new RuntimeException(String.format("%s (file: %s)", e.getMessage(), path), e);
            } catch (IOException e) {
                throw new IOException(String.format("%s (file: %s)", e.getMessage(), path), e);
            }
        }
        return model;
    }
}
