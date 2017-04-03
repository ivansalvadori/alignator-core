package br.ufsc.inf.lapesd.alignator.core.ontology.matcher;

import java.util.ArrayList;
import java.util.List;

public class M {

    public static void main(String[] args) {
        List<String> objects = new ArrayList<String>();
        objects.add("A");
        objects.add("B");
        objects.add("C");

        Combinations<String> combinations = new Combinations<String>(objects, 2);
        for (List<String> combination : combinations) {
            System.out.println(combination);
        }
    }
}
