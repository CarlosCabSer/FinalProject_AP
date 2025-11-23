package Main;

import Hilos.CriterioFiltro;
import Hilos.Manager;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Clase principal (Entry Point).
 * Se encarga de la interacción con el usuario y la configuración inicial.
 */
public class App {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        mostrarBanner();

        // 1. Solicitar Archivo de Entrada
        File archivoEntrada = null;
        while (archivoEntrada == null || !archivoEntrada.exists()) {
            System.out.print("\n[1] Ingrese la ruta del archivo CSV (Dataset origen): ");
            String ruta = scanner.nextLine().trim();
            // Remover comillas si el usuario copió la ruta como texto
            ruta = ruta.replace("\"", "");
            archivoEntrada = new File(ruta);

            if (!archivoEntrada.exists()) {
                System.err.println("Error: El archivo no existe. Intente nuevamente.");
            }
        }

        // 2. Solicitar Ruta de Salida
        System.out.print("\n[2] Nombre del archivo de salida (ej. resultado.csv): ");
        String nombreSalida = scanner.nextLine().trim();
        if (nombreSalida.isEmpty()) nombreSalida = "resultado_filtrado.csv";

        // 3. Configurar Criterio de Filtrado
        System.out.println("\n--- Configuración del Filtro ---");
        System.out.println("Ingrese el índice de la columna a filtrar (iniciando en 0).");
        System.out.println("Ejemplo: Para 'Country', si es la 3ra columna, ingrese 2.");

        int indiceFiltro = -1;
        while (indiceFiltro < 0) {
            System.out.print(">> Índice de columna: ");
            try {
                indiceFiltro = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.println("Por favor ingrese un número entero válido.");
            }
        }

        System.out.print(">> Valor buscado (ej. '304', 'MX', '2019'): ");
        String valorFiltro = scanner.nextLine().trim();

        CriterioFiltro criterio = new CriterioFiltro(indiceFiltro, valorFiltro);

        // 4. Configurar Columnas de Resultado
        System.out.println("\n--- Columnas de Interés ---");
        System.out.println("Ingrese los índices de las columnas que desea en el archivo final, separados por coma.");
        System.out.println("Ejemplo: 0, 2, 7 (Para traer año, país y valor)");

        int[] columnasDeseadas = null;
        while (columnasDeseadas == null) {
            System.out.print(">> Columnas: ");
            String entradaCols = scanner.nextLine().trim();
            try {
                columnasDeseadas = Arrays.stream(entradaCols.split(","))
                        .map(String::trim)
                        .mapToInt(Integer::parseInt)
                        .toArray();
            } catch (NumberFormatException e) {
                System.err.println("Formato incorrecto. Use números separados por comas (ej: 0,1,5)");
            }
        }

        // 5. Ejecutar el Manager
        System.out.println("\nIniciando procesamiento concurrente...");

        Manager manager = new Manager(archivoEntrada, nombreSalida, criterio, columnasDeseadas);

        // Medición simple del tiempo global (Main)
        // Nota: El Manager también mide su tiempo interno, pero este es el tiempo de 'usuario'.
        long inicio = System.currentTimeMillis();

        manager.procesar();

        long fin = System.currentTimeMillis();
        System.out.println("Tiempo total de ejecución (Main): " + ((fin - inicio) / 1000.0) + " seg.");

        scanner.close();
    }

    private static void mostrarBanner() {
        System.out.println("=================================================");
        System.out.println("   PROCESADOR DE ARCHIVOS CSV MASIVOS - JAVA 8   ");
        System.out.println("        Patrón Manager-Worker (Concurrente)      ");
        System.out.println("=================================================");
    }
}