import exceptions.LimiteBytesException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class GestorBBDD {

    private static final byte ESPACIO_EN_BLANCO = (byte) ' ';
    private static final int MAX_BYTES_MATRICULA = 7;
    private static final int MAX_BYTES_MARCA = 32;
    private static final int MAX_BYTES_MODELO = 32;
    private static final int LONGITUD_TOTAL_REGISTRO_EN_BYTES = 71;
    private static final Path RUTA_FICHERO_CSV = Path.of("src", "ficheros", "BBDDCoches.csv");
    private static final Path RUTA_FICHERO_CARGA_BBDD = Path.of("src", "ficheros", "carga_coches.dat");
    private static final String PRIMARY_KEY = "Matricula";
    private static final String SECOND_KEY = "Marca";
    private static final String THIRD_KEY = "Modelo"; // Corregido de "Marca" a "Modelo"

    private final Map<String, Integer> campos = new LinkedHashMap<>();
    private long numeroRegistros; // indica la longitud de registros que hay en bytes
    private long registrosBorrados;

    // Constructor
    public GestorBBDD() {
        campos.put(PRIMARY_KEY, MAX_BYTES_MATRICULA);
        campos.put(SECOND_KEY, MAX_BYTES_MARCA);
        campos.put(THIRD_KEY, MAX_BYTES_MODELO);

        this.numeroRegistros = 0;
        this.registrosBorrados = 0;

        if (Files.exists(RUTA_FICHERO_CARGA_BBDD)) {
            try {
                this.numeroRegistros = Files.size(RUTA_FICHERO_CARGA_BBDD) / LONGITUD_TOTAL_REGISTRO_EN_BYTES;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            try {
                Files.createFile(RUTA_FICHERO_CARGA_BBDD);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void cargarFicherocsv() {
    }

    private byte[] pasarRegistroABytes() {
        byte[] arrayDeBytes = new byte[LONGITUD_TOTAL_REGISTRO_EN_BYTES];


        return arrayDeBytes;
    }

    /**
     * Metodo encargado de comprobar los bytes que ocupa el campo introducido y rellenar
     * si no llega a los bytes que debe cumplir
     * @param campo el campo del registro a comprobar
     * @param bytesACumplir los bytes que debe ocupar el campo
     * @return un array de bytes con los bytes que ocupa el campo
     * @throws LimiteBytesException si los bytes sobrepasan la longitud a cumplir
     */
    private byte[] covertirCampoABytes(String campo, int bytesACumplir) throws LimiteBytesException {
        byte[] bytesEntrantes = campo.getBytes(StandardCharsets.UTF_8);

        if (bytesEntrantes.length > bytesACumplir) {
            throw new LimiteBytesException("Uno de los campos ha pasado del limite de espacio");
        }

        byte[] bytesCumplidos = new byte[bytesACumplir];

        Arrays.fill(bytesCumplidos, ESPACIO_EN_BLANCO); //bytes cumplidos se llena de espacios en blanco

        //los bytes del campo se guardan dentro de bytesCumplidos
        System.arraycopy(bytesEntrantes, 0, bytesCumplidos, 0, bytesEntrantes.length);


        return bytesCumplidos;
    }

    public void insertar(Map<String, Integer> registroALeer) {
    }

    /*
    private Map<String, Integer> buscarRegistro() {
        try (InputStream in = Files.newInputStream(RUTA_FICHERO_CARGA_BBDD)) {
            int pos = 0;

            while (pos < this.numeroRegistros) {
                byte[] buffer = new byte[this.longitudRegistro];
                if (in.read(buffer) < this.longitudRegistro) {
                    return null;
                }

                if (buffer[0] == 0) { //preguntar
                    pos++;
                    continue;
                }

                int referencia = 0;
                String claveLeida = null;

                //this.campos.values().stream().filter()

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    */

    /*
    private String extraerStringDeBytes(byte[] buffer, int offset, int longitud) {
        String texto = new String(buffer, offset, longitud, StandardCharsets.UTF_8);
        return texto.trim();
    }
    */
}