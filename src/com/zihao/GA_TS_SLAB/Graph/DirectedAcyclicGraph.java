package com.zihao.GA_TS_SLAB.Graph;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class DirectedAcyclicGraph {

    private final Map<Integer, List<Integer>> adjacencyList;

    public DirectedAcyclicGraph() {
        adjacencyList = new HashMap<>();
    }

    public DirectedAcyclicGraph(Map<Integer, List<Integer>> dag){
        adjacencyList = dag;
    }
    public void addEdge(int from, int to) {
        adjacencyList.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public List<Integer> getNeighbors(int node) {
        return adjacencyList.getOrDefault(node, new ArrayList<>());
    }

    public Map<Integer, List<Integer>> getAdjacencyList(){
        return adjacencyList;
    }

//    public void printGraph() {
//        StringBuilder dot = new StringBuilder();
//        dot.append("digraph G {\n");
//        for (Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()) {
//            int from = entry.getKey();
//            for (int to : entry.getValue()) {
//                dot.append("    ").append(from).append(" -> ").append(to).append(";\n");
//            }
//        }
//        dot.append("}");
//        System.out.println(dot.toString());
//    }
}