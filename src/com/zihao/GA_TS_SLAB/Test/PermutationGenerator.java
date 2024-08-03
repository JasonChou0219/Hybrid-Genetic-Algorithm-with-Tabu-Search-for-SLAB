package com.zihao.GA_TS_SLAB.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PermutationGenerator {

    public List<Integer> generateRandomPermutation(int n, Random random) {
        List<Integer> permutation = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            permutation.add(i);
        }
        Collections.shuffle(permutation, random);
        return permutation;
    }

    public static void main(String[] args) {
        Random random = new Random();
        PermutationGenerator generator = new PermutationGenerator();
        List<Integer> permutation = generator.generateRandomPermutation(17, random);
        System.out.println(permutation);
    }
}
