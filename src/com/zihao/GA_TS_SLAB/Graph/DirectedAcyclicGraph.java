package com.zihao.GA_TS_SLAB.Graph;

import java.util.*;

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

    public List<Integer> getParents(int node) {
        List<Integer> parents = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()) {
            int fromNode = entry.getKey();
            List<Integer> neighbors = entry.getValue();
            if (neighbors.contains(node)) {
                parents.add(fromNode);
            }
        }
        return parents;
    }

    public List<Integer> getPath(int start, int end) {
        Queue<List<Integer>> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(Collections.singletonList(start));

        while (!queue.isEmpty()) {
            List<Integer> path = queue.poll();
            int lastNode = path.get(path.size() - 1);

            if (lastNode == end) {
                return path;
            }

            if (!visited.contains(lastNode)) {
                visited.add(lastNode);
                for (Integer neighbor : getNeighbors(lastNode)) {
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }
        return Collections.emptyList(); // 如果没有找到路径，返回空列表
    }

    public void printGraph() {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph G {\n");
        for (Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()) {
            int from = entry.getKey();
            for (int to : entry.getValue()) {
                dot.append("    ").append(from).append(" -> ").append(to).append(";\n");
            }
        }
        dot.append("}");
        System.out.println(dot.toString());
    }
}