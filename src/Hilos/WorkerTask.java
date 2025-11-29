package Hilos;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.ArrayList;


public class WorkerTask implements Callable<ResumenResultados> {

    private final File archivoEntrada;     // El fragmento a leer (fragmento_n.csv)
    private final File carpetaTemporal;    // Donde guardar el resultado parcial
    private final int idWorker;            // Identificador para logs y nombres
    private final CriterioFiltro criterio; // Lógica de filtrado (ej. col 2 == "304")
    private final int[] columnasDeseadas;  // Índices de columnas a guardar (ej. {0, 2, 7})

    public WorkerTask(File archivoEntrada, File carpetaTemporal, int idWorker,
                      CriterioFiltro criterio, int[] columnasDeseadas) {
        this.archivoEntrada = archivoEntrada;
        this.carpetaTemporal = carpetaTemporal;
        this.idWorker = idWorker;
        this.criterio = criterio;
        this.columnasDeseadas = columnasDeseadas;
    }

    @Override
    public ResumenResultados call() throws Exception {
        // 1. Preparar archivos de salida (Resultado filtrado y Log de errores)
        File archivoSalida = new File(carpetaTemporal, "resultado_hilo_" + idWorker + ".csv");
        File archivoLog = new File(carpetaTemporal, "error_hilo_" + idWorker + ".log");

        long lineasProcesadas = 0;
        long lineasAceptadas = 0;
        long lineasConError = 0;

        // Usamos try-with-resources para asegurar el cierre de flujos
        try (BufferedReader lector = new BufferedReader(new FileReader(archivoEntrada));
             BufferedWriter escritorDatos = new BufferedWriter(new FileWriter(archivoSalida));
             BufferedWriter escritorLog = new BufferedWriter(new FileWriter(archivoLog))) {

            String linea;
            // Leer encabezado (asumimos que FileSplitter ya lo puso en la primera línea)
            String header = lector.readLine();

            // Opcional: Escribir el header en el archivo de salida (solo columnas deseadas)
            if (header != null) {
                escritorDatos.write(filtrarColumnas(header, columnasDeseadas));
                escritorDatos.newLine();
            }

            // 2. Procesar línea por línea
            while ((linea = lector.readLine()) != null) {
                lineasProcesadas++;
                try {
                    // Separar por comas (CSV simple)
                    String[] columnas = linea.split(",");

                    // Verificar criterio (Lógica de negocio)
                    if (cumpleCriterio(columnas, criterio)) {

                        // Construir la línea de salida solo con columnas deseadas
                        String lineaFiltrada = construirLineaSalida(columnas, columnasDeseadas);

                        escritorDatos.write(lineaFiltrada);
                        escritorDatos.newLine();
                        lineasAceptadas++;
                    }

                } catch (Exception e) {
                    // 3. Manejo de errores: Escribir en log sin detener el hilo
                    lineasConError++;
                    escritorLog.write("Error en línea " + lineasProcesadas + ": " + linea + " -> " + e.getMessage());
                    escritorLog.newLine();
                }
            }
        }

        // 4. Retornar el objeto con las estadísticas y la ruta del archivo generado
        return new ResumenResultados(archivoSalida, lineasProcesadas, lineasAceptadas, lineasConError);
    }

    // --- Métodos Auxiliares ---

    private boolean cumpleCriterio(String[] columnas, CriterioFiltro filtro) {
        // Si no hay filtro, pasa todo
        if (filtro == null) return true;

        // Validación de índice
        if (filtro.indiceColumna >= columnas.length) return false;

        String valorCelda = columnas[filtro.indiceColumna].trim();

        // Ejemplo simple: Comparación exacta o numérica
        // Aquí se puede expandir la lógica según el PDF (>, <, =, etc.)
        return valorCelda.equals(filtro.valorEsperado);
    }

    private String filtrarColumnas(String linea, int[] indices) {
        String[] partes = linea.split(",");
        return construirLineaSalida(partes, indices);
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