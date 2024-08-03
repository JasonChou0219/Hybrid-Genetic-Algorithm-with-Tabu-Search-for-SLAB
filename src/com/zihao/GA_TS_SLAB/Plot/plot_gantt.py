import pandas as pd
import matplotlib.pyplot as plt

# 读取CSV文件
df = pd.read_csv('schedule.csv')

# 绘制甘特图
fig, ax = plt.subplots(figsize=(10, 6))

for machine in df['Machine'].unique():
    machine_data = df[df['Machine'] == machine]
    ax.broken_barh(
        [(row['Start'], row['End'] - row['Start']) for _, row in machine_data.iterrows()],
        (machine - 0.4, 0.8),
        label=f'Machine {machine}'
    )

# 设置图表属性
ax.set_xlabel('Time')
ax.set_ylabel('Machine')
ax.set_yticks(df['Machine'].unique())
ax.set_yticklabels([f'Machine {int(machine)}' for machine in df['Machine'].unique()])

# 设置时间轴格式为整数
ax.xaxis.set_major_formatter(plt.FuncFormatter(lambda x, pos: f'{int(x)}'))

# 调整图例位置
plt.legend(title='Job ID', bbox_to_anchor=(1.05, 1), loc='upper left')
plt.title('Gantt Chart of Schedule')

plt.tight_layout()
plt.show()
