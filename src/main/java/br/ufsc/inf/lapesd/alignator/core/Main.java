package br.ufsc.inf.lapesd.alignator.core;

import java.io.File;
import java.net.URI;
import java.util.Enumeration;
import java.util.Properties;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.aroma.AROMA;

public class Main {

    public static void main(String[] args) {

        String[] x = { "/home/ivan/Documents/temp/executable/executable/8d99456f-9289-4ac9-acb7-f0db04d6d8e6_startingWithWorkersIDonly_0_ontology-payment.owl",
                "/home/ivan/Documents/temp/executable/executable/8d99456f-9289-4ac9-acb7-f0db04d6d8e6_startingWithWorkersIDonly_0_ontology-worker.owl" };

        try {
            Properties p = new Properties();
            p.setProperty("lexicalSim", "true");

            AROMA align = new AROMA();
            align.skos = "true".equals(p.getProperty("skos"));
            URI ontoURI1 = (new File(x[0])).toURI();
            URI ontoURI2 = (new File(x[1])).toURI();
            align.init(ontoURI1, ontoURI2);
            align.align(new ObjectAlignment(), p);

            Enumeration<Cell> elements = align.getElements();
            while (elements.hasMoreElements()) {
                Cell nextElement = elements.nextElement();
                System.out.println(nextElement.getStrength());
                System.out.println(nextElement.getObject1AsURI());
                System.out.println(nextElement.getObject2AsURI());
                System.out.println(nextElement.getRelation().getRelation());
            }
        }

        catch (AlignmentException e) {
            e.printStackTrace();
        }
    }
}
