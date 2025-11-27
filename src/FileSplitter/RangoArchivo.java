package FileSplitter;

/**
 * Representa un rango de bytes dentro de un archivo.
 * Se usa para que cada WorkerTask sepa qué porción del archivo debe procesar.
 */
public class RangoArchivo {
    public final long inicio;      // Byte de inicio (inclusive)
    public final long fin;         // Byte de fin (inclusive)
    public final int idFragmento;  // Identificador del fragmento

    public RangoArchivo(long inicio, long fin, int idFragmento) {
        this.inicio = inicio;
        this.fin = fin;
        this.idFragmento = idFragmento;
    }

    public long getTamano() {
        return fin - inicio + 1;
    }

    @Override
    public String toString() {
        return String.format("Fragmento %d: [%d - %d] (%d bytes)",
            idFragmento, inicio, fin, getTamano());
    }
}
