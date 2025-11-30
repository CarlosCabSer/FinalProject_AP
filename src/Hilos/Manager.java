package Hilos;

import FileSplitter.FileSplitter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Clase Manager (Orquestador).
 * Responsable de dividir el trabajo, coordinar el pool de hilos y consolidar resultados.
 */
public class Manager {

    private final File archivoOrigen;
    private final File carpetaTemporal;
    private final File archivoFinal;

    // Configuración del usuario
    private final CriterioFiltro criterio;
    private final int[] columnasDeseadas;

    public Manager(File archivoOrigen, String rutaSalida, CriterioFiltro criterio, int[] columnasDeseadas) {
        this.archivoOrigen = archivoOrigen;
        this.archivoFinal = new File(rutaSalida);
        // Carpeta temporal oculta o dedicada
        this.carpetaTemporal = new File("temp_processing_" + System.currentTimeMillis());
        this.criterio = criterio;
        this.columnasDeseadas = columnasDeseadas;
    }

    /**
     * Método principal que ejecuta todo el flujo de trabajo.
     */
    public void procesar() {
        // 1. Determinar número de hilos y subarchivos
        int numProcesadores = Runtime.getRuntime().availableProcessors();

        // Configuración: Cada hilo procesa EXACTAMENTE un fragmento
        // Número de fragmentos = Número de hilos
        int numSubArchivos = numProcesadores * 25;  // Cantidad de fragmentos
        int numHilos = numSubArchivos;              // Igualamos: 1 hilo por fragmento

        System.out.println("--- Inicio del Procesamiento CONCURRENTE ---");
        System.out.println("Núcleos detectados: " + numProcesadores);
        System.out.println("Hilos a crear: " + numHilos);
        System.out.println("Dividiendo archivo en " + numSubArchivos + " fragmentos...");

        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        List<Future<ResumenResultados>> listaFutures = new ArrayList<>();
        FileSplitter splitter = new FileSplitter();

        try {
            // 2. Fase de División (Splitting)
            long tiempoSplitInicio = System.currentTimeMillis();
            List<File> fragmentos = splitter.dividirArchivo(archivoOrigen, carpetaTemporal, numSubArchivos);
            long tiempoSplit = System.currentTimeMillis() - tiempoSplitInicio;
            System.out.println("Tiempo de división: " + (tiempoSplit / 1000.0) + " segundos.");

            // INICIAR CRONÓMETRO AQUÍ - Solo para procesamiento paralelo
            long tiempoInicio = System.currentTimeMillis();

            // 3. Fase de Asignación (Mapping) - Enviar TODAS las tareas de una vez
            System.out.println("Procesando en paralelo...");
            int idWorker = 0;
            for (File fragmento : fragmentos) {
                WorkerTask tarea = new WorkerTask(fragmento, carpetaTemporal, idWorker++, criterio, columnasDeseadas);
                // Enviamos la tarea al pool y guardamos el "recibo" (Future)
                Future<ResumenResultados> future = executor.submit(tarea);
                listaFutures.add(future);
            }

            // 4. Fase de Recolección de Estadísticas (Reduce parcial)
            // IMPORTANTE: Aquí todas las tareas YA están ejecutándose en paralelo
            // Solo esperamos a que TODAS terminen
            long totalProcesados = 0;
            long totalAceptados = 0;
            long totalErrores = 0;
            List<File> archivosParciales = new ArrayList<>();

            for (Future<ResumenResultados> f : listaFutures) {
                try {
                    // .get() bloquea hasta que ESTA tarea específica termine
                    // Pero las demás siguen ejecutándose en paralelo
                    ResumenResultados resultado = f.get();

                    totalProcesados += resultado.totalProcesados;
                    totalAceptados += resultado.totalAceptados;
                    totalErrores += resultado.totalErrores;
                    archivosParciales.add(resultado.archivoResultado);

                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error en un hilo: " + e.getMessage());
                }
            }

            // DETENER CRONÓMETRO - Después de recolectar resultados
            long tiempoProcesamiento = System.currentTimeMillis() - tiempoInicio;

            // 5. Fase de Unificación (Merge final)
            System.out.println("Unificando resultados en " + archivoFinal.getName() + "...");
            long tiempoMergeInicio = System.currentTimeMillis();
            unificarResultados(archivosParciales, archivoFinal);
            long tiempoMerge = System.currentTimeMillis() - tiempoMergeInicio;

            // Calcular tiempo total
            long tiempoTotal = tiempoSplit + tiempoProcesamiento + tiempoMerge;

            // Reporte final en consola con métricas detalladas
            System.out.println("\n=================================================");
            System.out.println("  REPORTE DETALLADO - MODO CONCURRENTE");
            System.out.println("=================================================");
            System.out.println("Tiempo de División (Split):    " + (tiempoSplit / 1000.0) + " s");
            System.out.println("Tiempo de Procesamiento:       " + (tiempoProcesamiento / 1000.0) + " s");
            System.out.println("Tiempo de Unificación (Merge): " + (tiempoMerge / 1000.0) + " s");
            System.out.println("-------------------------------------------------");
            System.out.println("TIEMPO TOTAL:                  " + (tiempoTotal / 1000.0) + " s");
            System.out.println("=================================================");
            System.out.println("Registros procesados:          " + totalProcesados);
            System.out.println("Registros filtrados (guardados): " + totalAceptados);
            System.out.println("Errores encontrados:           " + totalErrores);
            System.out.println("=================================================");

        } catch (IOException e) {
            System.err.println("Error crítico de E/S: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 6. Limpieza de recursos y carpeta temporal
            executor.shutdown();
            limpiarTemporales();
        }
    }

    /**
     * Combina los archivos parciales en uno solo.
     * Maneja la lógica para no repetir cabeceras si los parciales las tienen.
     */
    private void unificarResultados(List<File> parciales, File destino) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(destino))) {
            boolean esPrimerArchivo = true;

            for (File parcial : parciales) {
                try (BufferedReader lector = new BufferedReader(new FileReader(parcial))) {
                    String linea = lector.readLine();

                    // Manejo de cabecera
                    if (linea != null) {
                        if (esPrimerArchivo) {
                            // Escribimos la cabecera del primer archivo
                            escritor.write(linea);
                            escritor.newLine();
                            esPrimerArchivo = false;
                        }
                        // Nota: Si los parciales tienen cabecera, la leímos arriba.
                        // Si NO es el primer archivo, esa línea se descarta (no se escribe)
                        // para no repetir headers en medio del archivo final.
                    }

                    // Copiar el resto del contenido
                    while ((linea = lector.readLine()) != null) {
                        escritor.write(linea);
                        escritor.newLine();
                    }
                }
            }
        }
    }

    /**
     * Elimina la carpeta temporal y todo su contenido de forma recursiva.
     */
    private void limpiarTemporales() {
        if (carpetaTemporal.exists()) {
            File[] archivos = carpetaTemporal.listFiles();
            if (archivos != null) {
                for (File f : archivos) {
                    f.delete();
                }
            }
            carpetaTemporal.delete();
        }
    }
}