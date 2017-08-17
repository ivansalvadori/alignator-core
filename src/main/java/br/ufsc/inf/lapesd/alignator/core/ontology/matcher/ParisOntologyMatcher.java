package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import br.ufsc.inf.lapesd.alignator.core.Alignment;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.springframework.stereotype.Component;
import paris.Paris;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static br.ufsc.inf.lapesd.alignator.core.ontology.matcher.MatcherUtils.incorporateAlignments;

@Component
public class ParisOntologyMatcher implements OntologyMatcher  {
    public static final double DEFAULT_THRESHOLD = 0.8;
    private final double threshold;

    public ParisOntologyMatcher(double threshold) {
        this.threshold = threshold;
    }

    public ParisOntologyMatcher() {
        this(DEFAULT_THRESHOLD);
    }

    @Override
    public List<Alignment> align(@Nonnull Model mergedModel,
                                 @Nonnull Collection<Model> ontologies) throws Exception {
        List<Alignment> allAlignments = new ArrayList<>();

        File parisDir = Files.createTempDirectory("paris").toFile();
        try {
            List<File> ontologyFiles = MatcherUtils.serialize(ontologies, parisDir, Lang.NT);
            for (List<File> c : new Combinations<>(ontologyFiles, 2)) {
                List<Alignment> alignments = align(parisDir, c.get(0), c.get(1));
                incorporateAlignments(mergedModel, allAlignments, alignments);
            }
        } finally {
            FileUtils.deleteDirectory(parisDir);
        }

        return allAlignments;
    }

    @Nonnull
    private List<Alignment> align(File parisDir, File left, File right) throws Exception {
        File store1 = new File(parisDir, "store1");
        File store2 = new File(parisDir, "store2");
        File results = new File(parisDir, "results");
        File home = new File(parisDir, "home");
        File settingsFile = new File(parisDir, "settings");

        try {
            if (store1.exists()) FileUtils.deleteDirectory(store1);
            if (store2.exists()) FileUtils.deleteDirectory(store2);
            if (results.exists()) FileUtils.deleteDirectory(results);
            if (home.exists()) FileUtils.deleteDirectory(home);
            if (!store1.mkdirs()) throw new IOException("Could not mkdir " + store1);
            if (!store2.mkdirs()) throw new IOException("Could not mkdir " + store2);
            if (!results.mkdirs()) throw new IOException("Could not mkdir " + results);
            if (!home.mkdirs()) throw new IOException("Could not mkdir " + home);

            String settings = String.format("resultTSV = %1$s/results\n" +
                    "factstore1 = %1$s/store1\n" +
                    "factstore2 = %1$s/store2\n" +
                    "home = %1$s/home\n", parisDir);
            try (FileOutputStream out = new FileOutputStream(settingsFile)) {
                IOUtils.write(settings, out, StandardCharsets.UTF_8);
            }

            Files.move(left.toPath(), new File(store1, left.getName()).toPath());
            Files.move(right.toPath(), new File(store2, right.getName()).toPath());
            try {
                Paris.main(new String[]{settingsFile.getPath()});
                List<Alignment> alignments = getResults(results, "superrelations1");
                alignments.addAll(getResults(results, "superclasses1"));
                return alignments;
            } finally {
                Files.move(new File(store1, left.getName()).toPath(), left.toPath());
                Files.move(new File(store2, right.getName()).toPath(), right.toPath());
            }
        } finally {
            if (store1.exists())       FileUtils.deleteDirectory(store1);
            if (store2.exists())       FileUtils.deleteDirectory(store2);
            if (results.exists())       FileUtils.deleteDirectory(results);
            if (home.exists())         FileUtils.deleteDirectory(home);
            if (settingsFile.exists()) FileUtils.deleteQuietly(settingsFile);
        }
    }

    private List<Alignment> getResults(File results, String tsvName) throws IOException {
        Pattern pattern = Pattern.compile("(\\d+)_" + tsvName + ".tsv");
        File[] files = results.listFiles(n -> pattern.matcher(n.getName()).matches());
        if (files == null) return Collections.emptyList();

        try {
            HashMap<ImmutablePair<String, String>, Alignment> helper = new HashMap<>();
            Arrays.stream(files).sorted(Comparator.comparing(f -> {
                Matcher matcher = pattern.matcher(f.getName());
                return matcher.matches() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
            })).flatMap(this::parseAlignments).forEach(a -> {
                ImmutablePair<String, String> k = ImmutablePair.of(a.getUri1(), a.getUri2());
                Alignment old = helper.get(k);
                if (old == null || old.getStrength() < a.getStrength())
                    helper.put(k, a);
            });
            return new ArrayList<>(helper.values());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            }
            throw e;
        }
    }

    private Stream<Alignment> parseAlignments(File file) {
        List<Alignment> list = new ArrayList<>();
        try (CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.TDF)) {
            for (CSVRecord record : parser) {
                Alignment a = new Alignment();
                if (record.get(0).endsWith("-") || record.get(1).endsWith("-")) {
                    a.setUri2(record.get(0).replaceAll("^(.*)-$", "$1"));
                    a.setUri1(record.get(1).replaceAll("^(.*)-$", "$1"));
                    a.setRelation(">=");
                } else {
                    a.setUri1(record.get(0));
                    a.setUri2(record.get(1));
                    a.setRelation(">=");
                }
                a.setStrength(Double.parseDouble(record.get(2)));
                Alignment old = list.stream().filter(o -> o.getUri1().equals(a.getUri2())
                        && o.getUri2().equals(a.getUri1())).findFirst().orElse(null);
                if (old != null) {
                    old.setStrength((old.getStrength() + a.getStrength()) / 2);
                    old.setRelation("==");
                } else {
                    list.add(a);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list.stream().filter(a -> a.getStrength() >= threshold);
    }
}
