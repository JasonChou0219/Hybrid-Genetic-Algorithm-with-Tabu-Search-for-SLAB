import pandas as pd
from sklearn.decomposition import PCA
from sklearn.cluster import DBSCAN
import matplotlib.pyplot as plt

def plot_dbscan_clustering(file_path, eps=5, min_samples=5):
    data_df = pd.read_csv(file_path)

    # 检查数据读取是否正确
    print(data_df.head())  # 输出数据的前几行进行检查

    # 确保 OS 和 MS 都是字符串类型
    data_df['OS'] = data_df['OS'].apply(lambda x: [int(i) for i in str(x).split(',')])
    data_df['MS'] = data_df['MS'].apply(lambda x: [int(i) for i in str(x).split(',')])

    # 合并 OS 和 MS 作为特征
    features = data_df['OS'].tolist() + data_df['MS'].tolist()
    features = pd.DataFrame(features)  # 将列表转换为 DataFrame

    print(f"Features shape: {features.shape}")  # 输出特征的形状

    # 检查是否有多于一个特征
    n_components = min(2, features.shape[1])
    print(f"Using n_components={n_components} for PCA")

    # 进行 PCA 降维
    pca = PCA(n_components=n_components)
    reduced_features = pca.fit_transform(features)

    # 使用 DBSCAN 进行聚类
    dbscan = DBSCAN(eps=eps, min_samples=min_samples)
    clusters = dbscan.fit_predict(reduced_features)

    # 可视化聚类结果
    plt.figure(figsize=(10, 6))
    unique_clusters = set(clusters)

    if n_components == 2:
        for cluster in unique_clusters:
            row_idx = clusters == cluster
            plt.scatter(reduced_features[row_idx, 0], reduced_features[row_idx, 1], label=f'Cluster {cluster}')
        plt.xlabel('PCA Component 1')
        plt.ylabel('PCA Component 2')
    elif n_components == 1:
        for cluster in unique_clusters:
            row_idx = clusters == cluster
            plt.scatter(reduced_features[row_idx, 0], [0] * len(reduced_features[row_idx]), label=f'Cluster {cluster}')
        plt.xlabel('PCA Component 1')
        plt.ylabel('Arbitrary Constant')

    plt.title('DBSCAN Clustering of GA Population')
    plt.legend()
    plt.show()

# 提供路径到 CSV 文件
file_path = 'src/com/zihao/GA_TS_SLAB/Plot/dbscan_data.csv'
plot_dbscan_clustering(file_path, eps=5, min_samples=5)
