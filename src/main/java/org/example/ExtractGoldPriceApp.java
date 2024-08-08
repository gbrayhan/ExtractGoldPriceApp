package org.example;

import java.awt.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

public class ExtractGoldPriceApp {

    public static void main(String[] args) {
        // Abre el navegador con la URL especificada
        openBrowser("https://es.investing.com/commodities/gold?utm_source=google&utm_medium=cpc&utm_campaign=20676594895&utm_content=&utm_term=_&GL_Ad_ID=&GL_Campaign_ID=20676594895&show_see_in=2&af_adset_id=&ttw=2&gad_source=1&gclid=CjwKCAjw2dG1BhB4EiwA998cqL9iqJdjjNrDzwlW0Qot1b6FKQ2XuzfvmW_PkcxCHItmNPT6nDXKExoCZvQQAvD_BwE");

        // Espera un poco para que el navegador abra la página
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Toma una captura de pantalla y convierte la imagen a texto
        String screenshotPath = takeScreenshot();
        if (screenshotPath != null) {
            String extractedText = convertImageToText(screenshotPath);
            System.out.println("Texto extraído de la imagen: " + extractedText);
        }

    }

    private static void moveCursorRandomly() {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            int randomX = (int) (Math.random() * screenSize.getWidth());
            int randomY = (int) (Math.random() * screenSize.getHeight());

            Robot robot = new Robot();
            robot.mouseMove(randomX, randomY);

        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
            } else {
                throw new UnsupportedOperationException("Desktop is not supported");
            }
        } catch (IOException | UnsupportedOperationException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static String takeScreenshot() {
        try {
            Robot robot = new Robot();
            String folderPath = "capturas";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filePath = folderPath + "/captura_" + timestamp + ".png";
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenFullImage = robot.createScreenCapture(screenRect);
            ImageIO.write(screenFullImage, "png", new File(filePath));

            System.out.println("Captura de pantalla guardada: " + filePath);
            return filePath;
        } catch (AWTException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertImageToText(String imagePath) {
        try {
            String timestamp = imagePath.substring(imagePath.lastIndexOf("_") + 1, imagePath.lastIndexOf("."));
            String outputTextFile = "salidacaptura_" + timestamp + ".txt";

            // Comando para ejecutar Tesseract en la consola
            ProcessBuilder pb = new ProcessBuilder("tesseract", imagePath, "salidacaptura_" + timestamp);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            // Leer el archivo de salida generado por Tesseract
            File outputFile = new File(outputTextFile);
            StringBuilder text = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Elimina los espacios y las líneas vacías
                    line = line.replace(" ", "");
                    if (!line.isEmpty()) {
                        text.append(line).append("\n"); // Agrega la línea si no está vacía
                    }
                }
            }

            // Reescribir el archivo con el texto limpio
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
                bw.write(text.toString());
            }

            // Extraer la información específica
            return extractRangoDiaValue(text.toString());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String extractRangoDiaValue(String text) {
        String target = "Rangodia";
        int startIndex = text.indexOf(target);

        if (startIndex != -1) {
            // Avanza más allá de "Rango dia"
            startIndex += target.length();

            // Encuentra el primer símbolo de "+" o "-" después de "Rango dia"
            int plusIndex = text.indexOf("+", startIndex);
            int minusIndex = text.indexOf("-", startIndex);

            // Tomar el índice que ocurra primero y sea válido
            int endIndex;
            if (plusIndex != -1 && minusIndex != -1) {
                endIndex = Math.min(plusIndex, minusIndex);
            } else if (plusIndex != -1) {
                endIndex = plusIndex;
            } else if (minusIndex != -1) {
                endIndex = minusIndex;
            } else {
                return "No se encontró el símbolo + o -";
            }

            // Extrae el texto entre "Rango dia" y el primer símbolo de "+" o "-"
            String rangoDiaValue = text.substring(startIndex, endIndex).trim();

            System.out.println("Texto extraído: " + rangoDiaValue);
            // Buscar el primer número en esa subcadena que puede incluir comas y puntos decimales
            String[] parts = rangoDiaValue.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d{1,3}(\\.\\d{3})*(,\\d+)?")) { // Coincide con números en formato como 2.465,55 o 1,234.56
                    return part;
                }
            }
        }

        return "No se encontró el valor de Rango dia";
    }
}
