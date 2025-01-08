# from graphviz import Digraph
#
# # Create a new directed graph
# dot = Digraph(comment='Dependency Graph')
#
# # Set node shape to ellipse
# dot.attr('node', shape='ellipse')
#
# # Add the edges as specified in the graph
# edges = [
#     ('1', '2'), ('2', '7'),('2', '12'), ('7', '9'), ('9', '15'), ('15', '17'),
#     ('3', '4'), ('4', '10'), ('4', '8'), ('8', '9'), ('10', '12'),('12', '13'),('13', '14'),('14', '16'),('16', '17'),
#     ('5', '6'),('6', '11'), ('11', '12'), ('12', '17')
# ]
#
# # Add edges to the graph
# for start, end in edges:
#     dot.edge(start, end)
#
# # Render the graph to an SVG file
# output_filename = 'dependency_graph'
# dot.render(output_filename, format='svg', cleanup=True)
#
# # Output file is saved as 'dependency_graph.svg'
# print(f"Graph has been saved as {output_filename}.svg")
