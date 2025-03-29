import java.io.*;
import java.util.*;
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

class Imagen {
    byte[] header = new byte[54];
    byte[][][] imagen;
    int alto, ancho;
    int padding;
    
    public Imagen(String nombre) {
        try {
            FileInputStream fis = new FileInputStream(nombre);
            fis.read(header);
            ancho = ((header[21] & 0xFF) << 24) | ((header[20] & 0xFF) << 16) |
                    ((header[19] & 0xFF) << 8) | (header[18] & 0xFF);
            alto = ((header[25] & 0xFF) << 24) | ((header[24] & 0xFF) << 16) |
                   ((header[23] & 0xFF) << 8) | (header[22] & 0xFF);
            imagen = new byte[alto][ancho][3];
            int rowSize = ancho * 3;
            padding = (4 - (rowSize % 4)) % 4;
            byte[] pixel = new byte[3];
            for (int i = 0; i < alto; i++) {
                for (int j = 0; j < ancho; j++) {
                    fis.read(pixel);
                   
                    imagen[i][j][0] = pixel[2];
                    imagen[i][j][1] = pixel[1];
                    imagen[i][j][2] = pixel[0];
                }
                fis.skip(padding);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class GeneradorReferencias {
    public static void generar(String bmpFile, int pageSize) throws IOException {
        Imagen img = new Imagen(bmpFile);
        int NF = img.alto;
        int NC = img.ancho;
        
        int sizeImagen = NF * NC * 3;
        int sizeRespuesta = sizeImagen;
        int sizeFiltro = 3 * 3 * 4;
        
        int totalBytes = sizeImagen + 2 * sizeFiltro + sizeRespuesta;
        int NP = (int) Math.ceil((double) totalBytes / pageSize);
        
        List<String> referencias = new ArrayList<>();
        int refCount = 0;
        
        int baseImg = 0;
        int baseFiltroX = baseImg + sizeImagen;
        int baseFiltroY = baseFiltroX + sizeFiltro;
        int baseRespuesta = baseFiltroY + sizeFiltro;
        
        String[] colores = {"r", "g", "b"};
        
       
        for (int i = 1; i < NF - 1; i++) {
            for (int j = 1; j < NC - 1; j++) {
          
                for (int ki = -1; ki <= 1; ki++) {
                    for (int kj = -1; kj <= 1; kj++) {
                        int ii = i + ki;
                        int jj = j + kj;
                       
                        for (int color = 0; color < 3; color++) {
                            int offset = baseImg + ((ii * NC + jj) * 3 + color);
                            int page = offset / pageSize;
                            int disp = offset % pageSize;
                            referencias.add("Imagen[" + ii + "][" + jj + "]." + colores[color] + "," + page + "," + disp + ",R");
                            refCount++;
                        }
                   
                        int offsetX = baseFiltroX + (((ki + 1) * 3) + (kj + 1)) * 4;
                        int offsetY = baseFiltroY + (((ki + 1) * 3) + (kj + 1)) * 4;
                        int pageX = offsetX / pageSize;
                        int dispX = offsetX % pageSize;
                        int pageY = offsetY / pageSize;
                        int dispY = offsetY % pageSize;
                        referencias.add("SOBEL_X[" + (ki + 1) + "][" + (kj + 1) + "]," + pageX + "," + dispX + ",R");
                        referencias.add("SOBEL_X[" + (ki + 1) + "][" + (kj + 1) + "]," + pageX + "," + dispX + ",R");
                        referencias.add("SOBEL_X[" + (ki + 1) + "][" + (kj + 1) + "]," + pageX + "," + dispX + ",R");
                        referencias.add("SOBEL_Y[" + (ki + 1) + "][" + (kj + 1) + "]," + pageY + "," + dispY + ",R");
                        referencias.add("SOBEL_Y[" + (ki + 1) + "][" + (kj + 1) + "]," + pageY + "," + dispY + ",R");
                        referencias.add("SOBEL_Y[" + (ki + 1) + "][" + (kj + 1) + "]," + pageY + "," + dispY + ",R");
                        refCount += 6;
                    }
                }
                
                for (int color = 0; color < 3; color++) {
                    int offset = baseRespuesta + ((i * NC + j) * 3 + color);
                    int page = offset / pageSize;
                    int disp = offset % pageSize;
                    referencias.add("Rta[" + i + "][" + j + "]." + colores[color] + "," + page + "," + disp + ",W");
                    refCount++;
                }
            }
        }
        
        PrintWriter out = new PrintWriter("referencias_generadas.txt");
        out.println("TP=" + pageSize);
        out.println("NF=" + NF);
        out.println("NC=" + NC);
        out.println("NR=" + refCount);
        out.println("NP=" + NP);
        for (String ref : referencias) {
            out.println(ref);
        }
        out.close();
        
        System.out.println("Archivo de referencias generado: referencias_generadas.txt");
    }
}

class MemoriaVirtual {
    private int tamanioPagina;
    private int marcosAsignados;
    private List<Integer> memoriaRAM;
    private Map<Integer, Integer> tablaPaginas;
    private Set<Integer> usadasRecientemente;
    private int hits = 0, fallos = 0, totalRefs = 0;
    private final Object lock = new Object();
    private AtomicBoolean finished = new AtomicBoolean(false);
    
    public MemoriaVirtual(int tamanioPagina, int marcosAsignados) {
        this.tamanioPagina = tamanioPagina;
        this.marcosAsignados = marcosAsignados;
        memoriaRAM = new ArrayList<>();
        tablaPaginas = new HashMap<>();
        usadasRecientemente = new HashSet<>();
    }
    
    public void simularPaginacion(String archivoRef) throws IOException, InterruptedException {
        Thread refThread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new FileReader(archivoRef))) {
                String line;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("TP=") || line.startsWith("NF=") ||
                        line.startsWith("NC=") || line.startsWith("NR=") ||
                        line.startsWith("NP=")) {
                        continue;
                    }
                    String[] parts = line.split(",");
                    int pagina = Integer.parseInt(parts[1]);
                    procesarReferencia(pagina);
                    totalRefs++;
                    count++;
                    if (count % 10000 == 0) {
                        try {
                            Thread.sleep(1); //  espera 1 ms
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                finished.set(true);
            }
        });
        
        Thread updateThread = new Thread(() -> {
            while (!finished.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                actualizarEstado();
            }
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                actualizarEstado();
            }
        });
        
        refThread.start();
        updateThread.start();
        refThread.join();
        updateThread.join();
        imprimirResultados();
    }
    
    public void procesarReferencia(int pagina) {
        synchronized(lock) {
            if (tablaPaginas.containsKey(pagina)) {
                hits++;
                usadasRecientemente.add(pagina);
            } else {
                fallos++;
                if (memoriaRAM.size() >= marcosAsignados) {
                    int i = 0;
                    while (i < memoriaRAM.size() && usadasRecientemente.contains(memoriaRAM.get(i))) {
                        i++;
                    }
                    if (i < memoriaRAM.size()) {
                        int pag = memoriaRAM.get(i);
                        memoriaRAM.remove(i);
                        tablaPaginas.remove(pag);
                    } else {
                        int pag = memoriaRAM.remove(0);
                        tablaPaginas.remove(pag);
                    }
                    usadasRecientemente.clear();
                }
                memoriaRAM.add(pagina);
                tablaPaginas.put(pagina, memoriaRAM.size() - 1);
            }
        }
    }
    
    private void actualizarEstado() {
        synchronized(lock) {
            usadasRecientemente.clear();
        }
    }
    
    public void imprimirResultados() {
        synchronized(lock) {
            System.out.println("Hits: " + hits);
            System.out.println("Fallos: " + fallos);
            System.out.println("Total de referencias: " + totalRefs);
            double porcentajeHits = (totalRefs > 0) ? ((double)hits / totalRefs * 100) : 0;
            System.out.printf("Porcentaje de hits: %.2f%%\n", porcentajeHits);
        }
    }
}
