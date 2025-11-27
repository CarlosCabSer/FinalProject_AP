package secuencial;

import Hilos.CriterioFiltro;
import Hilos.ResumenResultados;
import Hilos.WorkerTask;
import FileSplitter.FileSplitter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Versión Secuencial que divide el archivo igual que la concurrente,
 * pero procesa los fragmentos uno por uno (sin hilos).
 * Realiza: Split -> Proceso Secuencial -> Merge
 */
public class ManagerSecuencial {

    private final File archivoOrigen;
    private final File archivoFinal;
    private final File carpetaTemporal;
    private final CriterioFiltro criterio;
    private final int[] columnasDeseadas;

    public ManagerSecuencial(File archivoOrigen, String rutaSalida, CriterioFiltro criterio, int[] columnasDeseadas) {
        this.archivoOrigen = archivoOrigen;
        this.archivoFinal = new File(rutaSalida);
        // Carpeta temporal distinta para no mezclar con la concurrente
        this.carpetaTemporal = new File("temp_secuencial_" + System.currentTimeMillis());
        this.criterio = criterio;
        this.columnasDeseadas = columnasDeseadas;
    }

    public void procesar() {
        // 1. Determinar número de fragmentos (mismo que concurrente)
        int numProcesadores = Runtime.getRuntime().availableProcessors();
        int numSubArchivos = numProcesadores * 2;

        System.out.println("--- Inicio Procesamiento Secuencial ---");
        System.out.println("Núcleos detectados: " + numProcesadores);
        System.out.println("Dividiendo archivo en " + numSubArchivos + " fragmentos...");

        FileSplitter splitter = new FileSplitter();
        List<ResumenResultados> listaResultados = new ArrayList<>();

        try {
            // 2. Fase de División (Splitting) - NO se cuenta en el tiempo
            List<File> fragmentos = splitter.dividirArchivo(archivoOrigen, carpetaTemporal, numSubArchivos);

            // INICIAR CRONÓMETRO AQUÍ - Solo para procesamiento
            long tiempoInicio = System.currentTimeMillis();

            System.out.println("Procesando fragmentos uno por uno (secuencial)...");

            // 3. Procesar fragmentos UNO POR UNO (sin hilos)
            int idWorker = 0;
            for (File fragmento : fragmentos) {
                WorkerTask tarea = new WorkerTask(fragmento, carpetaTemporal, idWorker++, criterio, columnasDeseadas);

                try {
                    // Llamamos a .call() directamente en el hilo principal
                    // El programa se detiene aquí hasta que termine este fragmento
                    ResumenResultados resultado = tarea.call();
                    listaResultados.add(resultado);
                    System.out.println("Fragmento " + idWorker + " procesado.");

                } catch (Exception e) {
                    System.err.println("Error procesando fragmento: " + e.getMessage());
                }
            }

            // 4. Unificación (Merge)
            System.out.println("Unificando resultados...");
            unificarResultados(listaResultados, archivoFinal);

            // DETENER CRONÓMETRO - Después de unificar
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            // Estadísticas
            long totalProcesados = listaResultados.stream().mapToLong(r -> r.totalProcesados).sum();
            long totalAceptados = listaResultados.stream().mapToLong(r -> r.totalAceptados).sum();
            long totalErrores = listaResultados.stream().mapToLong(r -> r.totalErrores).sum();

            System.out.println("--- Fin Secuencial ---");
            System.out.println("Tiempo de procesamiento secuencial: " + (tiempoTotal / 1000.0) + " segundos.");
            System.out.println("Registros procesados: " + totalProcesados);
            System.out.println("Registros filtrados (guardados): " + totalAceptados);
            System.out.println("Errores encontrados: " + totalErrores);

        } catch (IOException e) {
            System.err.println("Error crítico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            limpiarTemporales();
        }
    }

    /**
     * Combina los archivos parciales en uno solo.
     */
    private void unificarResultados(List<ResumenResultados> resultados, File destino) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(destino))) {
            boolean esPrimerArchivo = true;
            for (ResumenResultados res : resultados) {
                try (BufferedReader lector = new BufferedReader(new FileReader(res.archivoResultado))) {
                    String linea = lector.readLine();
                    if (linea != null) {
                        if (esPrimerArchivo) {
                            escritor.write(linea);
                            escritor.newLine();
                            esPrimerArchivo = false;
                        }
                    }
                    while ((linea = lector.readLine()) != null) {
                        escritor.write(linea);
                        escritor.newLine();
                    }
                }
            }
        }
    }

    /**
     * Elimina la carpeta temporal y todo su contenido.
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