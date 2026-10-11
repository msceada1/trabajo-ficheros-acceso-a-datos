import exceptions.LimiteBytesException;
import exceptions.PosicionException;
import exceptions.RegistroDuplicadoException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private static final String THIRD_KEY = "Modelo";

    private final Map<String, Integer> campos = new LinkedHashMap<>();
    private long numeroRegistros; // indica la longitud de registros que hay
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

    public long getNumeroRegistros() {
        return numeroRegistros;
    }

    private static void cargarFicherocsv() {
    }

    /**
     * Metodo encargado de pasar un registro completo a bytes
     *
     * @param registro el registro a guardar
     * @return el registro en un array de bytes
     * @throws LimiteBytesException si uno de los campos se pasa del limite de bytes que debe ocupar
     */
    private byte[] pasarRegistroABytes(Map<String, String> registro) throws LimiteBytesException {
        byte[] arrayDeBytes = new byte[LONGITUD_TOTAL_REGISTRO_EN_BYTES];
        int desplazamiento = 0;

        for (Map.Entry<String, Integer> campo : this.campos.entrySet()) {
            String clave = campo.getKey();
            String valor = registro.getOrDefault(clave, ""); //valor por defecto con "" si no entra valor para evitar nullPointerException
            int bytesCampo = campo.getValue();

            byte[] buffer = convertirCampoABytes(valor, bytesCampo);
            System.arraycopy(buffer, 0, arrayDeBytes, desplazamiento, bytesCampo);
            desplazamiento += bytesCampo;
        }

        return arrayDeBytes;
    }

    /**
     * Metodo encargado de comprobar los bytes que ocupa el campo introducido y rellenar
     * si no llega a los bytes que debe cumplir
     *
     * @param campo         el campo del registro a comprobar
     * @param bytesACumplir los bytes que debe ocupar el campo
     * @return un array de bytes con los bytes que ocupa el campo
     * @throws LimiteBytesException si los bytes sobrepasan la longitud a cumplir
     */
    private byte[] convertirCampoABytes(String campo, int bytesACumplir) throws LimiteBytesException {
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

    public void insertar(Map<String, String> registroALeer, int posicion)
            throws RegistroDuplicadoException, IOException {
        String valorMatricula = registroALeer.get(PRIMARY_KEY);

        if (registroExiste(valorMatricula)) {
            throw new RegistroDuplicadoException("Error: el registro ya existe");
        }

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            try {
                ByteBuffer bf = ByteBuffer.wrap(pasarRegistroABytes(registroALeer)); //convierte un array de bytes en un Buffer
                moverRegistro(fl, posicion);//si la posicion estaba ocupada se mueve a la derecha u sus suguientes tambien
                fl.position(LONGITUD_TOTAL_REGISTRO_EN_BYTES * posicion); //desplazamiento del puntero
                fl.write(bf);
                bf.clear();
            } catch (LimiteBytesException e) {
                System.out.println(e.getMessage());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Metodo que comprueba si un registro ya existe o no
     *
     * @param matricula la matricula del coche a insertar
     * @return true si el registro existe o false en caso contrario
     * @throws IOException
     */
    private boolean registroExiste(String matricula) throws IOException {
        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ)) {
            ByteBuffer bf = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);

            for (int i = 0; i < this.numeroRegistros; i++) {
                fl.read(bf); //vuelca el contenido en el Buffer
                byte[] lecturaRegistro = bf.array();

                if (lecturaRegistro[0] == 0) { //registro borrado
                    bf.clear();
                    continue;
                }

                int desplazamientoCampo = 0;
                String valorClaveLeida = null;
                for (Map.Entry<String, Integer> campo : this.campos.entrySet()) {
                    if (campo.getKey().equals(PRIMARY_KEY)) {
                        valorClaveLeida = extraerStringDeBytes(lecturaRegistro, desplazamientoCampo, campo.getValue());
                        break;
                    }
                    desplazamientoCampo += campo.getValue();
                }

                if (valorClaveLeida != null && valorClaveLeida.equals(matricula)) {
                    return true;
                }
                bf.clear(); //el buffer se "vacia" para que en la siguiente iteracion no tenga los datos del registro anterior
            }
        }

        return false;
    }

    /**
     * Metodo encargado de desplazar los registros cuando el usuario desee introducir uno donde hay otro existente
     *
     * @param fl              el canal de manipulacion del fichero de la base de datos
     * @param posicionDeseada la posicion donde el usuario quiere introducir un registro
     * @throws IOException
     */
    private void moverRegistro(FileChannel fl, int posicionDeseada) throws IOException {
        long totalPlazas = fl.size() / LONGITUD_TOTAL_REGISTRO_EN_BYTES;

        if (posicionDeseada >= totalPlazas) { //no hay registro que desplazar, se acaba el metodo
            return;
        }

        ByteBuffer br = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);

        for (long i = totalPlazas - 1; i >= posicionDeseada; i--) {
            br.clear();


            fl.position(i * LONGITUD_TOTAL_REGISTRO_EN_BYTES); //acceso a la posicion y lectura del coche
            fl.read(br);
            br.flip(); //cambio de lectura a escritura

            fl.position((i + 1) * LONGITUD_TOTAL_REGISTRO_EN_BYTES); //desplazamiento
            fl.write(br);
        }
    }


    /*
    private Map<String, Integer> buscarRegistro() throws IOException {
        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ)) {
            ByteBuffer bf = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);
        }
    }

     */


    private String extraerStringDeBytes(byte[] buffer, int desplazamiento, int longitud) {
        String texto = new String(buffer, desplazamiento, longitud, StandardCharsets.UTF_8);
        return texto.trim(); //los bytes que sean espacios en blanco los quita
    }

    static void main() {
        GestorBBDD gb = new GestorBBDD();

        Map<String, String> coche = new LinkedHashMap<>(Map.of("Matricula", "5678def", "Marca", "seat", "Modelo", "ibiza"));
    }
}