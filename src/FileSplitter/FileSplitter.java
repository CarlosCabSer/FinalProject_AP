package FileSplitter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * FileSplitter ULTRA-OPTIMIZADO usando NIO Channels y división por bytes.
 * Usa transferencia directa de bytes sin conversión a String.
 */
public class FileSplitter {

    public List<File> dividirArchivo(File archivoOrigen, File carpetaDestino, int numSubArchivos) throws IOException {

        if (!carpetaDestino.exists()) {
            carpetaDestino.mkdirs();
        }

        List<File> listaArchivosGenerados = new ArrayList<File>();
        long tamañoArchivo = archivoOrigen.length();

        // Leer encabezado usando RandomAccessFile (más rápido)
        byte[] encabezadoBytes;
        long inicioContenido;

        try (RandomAccessFile raf = new RandomAccessFile(archivoOrigen, "r")) {
            // Leer primera línea (encabezado) - buscar primer \n
            long pos = 0;
            while (pos < tamañoArchivo && raf.read() != '\n') {
                pos++;
            }
            pos++; // Incluir el \n

            inicioContenido = pos;

            // Leer el encabezado
            encabezadoBytes = new byte[(int) inicioContenido];
            raf.seek(0);
            raf.readFully(encabezadoBytes);
        }

        // Calcular tamaño de cada fragmento en bytes
        long tamañoContenido = tamañoArchivo - inicioContenido;
        long bytesPerFragmento = tamañoContenido / numSubArchivos;

        System.out.println("   Tamaño total: " + (tamañoArchivo / 1024 / 1024) + " MB");
        System.out.println("   Bytes por fragmento: ~" + (bytesPerFragmento / 1024 / 1024) + " MB");

        // Usar FileChannel para transferencia de bytes ultra-rápida
        try (RandomAccessFile rafOrigen = new RandomAccessFile(archivoOrigen, "r");
             FileChannel canalOrigen = rafOrigen.getChannel()) {

            for (int i = 0; i < numSubArchivos; i++) {
                File fragmento = new File(carpetaDestino, "fragmento_" + i + ".csv");
                listaArchivosGenerados.add(fragmento);

                // Calcular posición inicial de este fragmento
                long startPos = inicioContenido + (i * bytesPerFragmento);
                long endPos;

                if (i == numSubArchivos - 1) {
                    endPos = tamañoArchivo;
                } else {
                    endPos = startPos + bytesPerFragmento;
                }

                // Ajustar posiciones para no cortar líneas
                if (i > 0) {
                    startPos = buscarInicioLineaSiguiente(rafOrigen, startPos);
                }
                if (i < numSubArchivos - 1) {
                    endPos = buscarInicioLineaSiguiente(rafOrigen, endPos);
                }

                // Escribir fragmento usando FileChannel (transferencia directa)
                escribirFragmentoRapido(canalOrigen, fragmento, encabezadoBytes, startPos, endPos);
            }
        }

        return listaArchivosGenerados;
    }

    /**
     * Busca el inicio de la siguiente línea después de una posición.
     */
    private long buscarInicioLineaSiguiente(RandomAccessFile raf, long posicion) throws IOException {
        raf.seek(posicion);
        int b;
        while ((b = raf.read()) != -1) {
            if (b == '\n') {
                return raf.getFilePointer();
            }
        }
        return raf.length();
    }

    /**
     * Escribe fragmento usando FileChannel para máxima velocidad.
     * Usa transferencia directa de bytes sin conversión a String.
     */
    private void escribirFragmentoRapido(FileChannel canalOrigen, File destino,
                                         byte[] encabezadoBytes, long startPos, long endPos) throws IOException {

        try (RandomAccessFile rafDestino = new RandomAccessFile(destino, "rw");
             FileChannel canalDestino = rafDestino.getChannel()) {

            // Escribir encabezado directamente
            ByteBuffer bufferEncabezado = ByteBuffer.wrap(encabezadoBytes);
            canalDestino.write(bufferEncabezado);

            // Transferir datos directamente del canal origen al destino
            // Esta es la operación MÁS RÁPIDA posible en Java I/O
            long bytesTransferidos = 0;
            long bytesTotales = endPos - startPos;
            long posOrigen = startPos;

            while (bytesTransferidos < bytesTotales) {
                long transferidos = canalOrigen.transferTo(
                    posOrigen + bytesTransferidos,
                    Math.min(bytesTotales - bytesTransferidos, 1024 * 1024 * 64), // 64MB chunks
                    canalDestino
                );

                if (transferidos == 0) {
                    break;
                }

                bytesTransferidos += transferidos;
            }
        }
    }
}
