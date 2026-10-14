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

    //menu hecho con Gemini
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

    private static void programa(GestorBBDD gb) {
        boolean ejecutando = true;

        while (ejecutando) {
            menu();
            int opcion = MiEntradaSalida.leerEnteroEnRango("Elige una opcion:", 1, 7);

            switch (opcion) {
                case 1 -> gb.cargarFicheroCSV();
                case 2 -> {
                    try {
                        gb.insertar(solicitarCoche(), solicitarPosicion() - 1);//le resto 1 a la posicion, porque en programacion se empieza desde la pos 0
                    } catch (RegistroDuplicadoException | IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
                //case 3 ->
                //case 4 ->
                //case 5 ->
                //case 6 ->
                case 7 -> ejecutando = false;
            }
        }
    }

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

    private static int solicitarPosicion() {
        return MiEntradaSalida.leerEnteroPositivoMayorQueCero("Introduce una posicion:\n");
    }

}
