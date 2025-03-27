import java.io.FileInputStream;
import java.io.IOException;

public class Imagen {
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
