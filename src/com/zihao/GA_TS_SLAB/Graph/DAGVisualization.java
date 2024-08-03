package com.zihao.GA_TS_SLAB.Graph;

import com.zihao.GA_TS_SLAB.Data.Input;
import com.zihao.GA_TS_SLAB.Data.ProblemSetting;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class DAGVisualization extends Application {

    private static ProblemSetting problemSetting;

    public static void main(String[] args) {
        File parentDir = new File("src/Dataset/Gu2016/N1");
        Input input = new Input(parentDir);
        input.getProblemDesFromFile();
        problemSetting = input.getProblemSetting();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        DirectedAcyclicGraph dag = problemSetting.getDag();
        Pane pane = new Pane();
        Map<Integer, Double[]> nodePositions = calculateLayeredNodePositions(dag, 800, 600);

        for (Map.Entry<Integer, Double[]> entry : nodePositions.entrySet()) {
            Integer vertex = entry.getKey();
            Double[] position = entry.getValue();
            Text text = new Text(position[0], position[1], vertex.toString());
            Circle circle = new Circle(position[0], position[1], 15, Color.LIGHTBLUE);
            pane.getChildren().addAll(circle, text);
        }

        for (Map.Entry<Integer, List<Integer>> entry : dag.getAdjacencyList().entrySet()) {
            Integer source = entry.getKey();
            for (Integer target : entry.getValue()) {
                Double[] sourcePos = nodePositions.get(source);
                Double[] targetPos = nodePositions.get(target);

                if (sourcePos != null && targetPos != null) {
                    // Adjust line to end at the edge of the target circle
                    double radius = 15;
                    double angle = Math.atan2(targetPos[1] - sourcePos[1], targetPos[0] - sourcePos[0]);
                    double targetX = targetPos[0] - radius * Math.cos(angle);
                    double targetY = targetPos[1] - radius * Math.sin(angle);

                    Line line = new Line(sourcePos[0], sourcePos[1], targetX, targetY);
                    line.setStroke(Color.BLACK);

                    // Calculate arrow head
                    double arrowLength = 10;
                    double arrowWidth = 5;
                    double sin = Math.sin(angle);
                    double cos = Math.cos(angle);

                    double x1 = targetX - arrowLength * cos + arrowWidth * sin;
                    double y1 = targetY - arrowLength * sin - arrowWidth * cos;
                    double x2 = targetX - arrowLength * cos - arrowWidth * sin;
                    double y2 = targetY - arrowLength * sin + arrowWidth * cos;

                    Polygon arrowHead = new Polygon();
                    arrowHead.getPoints().addAll(targetX, targetY, x1, y1, x2, y2);
                    arrowHead.setFill(Color.BLACK);

                    pane.getChildren().addAll(line, arrowHead);
                }
            }
        }

        Scene scene = new Scene(pane, 800, 600);
        primaryStage.setTitle("Dependency Graph");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Map<Integer, Double[]> calculateLayeredNodePositions(DirectedAcyclicGraph dag, double width, double height) {
        Map<Integer, Double[]> positions = new HashMap<>();
        Map<Integer, Integer> levels = new HashMap<>();
        int maxLevel = assignLevels(dag, levels);

        double layerWidth = width / (maxLevel + 1);
        Map<Integer, List<Integer>> nodesAtLevel = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : levels.entrySet()) {
            nodesAtLevel.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        for (Map.Entry<Integer, List<Integer>> entry : nodesAtLevel.entrySet()) {
            int level = entry.getKey();
            List<Integer> nodes = entry.getValue();
            double layerHeight = height / (nodes.size() + 1);
            for (int i = 0; i < nodes.size(); i++) {
                double x = level * layerWidth + layerWidth / 2;
                double y = (i + 1) * layerHeight;
                positions.put(nodes.get(i), new Double[]{x, y});
            }
        }

        return positions;
    }

    public static int assignLevels(DirectedAcyclicGraph dag, Map<Integer, Integer> levels) {
        Map<Integer, Integer> inDegree = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();

        for (Integer node : dag.getAdjacencyList().keySet()) {
            inDegree.put(node, 0);
            levels.put(node, 0); // Ensure all nodes are in the levels map
        }

        for (Map.Entry<Integer, List<Integer>> entry : dag.getAdjacencyList().entrySet()) {
            for (Integer neighbor : entry.getValue()) {
                inDegree.put(neighbor, inDegree.getOrDefault(neighbor, 0) + 1);
            }
        }

        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
                levels.put(entry.getKey(), 0);
            }
        }

        int maxLevel = 0;
        while (!queue.isEmpty()) {
            int node = queue.poll();
            int level = levels.get(node);
            for (Integer neighbor : dag.getNeighbors(node)) {
                levels.put(neighbor, Math.max(levels.getOrDefault(neighbor, 0), level + 1));
                maxLevel = Math.max(maxLevel, levels.get(neighbor));
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return maxLevel;
    }
}
