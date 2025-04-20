import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os
import numpy as np

# Set the style
sns.set_style("whitegrid")
plt.figure(figsize=(12, 7))

# Define file paths based on the actual structure on your machine
base_dir = "/Users/jasonchou/IdeaProjects/Hybrid GA and TS for SLAB/src"
input_file = os.path.join(base_dir, "Solution/Solutions.tsv")
output_dir = os.path.join(base_dir, "Output")  # Changed output directory to src/Output

# For better debugging
print(f"Reading from: {input_file}")
print(f"Output will be saved to directory: {output_dir}")

# Check if input file exists
if not os.path.exists(input_file):
    print(f"ERROR: Input file not found at {input_file}")
    exit(1)

# Read the data
data = pd.read_csv(input_file, sep='\t')

# Create a copy of the original data for the normalized values
normalized_data = data.copy()

# Normalize the values using Branch and Bound as the optimal solution
normalized_data['GA-SA'] = data['GA-SA'] / data['Branch_and_Bound']
normalized_data['SAGAS'] = data['SAGAS'] / data['Branch_and_Bound']
normalized_data['Branch_and_Bound'] = data['Branch_and_Bound'] / data['Branch_and_Bound']  # Always 1.0

# Create the line plot for normalized data
plt.plot(normalized_data['Dataset'], normalized_data['Branch_and_Bound'], 'o-', color='#1f77b4', linewidth=2, label='Branch and Bound (Optimal)')
plt.plot(normalized_data['Dataset'], normalized_data['GA-SA'], 's-', color='#ff7f0e', linewidth=2, label='GA-SA')
plt.plot(normalized_data['Dataset'], normalized_data['SAGAS'], '^-', color='#2ca02c', linewidth=2, label='SAGAS')

# Add horizontal line at y=1.0 to emphasize optimal solution
plt.axhline(y=1.0, color='gray', linestyle='--', alpha=0.7)

# Set labels and title
plt.xlabel('Dataset', fontsize=14)
plt.ylabel('Normalized Makespan\n(ratio to optimal solution)', fontsize=14)
plt.title('Algorithm Performance Relative to Optimal Solution (Branch and Bound)', fontsize=16)

# Add legend with better positioning
plt.legend(loc='upper left', fontsize=12)

# Adjust tick parameters
plt.xticks(rotation=45, ha='right', fontsize=12)
plt.yticks(fontsize=12)

# Set y-axis to start from 1.0 and add some padding
plt.ylim(0.98, max(normalized_data['SAGAS'].max(), normalized_data['GA-SA'].max()) * 1.05)

# Add data labels with percentage over optimal
for i, dataset in enumerate(normalized_data['Dataset']):
    # Branch and Bound is always 1.0
    plt.text(i, normalized_data.loc[i, 'Branch_and_Bound'] - 0.02, "Optimal", ha='center', va='top', fontsize=10, color='#1f77b4')

    # GA-SA
    ga_sa_value = normalized_data.loc[i, 'GA-SA']
    percent_increase = (ga_sa_value - 1.0) * 100
    plt.text(i, ga_sa_value + 0.02, f"+{percent_increase:.1f}%", ha='center', va='bottom', fontsize=10, color='#ff7f0e')

    # SAGAS
    sagas_value = normalized_data.loc[i, 'SAGAS']
    percent_increase = (sagas_value - 1.0) * 100
    plt.text(i, sagas_value + 0.02, f"+{percent_increase:.1f}%", ha='center', va='bottom', fontsize=10, color='#2ca02c')

# Add a grid for better readability
plt.grid(True, linestyle='--', alpha=0.7)

# Tight layout
plt.tight_layout()

# Create output directory if it doesn't exist
os.makedirs(output_dir, exist_ok=True)

# Save the figure in PDF format
normalized_output_file = os.path.join(output_dir, "normalized_performance.pdf")
plt.savefig(normalized_output_file, format='pdf', bbox_inches='tight')
print(f"Normalized plot saved to {normalized_output_file}")

# Also create a second plot showing the actual makespan values for reference
plt.figure(figsize=(12, 7))

# Plot original data
plt.plot(data['Dataset'], data['Branch_and_Bound'], 'o-', color='#1f77b4', linewidth=2, label='Branch and Bound (Optimal)')
plt.plot(data['Dataset'], data['GA-SA'], 's-', color='#ff7f0e', linewidth=2, label='GA-SA')
plt.plot(data['Dataset'], data['SAGAS'], '^-', color='#2ca02c', linewidth=2, label='SAGAS')

# Set labels and title
plt.xlabel('Dataset', fontsize=14)
plt.ylabel('Makespan (min)', fontsize=14)
plt.title('Absolute Makespan Values Across Datasets', fontsize=16)

# Add legend
plt.legend(loc='best', fontsize=12)

# Adjust tick parameters
plt.xticks(rotation=45, ha='right', fontsize=12)
plt.yticks(fontsize=12)

# Add data labels
for i, dataset in enumerate(data['Dataset']):
    for algorithm in ['Branch_and_Bound', 'GA-SA', 'SAGAS']:
        plt.text(i, data.loc[i, algorithm] + 20, str(data.loc[i, algorithm]),
                 ha='center', va='bottom', fontsize=10)

# Add a grid for better readability
plt.grid(True, linestyle='--', alpha=0.7)

# Tight layout
plt.tight_layout()

# Save the absolute values figure in PDF format
abs_output_file = os.path.join(output_dir, "absolute_makespan.pdf")
plt.savefig(abs_output_file, format='pdf', bbox_inches='tight')
print(f"Absolute plot saved to {abs_output_file}")

# Show the plots
plt.show()