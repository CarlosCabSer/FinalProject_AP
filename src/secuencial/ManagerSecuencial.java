package secuencial;

import Hilos.CriterioFiltro;
import Hilos.ResumenResultados;
import Hilos.WorkerTask; // ¡Reutilizamos la lógica del worker!
import FileSplitter.FileSplitter; // ¡Reutilizamos el splitter!

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Versión Secuencial que reutiliza la arquitectura del sistema.
 * Realiza: Split -> Proceso Lineal -> Merge
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
        // Usamos una carpeta distinta para no mezclar con la ejecución concurrente si se corren a la vez
        this.carpetaTemporal = new File("temp_secuencial_" + System.currentTimeMillis());
        this.criterio = criterio;
        this.columnasDeseadas = columnasDeseadas;
    }

    public void procesar() {
        long tiempoInicio = System.currentTimeMillis();

        // Similamos la división igual que en la versión concurrente
        // Usamos 4 fragmentos por defecto solo para simular la carga de trabajo
        int numSubArchivos = 4;

        System.out.println("--- Inicio Procesamiento Secuencial (Reutilizando Componentes) ---");
        System.out.println("Dividiendo archivo en " + numSubArchivos + " fragmentos...");

        FileSplitter splitter = new FileSplitter();
        List<ResumenResultados> listaResultados = new ArrayList<>();

        try {
            // 1. REUTILIZACIÓN: Dividimos el archivo igual que en la versión con hilos
            List<File> fragmentos = splitter.dividirArchivo(archivoOrigen, carpetaTemporal, numSubArchivos);

            System.out.println("Procesando fragmentos uno por uno (Cuello de botella intencional)...");

            int idWorker = 0;
            for (File fragmento : fragmentos) {
                // 2. REUTILIZACIÓN: Instanciamos la misma tarea
                WorkerTask tarea = new WorkerTask(fragmento, carpetaTemporal, idWorker++, criterio, columnasDeseadas);

                try {
                    // AQUÍ ESTÁ LA CLAVE:
                    // En lugar de enviarlo a un hilo (executor.submit),
                    // lo ejecutamos en ESTE mismo hilo principal llamando a .call()
                    // El programa se detiene aquí hasta que termine este fragmento.
                    ResumenResultados resultado = tarea.call();

                    listaResultados.add(resultado);
                    System.out.println("Fragmento " + (idWorker) + " procesado."); // Feedback visual

                } catch (Exception e) {
                    System.err.println("Error procesando fragmento: " + e.getMessage());
                }
            }

            // 3. Unificación (Merge)
            System.out.println("Unificando resultados...");
            unificarResultados(listaResultados, archivoFinal);

            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            // Estadísticas
            long totalProcesados = listaResultados.stream().mapToLong(r -> r.totalProcesados).sum();

            System.out.println("--- Fin Secuencial ---");
            System.out.println("Tiempo total: " + (tiempoTotal / 1000.0) + " segundos.");
            System.out.println("Registros procesados: " + totalProcesados);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            limpiarTemporales();
        }
    }

    // Este método es idéntico al del Manager concurrente.
    // En un caso real, esto iría en una clase Utils para no copiar-pegar.
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

    private void limpiarTemporales() {
        if (carpetaTemporal.exists()) {
            File[] archivos = carpetaTemporal.listFiles();
            if (archivos != null) for (File f : archivos) f.delete();
            carpetaTemporal.delete();
        }
    }
}