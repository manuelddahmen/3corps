# 3corps - Three-Body Problem Simulator

A lightweight, interactive 3D physics simulator for the Three-Body (and N-body) gravitational problem. Built on Java and Kotlin using the **empty3** 3D engine library, this application provides real-time visualization of gravitational interactions, dynamic step configuration, and live JSON data editing.

---

## 🚀 Features

- **Interactive 3D Visualizer**: Real-time rendering of planetary movement and gravitational trajectories.
- **Newtonian Gravity Solver ([Force2.java](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Force2.java))**: Simulates exact gravitational forces using:
  $$F = G \frac{m_1 m_2}{r^2}$$
- **GUI Control Panel ([Problem.java](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Problem.java))**:
  - Start, pause, or step through the simulation.
  - Dynamically modify current time ($t$) and time step ($dt$).
- **Live Configuration Editor**:
  - Add, edit, remove, and delete celestial bodies (*Billes*).
  - Modify parameters via a Graphical Editor Dialog.
  - Import/Export simulated systems as formatted JSON files.

---

## 📂 Project Structure

```text
3corps/
├── .gradle/
├── .idea/
├── build/
├── gradle/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── one/empty3/corps3/
│   │   │       ├── Force2.java      <-- Gravity physics calculation & state updates
│   │   │       └── Problem.java     <-- Swing application, simulation loop & UI
│   │   └── kotlin/
│   └── Main.kt                      <-- Default project entrypoint
├── build.gradle.kts                 <-- Gradle build script with dependencies
├── FindSphere.java                  <-- Helper class to test empty3 dependencies
├── gradlew & gradlew.bat            <-- Gradle wrapper scripts
├── README.MD                        <-- This documentation file
└── settings.gradle.kts              <-- Project Gradle settings
```

---

## ⚙️ Prerequisites & Setup

- **Java Development Kit (JDK)**: Version 25 (configured in [build.gradle.kts](file:///d:/Current/3corps/build.gradle.kts))
- **Gradle**: Wrapper included in the project directory.

### Build and Run instructions

To compile the project and build the project artifacts, run:
```bash
./gradlew build
```

To run the simulation main entry point:
```bash
./gradlew run
```
*(Or execute the `main` method in [Problem.java](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Problem.java) directly via your IDE.)*

---

## 🪐 Physics Formulation

The physics simulation computes gravitational forces in a three-dimensional Cartesian space.

### Force Calculation
In [Force2.java](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Force2.java#L21-L25), gravitational force is calculated pairwise:
```java
@Override
public Point3D attractionRepulsion(Bille other, Bille p) {
    Point3D moins = other.position.moins(p.position);
    return moins.mult((6.674184E-11 * other.masse * p.masse / Math.pow(moins.norme(), 3)));
}
```
Where:
- $6.674184 \times 10^{-11} \, \text{m}^3 \text{kg}^{-1} \text{s}^{-2}$ is the gravitational constant ($G$).
- $\vec{r}$ (`moins`) is the displacement vector from body $p$ to body $other$.
- $r$ (`moins.norme()`) is the distance between the two bodies.

### Integration Step
In [Force2.java](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Force2.java#L28-L43), position and velocity are integrated over time step $dt$ using Euler integration:
$$\vec{v}_{t+dt} = \vec{v}_t + \frac{\vec{F}}{m} \cdot dt$$
$$\vec{x}_{t+dt} = \vec{x}_t + \vec{v}_{t+dt} \cdot dt$$

---

## 📝 Configuration File Schema

You can save and load simulation configurations via the **Editing of Bille objects** tab. The format uses standard JSON. Below is an example structure:

```json
[
  {
    "mass": 1.0,
    "position": [
      378.107,
      501.992,
      -120.485
    ],
    "vitesse": [
      4.123,
      -9.987,
      0.012
    ],
    "color": [
      255,
      120,
      0
    ]
  }
]
```

### Fields:
- `mass` (double): The mass of the body in kilograms ($kg$).
- `position` (array of 3 doubles): Coordinate vector $[X, Y, Z]$.
- `vitesse` (array of 3 doubles): Velocity vector $[v_x, v_y, v_z]$ in $m/s$.
- `color` (array of 3 integers): Color in RGB format $[Red, Green, Blue]$ (0-255).

---

## 🛠️ Main Components

- **[Problem](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Problem.java)**: Sets up the Swing GUI, initializes the 3D Scene camera, sets up the Z-Buffer renderer, holds the active state list of Billes, and implements the main simulation loop thread.
- **[Force2](file:///d:/Current/3corps/src/main/java/one/empty3/corps3/Force2.java)**: Inherits from the empty3 library's `Force` class to compute gravity calculations and update planetary position and speed vectors.
- **[FindSphere](file:///d:/Current/3corps/FindSphere.java)**: A utility class verify dependencies.
- **[Main.kt](file:///d:/Current/3corps/src/Main.kt)**: Default entrypoint template.
