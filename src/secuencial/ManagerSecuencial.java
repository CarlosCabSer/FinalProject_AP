package secuencial;

import Hilos.CriterioFiltro;

import java.io.*;

/**
 * Versión Secuencial PURA - Procesa el archivo completo sin dividir.
 * Lee el archivo línea por línea y aplica filtros directamente.
 * Esta es la línea base (baseline) para comparar con la versión concurrente.
 */
public class ManagerSecuencial {

    private final File archivoOrigen;
    private final File archivoFinal;
    private final CriterioFiltro criterio;
    private final int[] columnasDeseadas;

    public ManagerSecuencial(File archivoOrigen, String rutaSalida, CriterioFiltro criterio, int[] columnasDeseadas) {
        this.archivoOrigen = archivoOrigen;
        this.archivoFinal = new File(rutaSalida);
        this.criterio = criterio;
        this.columnasDeseadas = columnasDeseadas;
    }

    public void procesar() {
        System.out.println("--- Inicio Procesamiento Secuencial (Sin División) ---");
        System.out.println("Procesando archivo completo de forma secuencial...");

        long lineasProcesadas = 0;
        long lineasAceptadas = 0;
        long lineasConError = 0;

        try {
            // INICIAR CRONÓMETRO - Solo para procesamiento
            long tiempoInicio = System.currentTimeMillis();

            // Procesar el archivo completo línea por línea
            try (BufferedReader lector = new BufferedReader(new FileReader(archivoOrigen));
                 BufferedWriter escritor = new BufferedWriter(new FileWriter(archivoFinal))) {

                // Leer y procesar encabezado
                String header = lector.readLine();
                if (header != null) {
                    escritor.write(filtrarColumnas(header, columnasDeseadas));
                    escritor.newLine();
                }

                // Procesar línea por línea
                String linea;
                while ((linea = lector.readLine()) != null) {
                    lineasProcesadas++;
                    try {
                        // Separar por comas (CSV simple)
                        String[] columnas = linea.split(",");

                        // Verificar criterio (Lógica de negocio)
                        if (cumpleCriterio(columnas, criterio)) {
                            // Construir la línea de salida solo con columnas deseadas
                            String lineaFiltrada = construirLineaSalida(columnas, columnasDeseadas);

                            escritor.write(lineaFiltrada);
                            escritor.newLine();
                            lineasAceptadas++;
                        }

                    } catch (Exception e) {
                        // Manejo de errores: Contar pero continuar
                        lineasConError++;
                    }
                }
            }

            // DETENER CRONÓMETRO
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            // Reporte final
            System.out.println("--- Fin Secuencial ---");
            System.out.println("Tiempo de procesamiento secuencial: " + (tiempoTotal / 1000.0) + " segundos.");
            System.out.println("Registros procesados: " + lineasProcesadas);
            System.out.println("Registros filtrados (guardados): " + lineasAceptadas);
            System.out.println("Errores encontrados: " + lineasConError);

        } catch (IOException e) {
            System.err.println("Error crítico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica si una fila cumple con el criterio de filtrado.
     */
    private boolean cumpleCriterio(String[] columnas, CriterioFiltro filtro) {
        if (filtro == null) return true;
        if (filtro.indiceColumna >= columnas.length) return false;

        String valorCelda = columnas[filtro.indiceColumna].trim();
        return valorCelda.equals(filtro.valorEsperado);
    }

    /**
     * Filtra las columnas del encabezado.
     */
    private String filtrarColumnas(String linea, int[] indices) {
        String[] partes = linea.split(",");
        return construirLineaSalida(partes, indices);
    }

    /**
     * Construye una línea de salida con solo las columnas seleccionadas.
     */
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
