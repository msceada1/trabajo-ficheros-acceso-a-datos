import exceptions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class GestorBBDD {

    //definicion de constantes
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

    // Constructor
    public GestorBBDD() {
        campos.put(PRIMARY_KEY, MAX_BYTES_MATRICULA);
        campos.put(SECOND_KEY, MAX_BYTES_MARCA);
        campos.put(THIRD_KEY, MAX_BYTES_MODELO);
    }

    /**
     * Metodo encargado de cargar el fichero csv en la base de datos
     */
    public void cargarFicheroCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader(RUTA_FICHERO_CSV.toFile()))) {
            String linea;
            boolean esPrimeraLinea = true; //variable que permitira no leer el nombre de los campos en la primera linea

            while ((linea = br.readLine()) != null) {
                if (esPrimeraLinea) {
                    esPrimeraLinea = false;
                    continue;
                }

                String[] datos = linea.split(","); //separa los campos

                Map<String, String> coche = new LinkedHashMap<>();
                coche.put(PRIMARY_KEY, datos[0].trim());
                coche.put(SECOND_KEY, datos[1].trim());
                coche.put(THIRD_KEY, datos[2].trim());

                int posAInsetar = calcularRegistros();
                try {
                    insertar(coche, posAInsetar);
                } catch (RegistroDuplicadoException e) {
                    System.out.println("La matricula " + datos[0] + " existe");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
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

    /**
     * Metodo encargado de insertar un registro en la posicion que el usuario desea.
     *
     * @param registroALeer el registro que el usuario desea insertar
     * @param posicion      la posicion en la que el usuario desea insertar el registro
     * @throws RegistroDuplicadoException si la matricula que el usuario desea insertar ya estaba anteriormente
     *
     */
    public void insertar(Map<String, String> registroALeer, int posicion)
            throws RegistroDuplicadoException, IOException {
        String valorMatricula = registroALeer.get(PRIMARY_KEY);

        if (registroExiste(valorMatricula)) {
            throw new RegistroDuplicadoException("Error: el registro ya existe");
        }
        //si el usuario indica una posicion mayor pero hay hueco antes se mueve el puntero a la posicion libre mas cercana
        int totalPlazas = calcularRegistros();
        if (posicion > totalPlazas) {
            posicion = totalPlazas;
        }

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            try {
                ByteBuffer bf = ByteBuffer.wrap(pasarRegistroABytes(registroALeer)); //convierte un array de bytes en un Buffer
                moverRegistro(fl, posicion);//si la posicion estaba ocupada se mueve a la derecha y sus suguientes tambien
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
     * Metodo encargado de borrar un registro leyendo la matricula y accediendo a la posicion de esa
     *
     * @param matricula la matricula introducida por el usuario
     * @throws BorradoException  si la matricula no se encuentra
     * @throws PosicionException
     */
    public void borrarPorMatricula(String matricula) throws BorradoException, PosicionException {
        int pos = buscarPorMatricula(matricula);

        if (pos == -1) {
            throw new BorradoException("Error: La matricula no ha sido encontrada");
        }

        borrarPorPosicion(pos);
    }

    /**
     * Metodo encargado de modificar un registro guardando la matricula original para que esta no sea
     * cambiada
     *
     * @param registroActualizado los nuevos campos a actualizar
     * @param posicion            la posicion del registro que se desea actualizar
     * @throws PosicionException si la posicion no ha sido encontrada
     */
    public void modificar(Map<String, String> registroActualizado, int posicion) throws PosicionException {
        int totalPlazas = calcularRegistros();

        if (posicion >= totalPlazas || posicion < 0) {
            throw new PosicionException("La posicion no ha sido encontrda");
        }

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            int byteIncio = LONGITUD_TOTAL_REGISTRO_EN_BYTES * posicion;
            fl.position(byteIncio);//desplazamiento puntero

            //guardado de la maticula original
            ByteBuffer bf = ByteBuffer.allocate(MAX_BYTES_MATRICULA);
            fl.read(bf);

            String matriculaOriginal = new String(bf.array()).trim();

            registroActualizado.put(PRIMARY_KEY, matriculaOriginal);

            ByteBuffer bfnuevoNuevo = ByteBuffer.wrap(pasarRegistroABytes(registroActualizado));

            //se vuelve a mover el puntero al inicio
            fl.position(byteIncio);
            fl.write(bfnuevoNuevo);

        } catch (IOException | LimiteBytesException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * Metodo encargado de encontrar la posicion de un registro por su matricula
     *
     * @param matriculaABuscar la matricula que el usuario desea buscar
     * @return -1 no se ha encontrado,  la posicion en caso contrario
     */
    private int buscarPorMatricula(String matriculaABuscar) {
        int totalPlazas = calcularRegistros();

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ)) {
            ByteBuffer bf = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);


            for (int i = 0; i < totalPlazas; i++) {
                fl.read(bf);
                byte[] arrayBytes = bf.array();

                //se verifica si la plaza esta vacia
                if (arrayBytes[0] == 0) {
                    bf.clear();
                    continue;
                }
                int desplazamientoCampo = 0;
                String valorClaveLeida = null;
                for (Map.Entry<String, Integer> campo : this.campos.entrySet()) {
                    if (campo.getKey().equals(PRIMARY_KEY)) {
                        valorClaveLeida = extraerStringDeBytes(arrayBytes, desplazamientoCampo, campo.getValue());
                        break;
                    }
                    //se suma el tamaño del campo actual para saber en que byte exacto empieza el siguiente
                    desplazamientoCampo += campo.getValue();
                }

                if (valorClaveLeida != null && valorClaveLeida.equals(matriculaABuscar)) {
                    return i;
                }
                bf.clear();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return -1; //no ha encontrado la posicion
    }

    /**
     * Metodo encargado de borrar un registro moviendo el posterior a la posicion del borrado y lo mismo con los
     * posteriores una posicion menos
     *
     * @param posicion la posicion del coche que se desea eliminar
     * @throws PosicionException si la posicion es mayor que los registros que hay o si es menor que la primera por donde
     *                           se empieza
     */
    public void borrarPorPosicion(int posicion) throws PosicionException {
        int totalPlazas = calcularRegistros();
        if (posicion >= calcularRegistros() || posicion < 0) {
            throw new PosicionException("Error: posicion no encontrada");
        }

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer br = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);

            for (int i = posicion + 1; i < totalPlazas; i++) {
                br.clear();

                //lectura coche derecha
                fl.position(i * LONGITUD_TOTAL_REGISTRO_EN_BYTES);
                fl.read(br);

                br.flip();

                //desplazamiento a la posicion izquierda
                fl.position((i - 1) * LONGITUD_TOTAL_REGISTRO_EN_BYTES);
                fl.write(br);
            }

            int longitdArchivo = ((totalPlazas - 1) * LONGITUD_TOTAL_REGISTRO_EN_BYTES);
            //los ultimos 71 bytes se borran del archivo porque son los del coche duplicado
            fl.truncate(longitdArchivo);

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
        long totalRegistros = calcularRegistros();
        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ)) {
            ByteBuffer bf = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);

            for (int i = 0; i < totalRegistros; i++) {
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
        int totalPlazas = calcularRegistros();

        if (posicionDeseada >= totalPlazas) { //no hay registro que desplazar, se acaba el metodo
            return;
        }

        ByteBuffer br = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);

        for (int i = totalPlazas - 1; i >= posicionDeseada; i--) {
            br.clear();


            fl.position(i * LONGITUD_TOTAL_REGISTRO_EN_BYTES); //acceso a la posicion y lectura del coche
            fl.read(br);
            br.flip(); //cambio de lectura a escritura

            fl.position((i + 1) * LONGITUD_TOTAL_REGISTRO_EN_BYTES); //desplazamiento
            fl.write(br);
        }
    }

    /**
     * Metodo encargado de calcular la cantidad de registros que hay en la base de datos
     *
     * @return el numero de registros que hay
     */
    private int calcularRegistros() {
        int registros = 0;

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ)) {
            registros = (int) (fl.size() / LONGITUD_TOTAL_REGISTRO_EN_BYTES);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return registros;
    }


    /**
     * Metodo encargado de extraer texto desde una posicion de un array de bytes
     *
     * @param buffer         el array de bytes
     * @param desplazamiento la posicion del array donde se emoiez< a leer
     * @param longitud       la longitd de bytes del campo a extraer
     * @return el texto
     */
    private String extraerStringDeBytes(byte[] buffer, int desplazamiento, int longitud) {
        String texto = new String(buffer, desplazamiento, longitud, StandardCharsets.UTF_8);
        return texto.trim(); //los bytes que sean espacios en blanco los quita
    }

    /**
     * Transforma un registro en un Map
     *
     * @param lecturaRegistro
     * @return
     */
    private Map<String, String> extraerRegistroAMapa(byte[] lecturaRegistro) {
        Map<String, String> registro = new LinkedHashMap<>();
        int desplazamientoCampo = 0;

        for (Map.Entry<String, Integer> campo : this.campos.entrySet()) {
            String valor = extraerStringDeBytes(lecturaRegistro, desplazamientoCampo, campo.getValue());
            registro.put(campo.getKey(), valor);
            desplazamientoCampo += campo.getValue();
        }

        return registro;
    }

    public void ordenarPorMatricula() throws OrdenarException {
        int totalPlazas = calcularRegistros();

        if (totalPlazas <= 1) {
            throw new OrdenarException("no hay suficiente registros para ordenar");
        }

        List<Map<String, String>> listaCoches = new ArrayList<>();


        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.READ)) {
            ByteBuffer bf = ByteBuffer.allocate(LONGITUD_TOTAL_REGISTRO_EN_BYTES);

            for (int i = 0; i < totalPlazas; i++) {
                fl.read(bf);

                if (bf.array()[0] != 0) {
                    Map<String, String> coche = extraerRegistroAMapa(bf.array());
                    listaCoches.add(coche);
                }
                bf.clear();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return; // Abortamos para no destruir datos
        }

        listaCoches.sort(Comparator.comparing(c -> c.get(PRIMARY_KEY)));

        try (FileChannel fl = FileChannel.open(RUTA_FICHERO_CARGA_BBDD, StandardOpenOption.WRITE)) {
            fl.position(0); //desplazamiento del puntero a la posicion inicial

            for (Map<String, String> coche : listaCoches) {
                ByteBuffer bf = ByteBuffer.wrap(pasarRegistroABytes(coche));
                fl.write(bf);//escritura ordenada
            }

        } catch (IOException | LimiteBytesException e) {
            System.out.println(e.getMessage());
        }
    }

    static void main() {
        GestorBBDD gb = new GestorBBDD();

        Map<String, String> coche = new LinkedHashMap<>(Map.of("Matricula", "5678def", "Marca", "seat", "Modelo", "ibiza"));
        Map<String, String> coche2 = new LinkedHashMap<>(Map.of("Matricula", "1234abc", "Marca", "opel", "Modelo", "corsa"));
    }
}