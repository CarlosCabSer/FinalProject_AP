package Hilos;

import java.io.File;

/**
 * Clase inmutable para transportar los resultados del hilo al Manager.
 */
public class ResumenResultados {
    public final File archivoResultado;
    public final long totalProcesados;
    public final long totalAceptados;
    public final long totalErrores;

    public ResumenResultados(File archivo, long procesados, long aceptados, long errores) {
        this.archivoResultado = archivo;
        this.totalProcesados = procesados;
        this.totalAceptados = aceptados;
        this.totalErrores = errores;
    }
}