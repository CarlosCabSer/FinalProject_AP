# Documentación Técnica - Procesamiento Concurrente vs Secuencial

## 📋 Índice
1. [Visión General](#visión-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Flujo de Ejecución](#flujo-de-ejecución)
4. [Manejo de Concurrencia](#manejo-de-concurrencia)
5. [Comparación: Concurrente vs Secuencial](#comparación-concurrente-vs-secuencial)
6. [Detalles de Implementación](#detalles-de-implementación)

---

## 🎯 Visión General

Este proyecto implementa un sistema de procesamiento de archivos CSV que permite comparar el rendimiento entre dos estrategias:

- **Procesamiento Concurrente**: Utiliza múltiples hilos para procesar el archivo en paralelo
- **Procesamiento Secuencial**: Procesa el archivo usando un solo hilo

Ambas versiones utilizan la misma lógica de división y procesamiento, garantizando una comparación justa.

---

## 🏗️ Arquitectura del Sistema

### Componentes Principales

```
FinalProject_AP-VersionSecuencial_Concurrente/
├── Main/
│   └── App.java                    # Punto de entrada del programa
├── Hilos/
│   ├── Manager.java                # Orquestador concurrente
│   ├── WorkerTask.java             # Tarea de procesamiento (Callable)
│   ├── CriterioFiltro.java         # Objeto con criterios de filtrado
│   └── ResumenResultados.java      # Objeto con estadísticas de resultado
├── secuencial/
│   └── ManagerSecuencial.java      # Orquestador secuencial
└── FileSplitter/
    └── FileSplitter.java           # Divisor de archivos
```

### Patrón de Diseño

El proyecto implementa el patrón **Manager-Worker**:

- **Manager**: Coordina la división del trabajo y la recolección de resultados
- **Worker**: Ejecuta tareas específicas de procesamiento sobre fragmentos del archivo

---

## 🔄 Flujo de Ejecución

### 1. Inicio del Programa (App.java)

```java
main() {
    1. Solicita ruta del archivo CSV de entrada
    2. Solicita configuración de filtros y columnas
    3. Pregunta al usuario qué modo usar:
       - [1] Concurrente
       - [2] Secuencial
    4. Ejecuta el Manager correspondiente
    5. Mide y muestra el tiempo total
}
```

---

## ⚡ Manejo de Concurrencia

### Modo Concurrente (Manager.java)

#### **Fase 1: Configuración Inicial**

```java
// Detecta el número de núcleos del procesador
int numProcesadores = Runtime.getRuntime().availableProcessors();

// Calcula número de fragmentos (2 veces los núcleos)
int numSubArchivos = numProcesadores * 2;

// Crea pool de hilos con N hilos (uno por núcleo)
ExecutorService executor = Executors.newFixedThreadPool(numProcesadores);
```

**Ejemplo**: Si tu CPU tiene 8 núcleos:
- Se crearán **16 fragmentos** del archivo
- Se usarán **8 hilos** para procesarlos
- Cada hilo procesará aproximadamente 2 fragmentos

---

#### **Fase 2: División del Archivo**

```
Archivo Original (1,000,000 líneas)
          |
          | FileSplitter.dividirArchivo()
          v
    +-----+-----+
    |     |     |
   16 Fragmentos
    |     |     |
    v     v     v
fragmento_0.csv (≈62,500 líneas)
fragmento_1.csv (≈62,500 líneas)
    ...
fragmento_15.csv (≈62,500 líneas)
```

**Algoritmo de División (Round Robin)**:
```java
while (hay líneas) {
    int indiceArchivo = contadorLineas % numSubArchivos;
    escribir línea en fragmento[indiceArchivo];
    contadorLineas++;
}
```

**⏱️ Esta fase NO se cuenta en el tiempo de procesamiento**

---

#### **Fase 3: Procesamiento Paralelo**

**⏱️ INICIA EL CRONÓMETRO AQUÍ**

```java
// Crear tareas para cada fragmento
for (File fragmento : fragmentos) {
    WorkerTask tarea = new WorkerTask(fragmento, ...);
    Future<ResumenResultados> future = executor.submit(tarea);
    listaFutures.add(future);
}
```

**Diagrama de Ejecución Paralela**:

```
Tiempo →

Hilo 1: [Fragmento 0] → [Fragmento 8]  →
Hilo 2: [Fragmento 1] → [Fragmento 9]  →
Hilo 3: [Fragmento 2] → [Fragmento 10] →
Hilo 4: [Fragmento 3] → [Fragmento 11] →
Hilo 5: [Fragmento 4] → [Fragmento 12] →
Hilo 6: [Fragmento 5] → [Fragmento 13] →
Hilo 7: [Fragmento 6] → [Fragmento 14] →
Hilo 8: [Fragmento 7] → [Fragmento 15] →
        ↓
   Todos terminan
```

**¿Qué hace cada WorkerTask?**

1. Lee su fragmento línea por línea
2. Aplica el criterio de filtrado (ej: columna[2] == "304")
3. Selecciona solo las columnas deseadas
4. Escribe resultado en archivo parcial
5. Retorna estadísticas (líneas procesadas, aceptadas, errores)

---

#### **Fase 4: Recolección de Resultados**

```java
for (Future<ResumenResultados> future : listaFutures) {
    ResumenResultados resultado = future.get(); // BLOQUEA hasta que termine

    totalProcesados += resultado.totalProcesados;
    totalAceptados += resultado.totalAceptados;
    totalErrores += resultado.totalErrores;
    archivosParciales.add(resultado.archivoResultado);
}
```

**Sincronización con `Future.get()`**:
- El hilo principal **espera** a que cada hilo worker termine
- `get()` es una operación **bloqueante**
- Garantiza que todos los resultados estén listos antes de continuar

---

#### **Fase 5: Unificación (Merge)**

```
resultado_hilo_0.csv  ─┐
resultado_hilo_1.csv  ─┤
resultado_hilo_2.csv  ─┤
      ...              ├─→ unificarResultados() ─→ resultados_japon.csv
resultado_hilo_13.csv ─┤
resultado_hilo_14.csv ─┤
resultado_hilo_15.csv ─┘
```

**Proceso de Merge**:
```java
for (File parcial : archivosParciales) {
    if (es primer archivo) {
        escribir header;
        esPrimerArchivo = false;
    }
    // Copiar todas las líneas de datos (sin header)
    escribir líneas del parcial;
}
```

**⏱️ SE DETIENE EL CRONÓMETRO AQUÍ**

---

#### **Fase 6: Limpieza**

```java
finally {
    executor.shutdown();           // Cierra el pool de hilos
    limpiarTemporales();           // Elimina carpeta temp_processing_*
}
```

---

### Modo Secuencial (ManagerSecuencial.java)

El modo secuencial sigue **exactamente los mismos pasos** que el concurrente, con **una diferencia clave**:

#### **Diferencia en la Fase 3: Procesamiento Secuencial**

```java
// En lugar de submit() a un ExecutorService
for (File fragmento : fragmentos) {
    WorkerTask tarea = new WorkerTask(fragmento, ...);

    // Llamada DIRECTA en el hilo principal (SIN paralelismo)
    ResumenResultados resultado = tarea.call();

    listaResultados.add(resultado);
}
```

**Diagrama de Ejecución Secuencial**:

```
Tiempo →

Hilo Principal: [Frag 0][Frag 1][Frag 2]...[Frag 15]
                   ↓      ↓      ↓          ↓
              Uno después del otro
```

---

## 📊 Comparación: Concurrente vs Secuencial

| Aspecto | Concurrente | Secuencial |
|---------|-------------|------------|
| **División** | ✅ Sí (N*2 fragmentos) | ✅ Sí (N*2 fragmentos) |
| **Hilos** | N hilos (uno por núcleo) | 1 hilo (principal) |
| **Procesamiento** | Paralelo (varios fragmentos simultáneamente) | Secuencial (un fragmento a la vez) |
| **Merge** | ✅ Sí (unifica N*2 archivos) | ✅ Sí (unifica N*2 archivos) |
| **Tiempo medido** | Solo procesamiento + merge | Solo procesamiento + merge |
| **Speed-up esperado** | ~N veces más rápido (ideal) | Línea base |

### ¿Por qué dividir en 2N fragmentos con N hilos?

```
Con 8 núcleos:

Opción A: 8 fragmentos, 8 hilos
  - Si un fragmento tarda más, ese hilo queda ocioso al final
  - Desbalanceo de carga

Opción B: 16 fragmentos, 8 hilos ✅
  - Cuando un hilo termina su fragmento, toma otro
  - Mejor balanceo de carga
  - Aprovecha mejor los núcleos
```

---

## 🔧 Detalles de Implementación

### Clase WorkerTask (Callable)

```java
public class WorkerTask implements Callable<ResumenResultados> {

    @Override
    public ResumenResultados call() throws Exception {
        // 1. Abrir archivo de entrada (fragmento)
        BufferedReader lector = new BufferedReader(new FileReader(fragmento));

        // 2. Crear archivo de salida
        BufferedWriter escritor = new BufferedWriter(new FileWriter(archivoSalida));

        // 3. Procesar línea por línea
        while ((linea = lector.readLine()) != null) {
            String[] columnas = linea.split(",");

            if (cumpleCriterio(columnas, criterio)) {
                String lineaFiltrada = construirLineaSalida(columnas, columnasDeseadas);
                escritor.write(lineaFiltrada);
                lineasAceptadas++;
            }
            lineasProcesadas++;
        }

        // 4. Retornar estadísticas
        return new ResumenResultados(archivoSalida, lineasProcesadas,
                                     lineasAceptadas, lineasConError);
    }
}
```

---

### Criterio de Filtrado

```java
public class CriterioFiltro {
    int indiceColumna;      // Qué columna verificar
    String valorEsperado;   // Valor que debe tener
}

// Ejemplo: Filtrar países con código "304" (Japón)
CriterioFiltro criterio = new CriterioFiltro(2, "304");
```

---

### Selección de Columnas

```java
// Entrada: 0,1,2,3,4,5,6,7,8,9
int[] columnasDeseadas = {0, 2, 7}; // Queremos solo columnas 0, 2 y 7

// Salida: 0,2,7
String construirLineaSalida(String[] columnas, int[] indices) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indices.length; i++) {
        sb.append(columnas[indices[i]]);
        if (i < indices.length - 1) sb.append(",");
    }
    return sb.toString();
}
```

---

## 🎯 Ventajas de esta Arquitectura

### ✅ Reutilización de Código
- Ambas versiones usan el mismo `WorkerTask`
- Misma lógica de filtrado y procesamiento
- Comparación justa garantizada

### ✅ Escalabilidad
- Se adapta automáticamente al número de núcleos
- Fácil ajustar el factor de división (2N, 4N, etc.)

### ✅ Separación de Responsabilidades
- `Manager`: Coordina el trabajo
- `WorkerTask`: Ejecuta el trabajo
- `FileSplitter`: Divide el archivo

### ✅ Manejo de Errores
- Cada worker captura sus propios errores
- No afecta a otros workers
- Estadísticas completas de errores

---

## 📈 Ejemplo de Ejecución

### Escenario: CPU de 4 núcleos, archivo de 1,000,000 líneas

**Modo Concurrente**:
```
1. División: 8 fragmentos de ~125,000 líneas cada uno
2. Procesamiento: 4 hilos procesan 2 fragmentos cada uno en paralelo
3. Tiempo: ~5 segundos
4. Speed-up: 4x más rápido que secuencial
```

**Modo Secuencial**:
```
1. División: 8 fragmentos de ~125,000 líneas cada uno
2. Procesamiento: 1 hilo procesa los 8 fragmentos uno por uno
3. Tiempo: ~20 segundos
4. Speed-up: 1x (línea base)
```

---

## 🔍 Conceptos Clave de Concurrencia

### ExecutorService
```java
ExecutorService executor = Executors.newFixedThreadPool(N);
```
- Crea un **pool de N hilos** reutilizables
- Gestiona automáticamente la asignación de tareas
- Evita crear/destruir hilos constantemente (costoso)

### Future<T>
```java
Future<ResumenResultados> future = executor.submit(tarea);
ResumenResultados resultado = future.get(); // BLOQUEA
```
- Representa el **resultado futuro** de una tarea
- `get()` espera a que termine la tarea
- Permite sincronización entre hilos

### Callable<T>
```java
public class WorkerTask implements Callable<ResumenResultados> {
    @Override
    public ResumenResultados call() throws Exception { ... }
}
```
- Similar a `Runnable`, pero puede **retornar un valor**
- Puede lanzar excepciones checked
- Ideal para tareas que producen resultados

---

## 🚀 Mejoras Potenciales

### 1. Usar Streams Paralelos (Java 8+)
```java
List<ResumenResultados> resultados = fragmentos.parallelStream()
    .map(fragmento -> new WorkerTask(fragmento, ...).call())
    .collect(Collectors.toList());
```

### 2. Procesamiento Pipeline
- Leer fragmentos → Cola 1
- Procesar líneas → Cola 2
- Escribir resultados → Cola 3

### 3. Memory-Mapped Files
- Mapear el archivo en memoria para acceso más rápido
- Útil para archivos muy grandes

---

## 📝 Conclusión

Este proyecto demuestra claramente:

1. **Cómo implementar concurrencia** usando el patrón Manager-Worker
2. **Cuándo la concurrencia es beneficiosa** (archivos grandes, CPUs multi-core)
3. **Cómo medir correctamente** el rendimiento (excluyendo overhead)
4. **Cómo garantizar una comparación justa** (misma división, misma lógica)

La diferencia de rendimiento entre las dos versiones ilustra el poder del procesamiento paralelo en aplicaciones de I/O y procesamiento de datos.
