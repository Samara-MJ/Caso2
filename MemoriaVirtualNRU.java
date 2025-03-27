import java.io.*;
import java.util.*;
import GeneradorReferencias;
import Imagen;

public class MemoriaVirtualNRU {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Menu:\n1. Generar referencias\n2. Simular paginación (NRU)");
        int opcion = sc.nextInt();
        sc.nextLine();

        if (opcion == 1) {
            System.out.print("Ingrese el tamaño de página (en bytes): ");
            int pageSize = sc.nextInt();
            sc.nextLine();
            System.out.print("Ingrese el nombre del archivo BMP: ");
            String bmpFile = sc.nextLine();
            GeneradorReferencias.generar(bmpFile, pageSize);
        } else {
            System.out.println("Opción no implementada en esta versión.");
        }
    }
}
