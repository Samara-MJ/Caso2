import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoriaVirtual {
    private int tamanioPagina;
    private int marcosAsignados;
    private List<Integer> memoriaRAM;
    private Map<Integer, Integer> tablaPaginas;
    private Set<Integer> usadasRecientemente = ConcurrentHashMap.newKeySet();
    private AtomicInteger hits = new AtomicInteger(0);
    private AtomicInteger fallos = new AtomicInteger(0);
    private AtomicInteger totalRefs = new AtomicInteger(0);
    private final Object lock = new Object();
    private AtomicBoolean finished = new AtomicBoolean(false);

    public MemoriaVirtual(int tamanioPagina, int marcosAsignados) {
        this.tamanioPagina = tamanioPagina;
        this.marcosAsignados = marcosAsignados;
        memoriaRAM = new ArrayList<>();
        tablaPaginas = new HashMap<>();
        usadasRecientemente = ConcurrentHashMap.newKeySet();
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
                    count++;
                    if (count % 10000 == 0) {
                        try {
                            Thread.sleep(1);
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
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    actualizarEstado();
                }
            }
            
        });

        refThread.start();
        updateThread.start();
        refThread.join();
        updateThread.join();
        imprimirResultados();
    }

    public void procesarReferencia(int pagina) {
        synchronized (lock) {
            totalRefs.incrementAndGet();
            if (tablaPaginas.containsKey(pagina)) {
                hits.incrementAndGet();
                usadasRecientemente.add(pagina);
            } else {
                fallos.incrementAndGet();
                if (memoriaRAM.size() >= marcosAsignados) {
                    int i = 0;
                    while (i < memoriaRAM.size() && usadasRecientemente.contains(memoriaRAM.get(i))) {
                        i++;
                    }
                    
                    int pag = (i < memoriaRAM.size()) ? memoriaRAM.remove(i) : memoriaRAM.remove(0);
                    tablaPaginas.remove(pag);
                    usadasRecientemente.remove(pag); // Remover la página reemplazada de usadasRecientemente
                }
                memoriaRAM.add(pagina);
                tablaPaginas.put(pagina, memoriaRAM.size() - 1);
            }
        }
    }

    private void actualizarEstado() {
        synchronized (lock) {
            Set<Integer> copia = new HashSet<>(usadasRecientemente);
            usadasRecientemente.clear();
        }
    }
    

    public void imprimirResultados() {
        System.out.println("Hits: " + hits.get());
        System.out.println("Fallos: " + fallos.get());
        System.out.println("Total de referencias: " + totalRefs.get());
        double porcentajeHits = (totalRefs.get() > 0) ? ((double) hits.get() / totalRefs.get() * 100) : 0;
        System.out.printf("Porcentaje de hits: %.2f%%\n", porcentajeHits);

    }

}
