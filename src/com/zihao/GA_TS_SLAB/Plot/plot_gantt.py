import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os
import sys

# 检查是否提供了算法名称和数据集名称参数
if len(sys.argv) >= 3:
    algorithm_name = sys.argv[1]
    dataset_name = sys.argv[2]
else:
    # 默认值，以防没有提供参数
    algorithm_name = "Unknown"
    dataset_name = "Unknown"

# Read the schedule data
df = pd.read_csv('src/com/zihao/GA_TS_SLAB/Plot/schedule.csv')

# Set up figure with increased height ratio
fig, ax = plt.subplots(figsize=(14, 6))

# Get unique jobs for color mapping
unique_jobs = sorted(df['Job'].unique())

# Define a color palette with medium saturation and good contrast
medium_saturation_colors = [
    '#E57373',  # Soft red
    '#FFB74D',  # Soft orange
    '#FFF176',  # Soft yellow
    '#81D4FA',  # Soft blue
    '#BA68C8',  # Soft purple
    '#AED581',  # Soft green
    '#4FC3F7',  # Light blue
    '#FFD54F',  # Amber
    '#9575CD',  # Light purple
    '#F06292',  # Light pink
    '#7986CB',  # Indigo
    '#A1887F',  # Brown
    '#90A4AE',  # Blue grey
    '#DCE775',  # Lime
    '#4DB6AC',  # Teal
    '#FF8A65',  # Deep orange
]

# Ensure we have enough colors for all jobs
while len(medium_saturation_colors) < len(unique_jobs):
    medium_saturation_colors.extend(medium_saturation_colors)

# Create color mapping
job_colors = {job: medium_saturation_colors[i % len(medium_saturation_colors)] for i, job in enumerate(unique_jobs)}

# Create dictionary to map machine ID to machine name
machine_names = {}
for _, row in df.iterrows():
    machine_id = row['Machine']
    machine_name = row['MachineName']
    machine_names[machine_id] = machine_name

# Plot operations for each machine
for machine in df['Machine'].unique():
    machine_data = df[df['Machine'] == machine]

    # Plot each operation on this machine with color based on job
    for _, row in machine_data.iterrows():
        job_id = row['Job']
        color = job_colors[job_id]

        # Draw the operation bar with reduced height
        ax.broken_barh(
            [(row['Start'], row['End'] - row['Start'])],
            (machine - 0.25, 0.5),
            facecolors=color,
            edgecolors='black',
            linewidth=1
        )

# Set up axes and labels
ax.set_xlabel('Time (min)')
ax.set_ylabel('Machine')

# Get machine IDs in sorted order
sorted_machine_ids = sorted(df['Machine'].unique())

# Set y-ticks
ax.set_yticks(sorted_machine_ids)

# Create labels with machine names
machine_labels = []
for machine_id in sorted_machine_ids:
    if machine_id in machine_names:
        machine_labels.append(f"{machine_names[machine_id]}")
    else:
        machine_labels.append(f"Machine {int(machine_id)}")

# Set the y-tick labels
ax.set_yticklabels(machine_labels)

# Format x-axis to show integer time values
ax.xaxis.set_major_formatter(plt.FuncFormatter(lambda x, pos: f'{int(x)}'))

# Create legend for job colors
job_patches = [plt.Rectangle((0, 0), 1, 1, color=job_colors[job]) for job in unique_jobs]
ax.legend(job_patches, [f'Job {job}' for job in unique_jobs],
          title='Job ID',
          bbox_to_anchor=(1.05, 1),
          loc='upper left')

# Calculate makespan
makespan = df['End'].max()

# 使用从Java传入的算法名称
plt.suptitle(f'{algorithm_name}', fontsize=14)
plt.title(f'Makespan: {int(makespan)} min', fontsize=12)

# Add grid for better readability
plt.grid(axis='x', linestyle='--', alpha=0.3)
plt.tight_layout(rect=[0, 0, 1, 0.95])  # Adjust layout for two-line title

# 获取桌面路径
desktop_path = os.path.join(os.path.expanduser("~"), "Desktop")

# 创建文件名：算法名称-数据集名称.svg
filename = f"{algorithm_name.lower().replace(' ', '_')}-{dataset_name}.svg"
filepath = os.path.join(desktop_path, filename)

# 保存SVG文件到桌面
plt.savefig(filepath, format='svg', bbox_inches='tight')
print(f"Gantt chart saved as {filepath}")

# Still show the plot
plt.show()