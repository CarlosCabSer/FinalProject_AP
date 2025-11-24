package Secuencial;

import Hilos.CriterioFiltro; // Reusamos la clase de modelo
import java.io.*;

/**
 * Implementación Secuencial (Línea Base).
 * Procesa el archivo línea por línea en el hilo principal sin dividirlo.
 */
public class Secuencial {

    private final File archivoOrigen;
    private final File archivoDestino;
    private final CriterioFiltro criterio;
    private final int[] columnasDeseadas;

    public Secuencial(File archivoOrigen, String rutaSalida,
                                CriterioFiltro criterio, int[] columnasDeseadas) {
        this.archivoOrigen = archivoOrigen;
        this.archivoDestino = new File(rutaSalida);
        this.criterio = criterio;
        this.columnasDeseadas = columnasDeseadas;
    }

    public void procesar() {
        System.out.println("--- Iniciando Procesamiento Secuencial ---");
        long inicio = System.currentTimeMillis();

        long lineasTotales = 0;
        long lineasGuardadas = 0;
        long errores = 0;

        // Log de errores específico para la versión secuencial
        File archivoLog = new File("errores_secuencial.log");

        try (BufferedReader lector = new BufferedReader(new FileReader(archivoOrigen));
             BufferedWriter escritor = new BufferedWriter(new FileWriter(archivoDestino));
             BufferedWriter logWriter = new BufferedWriter(new FileWriter(archivoLog))) {

            String linea;

            // 1. Manejo del Encabezado
            String header = lector.readLine();
            if (header != null) {
                escritor.write(construirLineaSalida(header.split(","), columnasDeseadas));
                escritor.newLine();
            }

            // 2. Bucle principal (Cuello de botella en secuencial)
            while ((linea = lector.readLine()) != null) {
                lineasTotales++;

                // Feedback visual cada millón de registros para saber que no se trabó
                if (lineasTotales % 1_000_000 == 0) {
                    System.out.println("Procesando línea: " + lineasTotales + "...");
                }

                try {
                    String[] columnas = linea.split(",");

                    if (cumpleCriterio(columnas, criterio)) {
                        String salida = construirLineaSalida(columnas, columnasDeseadas);
                        escritor.write(salida);
                        escritor.newLine();
                        lineasGuardadas++;
                    }
                } catch (Exception e) {
                    errores++;
                    logWriter.write("Error en línea " + lineasTotales + ": " + e.getMessage());
                    logWriter.newLine();
                }
            }

        } catch (IOException e) {
            System.err.println("Error fatal de E/S: " + e.getMessage());
        }

        long fin = System.currentTimeMillis();
        double tiempoSegundos = (fin - inicio) / 1000.0;

        System.out.println("--- Fin del Procesamiento Secuencial ---");
        System.out.println("Tiempo Total: " + tiempoSegundos + " segundos.");
        System.out.println("Registros leídos: " + lineasTotales);
        System.out.println("Registros guardados: " + lineasGuardadas);
        System.out.println("Errores: " + errores);
    }

    // --- Métodos Auxiliares (Misma lógica que en WorkerTask) ---

    private boolean cumpleCriterio(String[] columnas, CriterioFiltro filtro) {
        if (filtro == null) return true;
        if (filtro.indiceColumna >= columnas.length) return false;
        return columnas[filtro.indiceColumna].trim().equals(filtro.valorEsperado);
    }

    private String construirLineaSalida(String[] columnas, int[] indices) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indices.length; i++) {
            int indice = indices[i];
            if (indice < columnas.length) {
                sb.append(columnas[indice]);
                if (i < indices.length - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }
}