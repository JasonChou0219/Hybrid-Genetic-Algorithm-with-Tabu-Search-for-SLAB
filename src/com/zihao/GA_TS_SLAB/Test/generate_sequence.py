import numpy as np

def generate_sequences(order_matrix):
    num_operations = order_matrix.shape[0]
    visited = [False] * num_operations
    in_degree = [0] * num_operations
    sequences = []
    current_sequence = []

    # 计算每个节点的入度
    for i in range(num_operations):
        for j in range(num_operations):
            if order_matrix[i][j] == 1:
                in_degree[j] += 1

    # 使用DFS生成所有拓扑排序
    def dfs():
        is_end = True
        for i in range(num_operations):
            if in_degree[i] == 0 and not visited[i]:
                if len(current_sequence) == 0 and i == 3:  # 跳过以3开头的序列
                    continue
                for j in range(num_operations):
                    if order_matrix[i][j] == 1:
                        in_degree[j] -= 1
                current_sequence.append(i)
                visited[i] = True
                dfs()
                visited[i] = False
                current_sequence.pop()
                for j in range(num_operations):
                    if order_matrix[i][j] == 1:
                        in_degree[j] += 1
                is_end = False
        if is_end:
            sequences.append(current_sequence.copy())

    dfs()
    return sequences

# Order Matrix
order_matrix = np.array([
    [0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1],
    [0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1],
    [0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1],
    [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
])

# 生成符合依赖关系的所有序列
sequences = generate_sequences(order_matrix)

# 打印生成的序列数量
print("Number of valid sequences:", len(sequences))