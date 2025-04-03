import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

public class MemoriaVirtualNRU {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Menu:");
        System.out.println("1. Generar referencias");
        System.out.println("2. Simular paginación (NRU)");
        int opcion = sc.nextInt();
        sc.nextLine();
        if (opcion == 1) {
            System.out.print("Ingrese el tamaño de página (en bytes): ");
            int pageSize = sc.nextInt();
            sc.nextLine();
            System.out.print("Ingrese el nombre del archivo BMP: ");
            String bmpFile = sc.nextLine();
            GeneradorReferencias.generar(bmpFile, pageSize);
        } else if (opcion == 2) {
          
            System.out.print("Ingrese el tamaño de página (en bytes): ");
            int tp = sc.nextInt();
            System.out.print("Ingrese el número de marcos de página: ");
            int marcos = sc.nextInt();
            sc.nextLine();
            System.out.print("Ingrese el archivo de referencias: ");
            String archivoRef = sc.nextLine();
            MemoriaVirtual mv = new MemoriaVirtual(tp, marcos);
            mv.simularPaginacion(archivoRef);
        } else {
            System.out.println("Opción no válida.");
        }
    }
}
