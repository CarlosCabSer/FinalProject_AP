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
        long tiempoInicio = System.currentTimeMillis();

        // 1. Determinar número de hilos y subarchivos
        int numProcesadores = Runtime.getRuntime().availableProcessors();
        // El PDF sugiere dividir en más partes que procesadores para mejor balanceo (ej. 2*N o 4*N)
        int numSubArchivos = numProcesadores * 2;

        System.out.println("--- Inicio del Procesamiento ---");
        System.out.println("Núcleos detectados: " + numProcesadores);
        System.out.println("Dividiendo archivo en " + numSubArchivos + " fragmentos...");

        ExecutorService executor = Executors.newFixedThreadPool(numProcesadores);
        List<Future<ResumenResultados>> listaFutures = new ArrayList<>();
        FileSplitter splitter = new FileSplitter();

        try {
            // 2. Fase de División (Splitting)
            List<File> fragmentos = splitter.dividirArchivo(archivoOrigen, carpetaTemporal, numSubArchivos);

            // 3. Fase de Asignación (Mapping)
            int idWorker = 0;
            for (File fragmento : fragmentos) {
                WorkerTask tarea = new WorkerTask(fragmento, carpetaTemporal, idWorker++, criterio, columnasDeseadas);
                // Enviamos la tarea al pool y guardamos el "recibo" (Future)
                Future<ResumenResultados> future = executor.submit(tarea);
                listaFutures.add(future);
            }

            // 4. Fase de Recolección de Estadísticas (Reduce parcial)
            long totalProcesados = 0;
            long totalAceptados = 0;
            long totalErrores = 0;
            List<File> archivosParciales = new ArrayList<>();

            System.out.println("...");

            for (Future<ResumenResultados> f : listaFutures) {
                try {
                    // .get() bloquea hasta que el hilo termine su tarea específica
                    ResumenResultados resultado = f.get();

                    totalProcesados += resultado.totalProcesados;
                    totalAceptados += resultado.totalAceptados;
                    totalErrores += resultado.totalErrores;
                    archivosParciales.add(resultado.archivoResultado);

                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error en un hilo: " + e.getMessage());
                }
            }

            // 5. Fase de Unificación (Merge final)
            System.out.println("Unificando resultados en " + archivoFinal.getName() + "...");
            unificarResultados(archivosParciales, archivoFinal);

            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            // Reporte final en consola
            System.out.println("--- Procesamiento Finalizado ---");
            System.out.println("Tiempo total: " + (tiempoTotal / 1000.0) + " segundos.");
            System.out.println("Registros procesados: " + totalProcesados);
            System.out.println("Registros filtrados (guardados): " + totalAceptados);
            System.out.println("Errores encontrados: " + totalErrores);

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