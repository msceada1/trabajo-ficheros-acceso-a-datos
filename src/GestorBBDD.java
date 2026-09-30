import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class GestorBBDD {


    private static final byte ESPACIO_EN_BLANCO = (byte) ' ';
    private static final int MAX_BYTES_MATRICULA = 7;
    private static final int MAX_BYTES_MARCA = 32;
    private static final int MAX_BYTES_MODELO = 32;
    private static final Path RUTA_FICHERO_CSV = Path.of("src", "ficheros", "BBDDCoches.csv");
    private static final Path RUTA_FICHERO_CARGA_BBDD = Path.of("src", "ficheros", "carga_coches.dat");
    private static final String PRIMARY_KEY = "Matricula";

    private final Map<String, Integer> campos = new LinkedHashMap<>();
    private int longitudRegistro; //indica la longitud que tiene un registro en bytes
    private long numeroRegistros; //indica la longitud de registros que hay en bytes
    private long registrosBorrados;

    //constructor
    public GestorBBDD() {
        campos.put("Matricula", MAX_BYTES_MATRICULA);
        campos.put("Marca", MAX_BYTES_MARCA);
        campos.put("Modelo", MAX_BYTES_MODELO);

        this.longitudRegistro = 0;
        this.numeroRegistros = 0;
        this.registrosBorrados = 0;

        for (Map.Entry<String, Integer> campo : campos.entrySet()) {
            this.longitudRegistro += campo.getValue();
        }

        if (Files.exists(RUTA_FICHERO_CARGA_BBDD)) {
            try {
                this.numeroRegistros = Files.size(RUTA_FICHERO_CARGA_BBDD) / this.longitudRegistro;
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

    private static void cumpleBytes(Map<String, Integer> valores, Integer bytesACumplir) { //preguntar

    }

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

                this.campos.values().stream().filter()

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    private String extraerStringDeBytes(byte[] buffer, int offset, int longitud) {
        String texto = new String(buffer, offset, longitud, StandardCharsets.UTF_8);
        return texto.trim();
    }

}
