package br.ufsc.inf.lapesd.alignator.core.entity.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Permutations<T> {

    public Set<List<String>> permute(Collection<String> input, int n) {
        Permutations<String> obj = new Permutations<>();
        Collection<List<String>> output = obj.permuteNxN(input);
        Set<List<String>> pnr = null;

        pnr = new HashSet<List<String>>();
        for (List<String> integers : output) {
            pnr.add(integers.subList(integers.size() - n, integers.size()));
        }
        return pnr;
    }

    private Collection<List<T>> permuteNxN(Collection<T> input) {
        Collection<List<T>> output = new ArrayList<List<T>>();
        if (input.isEmpty()) {
            output.add(new ArrayList<T>());
            return output;
        }
        List<T> list = new ArrayList<T>(input);
        T head = list.get(0);
        List<T> rest = list.subList(1, list.size());
        for (List<T> permutations : permuteNxN(rest)) {
            List<List<T>> subLists = new ArrayList<List<T>>();
            for (int i = 0; i <= permutations.size(); i++) {
                List<T> subList = new ArrayList<T>();
                subList.addAll(permutations);
                subList.add(i, head);
                subLists.add(subList);
            }
            output.addAll(subLists);
        }
        return output;
    }
}