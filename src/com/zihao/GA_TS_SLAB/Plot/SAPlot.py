import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

# Define the function p(x, y)
def p(x, y):
    # Avoid division by zero
    return -np.exp(np.divide(x, y, where=(y != 0)))

# Define the range for x and y
x = np.linspace(-2, 2, 100)
y = np.linspace(-2, 2, 100)
x, y = np.meshgrid(x, y)

# Compute z values
z = p(x, y)

# Plot the 3D surface
fig = plt.figure(figsize=(10, 7))
ax = fig.add_subplot(111, projection='3d')

# Mask out invalid values
z = np.nan_to_num(z, nan=0.0, posinf=0.0, neginf=0.0)

surf = ax.plot_surface(x, y, z, cmap='viridis', edgecolor='none')

# Add labels and title
ax.set_xlabel('X')
ax.set_ylabel('Y')
ax.set_zlabel('p(x, y)')
ax.set_title('3D Surface Plot of p(x, y) = -e^(x/y)')

# Add color bar
fig.colorbar(surf, shrink=0.5, aspect=10)

plt.show()
