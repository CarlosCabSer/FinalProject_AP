package Hilos;

/**
 * Configuración simple del filtro (Patrón DTO).
 * Ejemplo: indiceColumna = 2 (Country), valorEsperado = "304" (Mexico)
 */
public class CriterioFiltro {
    public int indiceColumna;
    public String valorEsperado;
    // Se podría agregar un enum para el operador: MAYOR_QUE, IGUAL, MENOR_QUE

    public CriterioFiltro(int indiceColumna, String valorEsperado) {
        this.indiceColumna = indiceColumna;
        this.valorEsperado = valorEsperado;
    }
}