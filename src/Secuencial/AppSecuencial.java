package Secuencial;

import Hilos.CriterioFiltro;
import java.io.File;

/**
 * Entry Point para la versión Secuencial.
 * Configurado 'harcoded' o simple para pruebas rápidas de comparación.
 */
public class AppSecuencial {
    public static void main(String[] args) {

        // NOTA: Para comparar justamente, usa LOS MISMOS parámetros que en la versión concurrente.

        // Ajusta estas rutas a tu entorno local
        File archivoEntrada = new File("C:\\Users\\cabse\\Documents\\FinalProject_AP\\src\\Data\\JapanTrade.csv");
        String rutaSalida = "resultado_secuencial.csv";

        // Ejemplo: Filtrar por Country = 304 (México) y traer columnas 0,2,7
        CriterioFiltro criterio = new CriterioFiltro(2, "304");
        int[] columnas = {0, 2, 7};

        if (archivoEntrada.exists()) {
            Secuencial procesador = new Secuencial(
                    archivoEntrada, rutaSalida, criterio, columnas
            );
            procesador.procesar();
        } else {
            System.err.println("Archivo no encontrado: " + archivoEntrada.getAbsolutePath());
        }
    }
}