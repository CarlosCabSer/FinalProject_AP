package FileSplitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileSplitter {

    public List<File> dividirArchivo(File archivoOrigen, File carpetaDestino, int numSubArchivos) throws IOException {

        // Validar que la carpeta destino exista, si no, crearla
        if (!carpetaDestino.exists()) {
            carpetaDestino.mkdirs();
        }

        List<File> listaArchivosGenerados = new ArrayList<>();
        BufferedWriter[] escritores = new BufferedWriter[numSubArchivos];

        try {
            // 1. Inicializar los escritores (uno por cada subarchivo)
            for (int i = 0; i < numSubArchivos; i++) {
                // Nombre del archivo: fragmento_0.csv, fragmento_1.csv, etc.
                File subArchivo = new File(carpetaDestino, "fragmento_" + i + ".csv");
                listaArchivosGenerados.add(subArchivo);

                // Abrimos el flujo de escritura para este fragmento
                escritores[i] = new BufferedWriter(new FileWriter(subArchivo));
            }

            // 2. Leer el archivo origen
            try (BufferedReader lector = new BufferedReader(new FileReader(archivoOrigen))) {

                // Lectura y replicación del encabezado (Header)
                String encabezado = lector.readLine();
                if (encabezado != null) {
                    for (BufferedWriter escritor : escritores) {
                        escritor.write(encabezado);
                        escritor.newLine(); // Salto de línea importante
                    }
                }

                // 3. Distribución de líneas (Round Robin)
                String linea;
                long contadorLineas = 0;

                while ((linea = lector.readLine()) != null) {
                    // Calculamos a qué archivo le toca esta línea usando el módulo
                    int indiceArchivo = (int) (contadorLineas % numSubArchivos);

                    escritores[indiceArchivo].write(linea);
                    escritores[indiceArchivo].newLine();

                    contadorLineas++;
                }
            }

        } finally {
            // 4. Cerrar todos los escritores para asegurar que los datos se guarden
            // Esto se ejecuta pase lo que pase (incluso si hay error)
            for (BufferedWriter escritor : escritores) {
                if (escritor != null) {
                    escritor.close();
                }
            }
        }

        return listaArchivosGenerados;
    }
}