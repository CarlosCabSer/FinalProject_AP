package Main;

import Hilos.CriterioFiltro;
import Hilos.Manager;
import secuencial.ManagerSecuencial; // Importamos el nuevo manager

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Clase Principal (Entry Point).
 * Permite configurar la ejecución y seleccionar entre modo Concurrente o Secuencial.
 * Ambos modos reutilizan la lógica de división (FileSplitter) y procesamiento (WorkerTask)
 * para garantizar una comparativa justa de rendimiento.
 */
public class App {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        mostrarBanner();

        // --- 1. CONFIGURACIÓN DEL ARCHIVO DE ENTRADA ---
        File archivoEntrada = null;
        while (archivoEntrada == null || !archivoEntrada.exists()) {
            System.out.print("\n[1] Ruta del dataset (CSV): ");
            String ruta = scanner.nextLine().trim().replace("\"", "");
            archivoEntrada = new File(ruta);

            if (!archivoEntrada.exists()) {
                System.err.println("   Error: El archivo no existe. Intente nuevamente.");
            }
        }

        // --- 2. CONFIGURACIÓN DE SALIDA ---
        System.out.print("\n[2] Nombre archivo salida (Enter para default): ");
        String nombreSalida = scanner.nextLine().trim();
        if (nombreSalida.isEmpty()) nombreSalida = "resultados_japon.csv";

        // --- 3. CONFIGURACIÓN DE FILTROS ---
        System.out.println("\n--- [3] Filtros de Negocio ---");
        System.out.println("   Columnas disponibles para filtrar:");
        System.out.println("     0: yearmonth");
        System.out.println("     1: exp_imp");
        System.out.println("     2: Country");
        System.out.println("     3: Custom");
        System.out.println("     4: hs9");
        System.out.println("     5: Q1");
        System.out.println("     6: Q2");
        System.out.println("     7: Value");
        int indiceFiltro = leerEntero(scanner, "\n   Índice de columna a filtrar: ");
        System.out.print("   Valor buscado (ej. '304', '2019', '1'): ");
        String valorFiltro = scanner.nextLine().trim();
        CriterioFiltro criterio = new CriterioFiltro(indiceFiltro, valorFiltro);

        // --- 4. CONFIGURACIÓN DE COLUMNAS (PROYECCIÓN) ---
        System.out.println("\n--- [4] Columnas a guardar ---");
        System.out.println("   Columnas disponibles:");
        System.out.println("     0: yearmonth");
        System.out.println("     1: exp_imp");
        System.out.println("     2: Country");
        System.out.println("     3: Custom");
        System.out.println("     4: hs9");
        System.out.println("     5: Q1");
        System.out.println("     6: Q2");
        System.out.println("     7: Value");
        System.out.println("\n   Ingrese índices separados por coma (ej: 0,2,7)");
        System.out.println("   O ingrese '*' para seleccionar todas las columnas");
        int[] columnasDeseadas = null;
        while (columnasDeseadas == null) {
            System.out.print("   >> Índices: ");
            try {
                String linea = scanner.nextLine().trim();

                // Si el usuario ingresa '*', seleccionar todas las columnas
                if (linea.equals("*")) {
                    columnasDeseadas = obtenerTodasLasColumnas(archivoEntrada);
                    System.out.println("   Se seleccionaron todas las columnas (" + columnasDeseadas.length + " columnas)");
                } else {
                    columnasDeseadas = Arrays.stream(linea.split(","))
                            .map(String::trim)
                            .mapToInt(Integer::parseInt)
                            .toArray();
                }
            } catch (Exception e) {
                System.err.println("   Error: Formato inválido. Use solo números y comas, o '*' para todas.");
            }
        }

        // --- 5. SELECCIÓN DE MODO (1: Concurrente, 2: Secuencial) ---
        System.out.println("\n=================================================");
        System.out.println(" SELECCIONE ESTRATEGIA DE EJECUCIÓN ");
        System.out.println("=================================================");
        System.out.println(" [1] CONCURRENTE (Manager-Worker con Hilos)");
        System.out.println("     - Divide el archivo y procesa en PARALELO.");
        System.out.println("     - Usa todos los núcleos del CPU.");
        System.out.println("\n [2] SECUENCIAL (Mismo Split, Sin Hilos)");
        System.out.println("     - Divide el archivo igual que [1].");
        System.out.println("     - Procesa fragmentos UNO POR UNO (sin paralelismo).");
        System.out.println("     - Usa solo 1 núcleo (Línea base para comparar).");
        System.out.println("=================================================");
        int opcion = leerEntero(scanner, ">> Opcion (1 o 2): ");

        // --- 6. EJECUCIÓN Y MEDICIÓN ---
        System.out.println("\nProcesando...");
        long inicio = System.currentTimeMillis();

        if (opcion == 1) {
            // MODO CONCURRENTE
            Manager manager = new Manager(archivoEntrada, nombreSalida, criterio, columnasDeseadas);
            manager.procesar();

        } else {
            // MODO SECUENCIAL (REUTILIZABLE)
            if (!nombreSalida.contains("_sec")) {
                nombreSalida = nombreSalida.replace(".csv", "_secuencial.csv");
            }

            ManagerSecuencial managerSec = new ManagerSecuencial(archivoEntrada, nombreSalida, criterio, columnasDeseadas);
            managerSec.procesar();
        }

        long fin = System.currentTimeMillis();
        double segundos = (fin - inicio) / 1000.0;

        System.out.println("\n*************************************************");
        System.out.println(" REPORTE FINAL DE TIEMPO (MAIN)");
        System.out.println(" Modo: " + (opcion == 1 ? "CONCURRENTE" : "SECUENCIAL"));
        System.out.println(" Tiempo Total: " + segundos + " segundos.");
        System.out.println(" Archivo generado: " + nombreSalida);
        System.out.println("*************************************************");

        scanner.close();
    }

    // Método auxiliar para leer enteros de forma segura
    private static int leerEntero(Scanner sc, String mensaje) {
        int valor = -1;
        while (valor < 0) {
            System.out.print(mensaje);
            try {
                String input = sc.nextLine().trim();
                valor = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.err.println("   Por favor ingrese un número válido.");
            }
        }
        return valor;
    }

    private static void mostrarBanner() {
        System.out.println("#################################################");
        System.out.println("#      ANALIZADOR DE COMERCIO EXTERIOR (JAPÓN)  #");
        System.out.println("#           Proyecto Final - PA2026-1           #");
        System.out.println("#################################################");
    }

    /**
     * Lee el archivo CSV y determina el número total de columnas.
     * Retorna un array con todos los índices de columnas (0, 1, 2, ..., n-1)
     */
    private static int[] obtenerTodasLasColumnas(File archivoCsv) {
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoCsv))) {
            String primeraLinea = reader.readLine();
            if (primeraLinea != null) {
                String[] columnas = primeraLinea.split(",");
                int totalColumnas = columnas.length;

                // Crear array con índices [0, 1, 2, ..., n-1]
                int[] indices = new int[totalColumnas];
                for (int i = 0; i < totalColumnas; i++) {
                    indices[i] = i;
                }
                return indices;
            }
        } catch (IOException e) {
            System.err.println("   Error al leer el archivo para determinar columnas: " + e.getMessage());
        }

        // Si hay error, retornar array vacío
        return new int[0];
    }
}