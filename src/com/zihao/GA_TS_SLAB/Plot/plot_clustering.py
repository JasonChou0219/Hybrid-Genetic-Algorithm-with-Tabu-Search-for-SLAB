import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA

# 读取Java导出的特征向量
data = pd.read_csv('population_features.csv', header=None)

# 使用PCA将特征降维到2D，以便于可视化
pca = PCA(n_components=2)
reduced_data = pca.fit_transform(data)

# 使用K-Means进行聚类
num_clusters = 4
kmeans = KMeans(n_clusters=num_clusters)
kmeans.fit(reduced_data)
labels = kmeans.labels_

# 绘制聚类结果
plt.figure(figsize=(10, 7))
for i in range(num_clusters):
    cluster_data = reduced_data[labels == i]
    plt.scatter(cluster_data[:, 0], cluster_data[:, 1], label=f'Cluster {i}')

# 绘制聚类中心
centroids = kmeans.cluster_centers_
plt.scatter(centroids[:, 0], centroids[:, 1], s=300, c='red', marker='X', label='Centroids')

plt.title('K-Means Clustering of GA Population')
plt.xlabel('PCA Component 1')
plt.ylabel('PCA Component 2')
plt.legend()
plt.show()
