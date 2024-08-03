import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.lines as mlines


df = pd.read_csv('src/com/zihao/GA_TS_SLAB/Plot/schedule.csv')

precedence = {}
with open('src/com/zihao/GA_TS_SLAB/Plot/adjacency_list.csv', 'r') as file:
    next(file)  # 跳过表头
    for line in file:
        parts = line.strip().split(',')
        key = int(parts[0])
        values = list(map(int, parts[1:]))
        precedence[key] = values


fig, ax = plt.subplots(figsize=(10, 6))

for machine in df['Machine'].unique():
    machine_data = df[df['Machine'] == machine]
    ax.broken_barh(
        [(row['Start'], row['End'] - row['Start']) for _, row in machine_data.iterrows()],
        (machine - 0.4, 0.8),
        label=f'Machine {machine}'
    )
    for _, row in machine_data.iterrows():
        ax.text(
            row['Start'] + (row['End'] - row['Start']) / 2,
            machine,
            str(row['Operation']),
            va='center',
            ha='center',
            color='black',
            fontsize=8,
            fontweight='bold'
        )


for key, values in precedence.items():
    for value in values:
        start_op = df[df['Operation'] == key].iloc[0]
        end_op = df[df['Operation'] == value].iloc[0]
        line = mlines.Line2D([start_op['End'], end_op['Start']], [start_op['Machine'], end_op['Machine']],
                             color='gray', linestyle='--', linewidth=1)
        ax.add_line(line)


ax.set_xlabel('Time(min)')
ax.set_ylabel('Machine')
ax.set_yticks(df['Machine'].unique())
ax.set_yticklabels([f'Machine {int(machine)}' for machine in df['Machine'].unique()])

ax.xaxis.set_major_formatter(plt.FuncFormatter(lambda x, pos: f'{int(x)}'))

# set the location of tag
plt.legend(title='Job ID', bbox_to_anchor=(1.05, 1), loc='upper left')
plt.title('Gantt Chart of Schedule')

plt.tight_layout()
plt.show()
