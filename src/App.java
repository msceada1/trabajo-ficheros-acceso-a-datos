import exceptions.BorradoException;
import exceptions.OrdenarException;
import exceptions.PosicionException;
import exceptions.RegistroDuplicadoException;
import utils.MiEntradaSalida;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class App {

    private static final String PRIMARY_KEY = "Matricula";
    private static final String SECOND_KEY = "Marca";
    private static final String THIRD_KEY = "Modelo";

    static void main() {
        GestorBBDD gb = new GestorBBDD();
        programa(gb);
    }

    //menu hecho con Gemini para que el usuario vea las opciones a realizar
    private static void menu() {
        System.out.println("\n=========================================");
        System.out.println("   GESTOR DE BASE DE DATOS DE COCHES");
        System.out.println("=========================================");
        System.out.println("1. Cargar fichero CSV");
        System.out.println("2. Insertar un coche nuevo");
        System.out.println("3. Modificar coche (por posición)");
        System.out.println("4. Borrar coche (por matrícula)");
        System.out.println("5. Borrar coche (por posición)");
        System.out.println("6. Ordenar registros por matrícula");
        System.out.println("7. Salir");
        System.out.println("=========================================");
    }

    /**
     * metodo encargado de hacer la conexion entre frontend y backend
     *
     * @param gb la base de datos donde se van a guardar las modificaciones del usuario
     */
    private static void programa(GestorBBDD gb) {
        boolean ejecutando = true;

        while (ejecutando) {
            menu();
            int opcion = MiEntradaSalida.leerEnteroEnRango("Elige una opcion:", 1, 7);

            switch (opcion) {
                case 1 -> gb.cargarFicheroCSV();
                case 2 -> {
                    try {
                        gb.insertar(solicitarCoche(), solicitarPosicion());
                    } catch (RegistroDuplicadoException | IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.println("Cantidad de resgistros actuales:\n" + gb.calcularRegistros());
                    try {
                        gb.modificar(modificar(), solicitarPosicion());
                    } catch (PosicionException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 4 -> {
                    try {
                        gb.borrarPorMatricula(solictarMaticula());
                    } catch (BorradoException | PosicionException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.println("Cantidad de resgistros actuales:\n" + gb.calcularRegistros());
                    try {
                        gb.borrarPorPosicion(solicitarPosicion());
                    } catch (PosicionException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 6 -> {
                    try {
                        gb.ordenarPorMatricula();
                    } catch (OrdenarException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case 7 -> ejecutando = false;
            }
        }
    }

    /**
     * Metodo encargado de devolver el registro de un coche interactuando con el usuario y pidiendo los datos
     *
     * @return el coche a insertar
     */
    private static Map<String, String> solicitarCoche() {
        String matricula = MiEntradaSalida.leerCadena("Indica la matricula:\n");
        String marca = MiEntradaSalida.leerCadena("Indica la marca:\n");
        String modelo = MiEntradaSalida.leerCadena("Indica el modelo:\n");

        Map<String, String> coche = new LinkedHashMap<>();
        coche.put(PRIMARY_KEY, matricula);
        coche.put(SECOND_KEY, marca);
        coche.put(THIRD_KEY, modelo);

        return coche;
    }

    /**
     * metodo encargado de solicitar la posicion al usuario y restarle 1 ya que en programacion se empieza desde la
     * posicion 0
     *
     * @return la posicion donde se va a llevar a cabo la operacion
     */
    private static int solicitarPosicion() {
        return MiEntradaSalida.leerEnteroPositivoMayorQueCero("Introduce una posicion (de 1 en adelante):\n") - 1;
    }

    /**
     * metodo encargado de solicitar los datos del coche a modificar excepto la matricula
     *
     * @return el coche con la marca y el modelo actualizados
     */
    private static Map<String, String> modificar() {
        String matricula = ""; //la matricula no cambia, no se le pide al usuario
        String marca = MiEntradaSalida.leerCadena("Indica la nueva marca:\n");
        String modelo = MiEntradaSalida.leerCadena("Indica el modelo nuevo:\n");

        Map<String, String> cocheActualizado = new LinkedHashMap<>();
        cocheActualizado.put(PRIMARY_KEY, matricula);
        cocheActualizado.put(SECOND_KEY, marca);
        cocheActualizado.put(THIRD_KEY, modelo);

        return cocheActualizado;
    }

    /**
     * metodo encargado de solicitar por consola la matricula al usuario
     *
     * @return la matricula
     */
    private static String solictarMaticula() {
        return MiEntradaSalida.leerCadena("Indica la matricula:\n");
    }

}
