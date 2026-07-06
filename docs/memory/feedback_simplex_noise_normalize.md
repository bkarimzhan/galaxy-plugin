---
name: Bukkit SimplexOctaveGenerator returns [-1,1] not [0,1]
description: Bukkit's SimplexOctaveGenerator.noise(...,normalized=true) returns [-1,1]. Treating it as [0,1] systematically biases terrain low. Use (n+1)*0.5 to normalize to [0,1] before height-mapping.
type: feedback
originSessionId: f4c2d9c3-4371-4882-abc2-8e6861d6d624
---
`org.bukkit.util.noise.SimplexOctaveGenerator.noise(x, z, freq, amp, normalized=true)` возвращает значения в **[-1, 1]**, не [0, 1].

Если код предполагает что output ∈ [0, 1] и делает `BASE_Y + (n - 0.5) * 2 * RANGE`, terrain систематически просядет ниже BASE_Y (среднее значение n ≈ 0, а не 0.5, так что (0 - 0.5) * 2 * RANGE = -RANGE). Поверхность окажется в нижней половине окна.

**Why:** Случилось в `MarsTerrainGenerator` — сначала high baseY=80 давал поверхность Y=50–58, что почти на дне worldborder. Заметили после force-load + сэмплинга высот. После добавления `(n + 1.0) * 0.5` — поверхность встала в окно [BASE_Y - HEIGHT_RANGE, BASE_Y + HEIGHT_RANGE] как и ожидалось.

**How to apply:** Для любого нового террейн-генератора через `SimplexOctaveGenerator`, перед использованием noise в формуле высоты — нормализуй:
```java
double n = (gen.noise(x, z, freq, amp, true) + 1.0) * 0.5;  // [0, 1]
```
Ridge-noise после `1 - Math.abs(...)` уже даёт [0, 1] корректно (т.к. abs(noise) ∈ [0,1]).

Существующий `TerrainPlanetGenerator.heightAt()` имеет тот же баг (строка `n * (1.0 - p.ridgeStrength()) + r * p.ridgeStrength()` где n ∈ [-1,1]) — но компенсирует через высокий BASE_Y. Если стоит задача чтобы высоты других планет (mercury/venus/uranus/neptune) попадали ровно в заявленный диапазон, нужно так же нормализовать там.
