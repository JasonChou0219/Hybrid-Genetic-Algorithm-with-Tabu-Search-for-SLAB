# # import csv
# # import networkx as nx
# # import matplotlib.pyplot as plt
# # import matplotlib.cm as cm
# #
# # # Debugging function to print step-by-step data
# # def debug_print(message, data):
# #     print(f"DEBUG: {message}: {data}")
# #
# # # Read data from CSV
# # precedence_edges = []
# # machine_paths = {}
# # tcmb_constraints = []
# #
# # with open('src/com/zihao/GA_TS_SLAB/Plot/conjunctive_plot_data.csv', 'r') as file:
# #     reader = csv.reader(file)
# #     section = None
# #     for row in reader:
# #         if len(row) == 0:
# #             continue
# #         if row[0].startswith("#"):
# #             section = row[0][2:]
# #             continue
# #         if section == "Precedence Relationships":
# #             precedence_edges.append((int(row[0]), int(row[1])))
# #         elif section == "Machine Paths":
# #             machine = int(row[0])
# #             operations = list(map(int, row[1:]))
# #             machine_paths[machine] = operations
# #         elif section == "TCMB Constraints":
# #             tcmb_constraints.append((int(row[0]), int(row[1]), int(row[2])))
# #
# # # Debugging output
# # debug_print("Precedence Edges", precedence_edges)
# # debug_print("Machine Paths", machine_paths)
# # debug_print("TCMB Constraints", tcmb_constraints)
# #
# # # Create a directed graph
# # G = nx.DiGraph()
# #
# # # Add precedence relationships
# # for edge in precedence_edges:
# #     G.add_edge(*edge, color='black')
# #
# # # Add machine paths
# # num_machines = len(machine_paths)
# # colors = cm.get_cmap('hsv', num_machines)
# # for idx, (machine, operations) in enumerate(machine_paths.items()):
# #     color = colors(idx)
# #     for i in range(len(operations) - 1):
# #         G.add_edge(operations[i], operations[i + 1], color=color)
# #
# # # Add TCMB constraints
# # for op1, op2, time_constraint in tcmb_constraints:
# #     G.add_edge(op1, op2, color='red', style='dashed')
# #
# # # Draw the graph
# # edge_colors = [G[u][v]['color'] for u, v in G.edges()]
# #
# # # Debugging output for graph edges and colors
# # debug_print("Graph Edges", list(G.edges(data=True)))
# # debug_print("Edge Colors", edge_colors)
# #
# # pos = nx.spring_layout(G, seed=42, k=0.5, iterations=50)
# # plt.figure(figsize=(12, 8))
# # nx.draw(G, pos, with_labels=True, edge_color=edge_colors, node_size=500, font_size=10, font_color='white', arrows=True)
# # plt.title("Conjunctive Graph Visualization")
# # plt.show()
#
# import csv
# import networkx as nx
# import matplotlib.pyplot as plt
# import matplotlib.cm as cm
# from networkx.drawing.nx_agraph import graphviz_layout
#
# # Debugging function to print step-by-step data
# def debug_print(message, data):
#     print(f"DEBUG: {message}: {data}")
#
# # Read data from CSV
# precedence_edges = []
# machine_paths = {}
# tcmb_constraints = []
#
# with open('src/com/zihao/GA_TS_SLAB/Plot/conjunctive_plot_data.csv', 'r') as file:
#     reader = csv.reader(file)
#     section = None
#     for row in reader:
#         if len(row) == 0:
#             continue
#         if row[0].startswith("#"):
#             section = row[0][2:]
#             continue
#         if section == "Precedence Relationships":
#             precedence_edges.append((int(row[0]), int(row[1])))
#         elif section == "Machine Paths":
#             machine = int(row[0])
#             operations = list(map(int, row[1:]))
#             machine_paths[machine] = operations
#         elif section == "TCMB Constraints":
#             tcmb_constraints.append((int(row[0]), int(row[1]), int(row[2])))
#
# # Debugging output
# debug_print("Precedence Edges", precedence_edges)
# debug_print("Machine Paths", machine_paths)
# debug_print("TCMB Constraints", tcmb_constraints)
#
# # Create a directed graph
# G = nx.DiGraph()
#
# # Add precedence relationships
# for edge in precedence_edges:
#     G.add_edge(*edge, color='black')
#
# # Add machine paths
# num_machines = len(machine_paths)
# # colors = cm.get_cmap('hsv', num_machines)
# colors = plt.get_cmap('hsv', num_machines)
#
# for idx, (machine, operations) in enumerate(machine_paths.items()):
#     color = colors(idx)
#     for i in range(len(operations) - 1):
#         G.add_edge(operations[i], operations[i + 1], color=color)
#
# # Add TCMB constraints
# for op1, op2, time_constraint in tcmb_constraints:
#     G.add_edge(op1, op2, color='red', style='dashed')
#
# # Use Graphviz's dot layout to arrange nodes from left to right based on precedence
# pos = graphviz_layout(G, prog='dot')
#
# # Get edge colors
# edge_colors = [G[u][v]['color'] for u, v in G.edges()]
#
# # Draw the graph
# plt.figure(figsize=(14, 10))
# nx.draw(G, pos, with_labels=True, edge_color=edge_colors, node_size=600, font_size=10, font_color='white', arrows=True)
#
# plt.title("Graph Visualization with Precedence Relationship (Left to Right)")
# plt.show()
#
#

import csv
import networkx as nx
import matplotlib.pyplot as plt
import matplotlib.cm as cm
from networkx.drawing.nx_agraph import graphviz_layout

# Debugging function to print step-by-step data
def debug_print(message, data):
    print(f"DEBUG: {message}: {data}")

# Read data from CSV
precedence_edges = []
machine_paths = {}
tcmb_constraints = []

with open('src/com/zihao/GA_TS_SLAB/Plot/conjunctive_plot_data.csv', 'r') as file:
    reader = csv.reader(file)
    section = None
    for row in reader:
        if len(row) == 0:
            continue
        if row[0].startswith("#"):
            section = row[0][2:]
            continue
        if section == "Precedence Relationships":
            precedence_edges.append((int(row[0]), int(row[1])))
        elif section == "Machine Paths":
            machine = int(row[0])
            operations = list(map(int, row[1:]))
            machine_paths[machine] = operations
        elif section == "TCMB Constraints":
            tcmb_constraints.append((int(row[0]), int(row[1]), int(row[2])))

# Debugging output
debug_print("Precedence Edges", precedence_edges)
debug_print("Machine Paths", machine_paths)
debug_print("TCMB Constraints", tcmb_constraints)

# Create a directed graph
G = nx.DiGraph()

# Add precedence relationships
for edge in precedence_edges:
    G.add_edge(*edge, color='black', style='solid')

# Add machine paths
num_machines = len(machine_paths)
colors = plt.get_cmap('hsv', num_machines)

for idx, (machine, operations) in enumerate(machine_paths.items()):
    color = colors(idx)
    for i in range(len(operations) - 1):
        G.add_edge(operations[i], operations[i + 1], color=color, style='solid')

# Add TCMB constraints (dashed edges)
for op1, op2, time_constraint in tcmb_constraints:
    G.add_edge(op1, op2, color='red', style='dashed')

# Use Graphviz's dot layout to arrange nodes from left to right based on precedence
pos = graphviz_layout(G, prog='dot')

# Separate edges by style (dashed and solid)
dashed_edges = [(u, v) for u, v in G.edges() if G[u][v]['style'] == 'dashed']
solid_edges = [(u, v) for u, v in G.edges() if G[u][v]['style'] == 'solid']

# Draw solid edges
solid_edge_colors = [G[u][v]['color'] for u, v in solid_edges]
nx.draw_networkx_edges(G, pos, edgelist=solid_edges, edge_color=solid_edge_colors, style='solid')

# Draw dashed edges
nx.draw_networkx_edges(G, pos, edgelist=dashed_edges, edge_color='red', style='dashed')

# Draw nodes and labels
nx.draw_networkx_nodes(G, pos, node_size=600)
nx.draw_networkx_labels(G, pos, font_size=10, font_color='white')

# Set title and display plot
plt.title("Graph Visualization with Precedence Relationships and TCMB Constraints")
plt.show()

