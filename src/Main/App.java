package Main;

import Hilos.CriterioFiltro;
import Hilos.Manager;
import secuencial.ManagerSecuencial; // Importamos el nuevo manager

import java.io.File;
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
        int indiceFiltro = leerEntero(scanner, "   Índice de columna a filtrar (empieza en 0): ");

        System.out.print("   Valor buscado (ej. '304', '2019', '1'): ");
        String valorFiltro = scanner.nextLine().trim();
        CriterioFiltro criterio = new CriterioFiltro(indiceFiltro, valorFiltro);

        // --- 4. CONFIGURACIÓN DE COLUMNAS (PROYECCIÓN) ---
        System.out.println("\n--- [4] Columnas a guardar ---");
        System.out.println("   Ingrese índices separados por coma (ej: 0,2,7)");
        int[] columnasDeseadas = null;
        while (columnasDeseadas == null) {
            System.out.print("   >> Índices: ");
            try {
                String linea = scanner.nextLine().trim();
                columnasDeseadas = Arrays.stream(linea.split(","))
                        .map(String::trim)
                        .mapToInt(Integer::parseInt)
                        .toArray();
            } catch (Exception e) {
                System.err.println("   Error: Formato inválido. Use solo números y comas.");
            }
        }

        // --- 5. SELECCIÓN DE MODO (EL PUNTO CLAVE DEL PROYECTO) ---
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

        int opcion = leerEntero(scanner, ">> Opción (1 o 2): ");

        // --- 6. EJECUCIÓN Y MEDICIÓN ---
        System.out.println("\nIniciando proceso... (El cronómetro corre ahora)");
        long inicio = System.currentTimeMillis();

        if (opcion == 1) {
            // MODO CONCURRENTE
            Manager manager = new Manager(archivoEntrada, nombreSalida, criterio, columnasDeseadas);
            manager.procesar();
        } else {
            // MODO SECUENCIAL (REUTILIZABLE)
            // Ajustamos el nombre para no sobrescribir la prueba concurrente
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
}