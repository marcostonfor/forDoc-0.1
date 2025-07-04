import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

public class PromptProcessor {

    public static void main(String[] args) {
        String prompt = "";
        String rutaCarpeta;
        int lineasPorFragmento = 20; // Valor por defecto
        long timeoutSegundos = 360; // Valor por defecto (6 minutos)

        // Leer el prompt desde stdin
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            prompt = reader.lines().collect(Collectors.joining("\n"));
            System.err.println("[DEBUG] Prompt recibido: " + prompt);
        } catch (IOException e) {
            System.err.println("Error al leer el prompt: " + e.getMessage());
            System.exit(1);
        }

        // Obtener argumentos de la línea de comandos
        if (args.length > 0) {
            rutaCarpeta = args[0];
        } else {
            rutaCarpeta = null; // No se proporcionó ruta
        }
        if (args.length > 1) {
            try {
                lineasPorFragmento = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) { /* usa el valor por defecto */ }
        }
        if (args.length > 2) {
            try {
                timeoutSegundos = Long.parseLong(args[2]);
            } catch (NumberFormatException e) { /* usa el valor por defecto */ }
        }

        System.err.println("[DEBUG] Ruta de carpeta: " + (rutaCarpeta != null ? rutaCarpeta : "Ninguna"));

        String respuesta;
        if (rutaCarpeta != null) {
            Path path = Paths.get(rutaCarpeta);
            if (Files.isDirectory(path)) {
                // Lógica anterior: procesar una carpeta entera
                respuesta = procesarCarpeta(prompt, rutaCarpeta, lineasPorFragmento, timeoutSegundos);
            } else if (Files.isRegularFile(path)) {
                // Nueva lógica: procesar un único archivo
                respuesta = procesarUnicoArchivo(prompt, rutaCarpeta, lineasPorFragmento, timeoutSegundos);
            } else {
                respuesta = "# Error: La ruta proporcionada no es un archivo ni un directorio válido.";
            }
        } else {
            // Leer contenido de prompt.txt y combinar con el prompt del usuario
            String contenidoPromptTxt = leerPromptTxt();
            System.err.println("[DEBUG] Contenido de prompt.txt: " + contenidoPromptTxt);
            if (!contenidoPromptTxt.isEmpty()) {
                prompt = contenidoPromptTxt + "\n\n" + prompt; // Combinar prompts
                System.err.println("[DEBUG] Prompt combinado: " + prompt);
            }

            respuesta = procesarConLlamaCpp(prompt, timeoutSegundos); // Procesar con llama.cpp
        }

        if (respuesta != null && !respuesta.isEmpty()) {
            System.err.println("[DEBUG] Respuesta generada (antes de imprimir): " + respuesta);
            System.out.println(respuesta);
            System.exit(0);
        } else {
            System.err.println("Error: No se generó ninguna respuesta.");
            System.out.println("# Error: No se generó ninguna respuesta.");
            System.exit(1);
        }
    }

    private static String procesarCarpeta(String prompt, String rutaCarpeta, int lineasPorFragmento, long timeoutSegundos) {
        StringBuilder respuesta = new StringBuilder();
        System.err.println("[DEBUG] Iniciando análisis de la carpeta: " + rutaCarpeta);
        respuesta.append("# Análisis de la Carpeta `").append(rutaCarpeta).append("`\n\n");
        if (!prompt.isEmpty()) {
            respuesta.append("**Instrucción adicional del usuario:** `").append(prompt).append("`\n\n");
        }
        respuesta.append("---\n\n");

        try {
            Path directorio = Paths.get(rutaCarpeta);

            if (!Files.exists(directorio) || !Files.isDirectory(directorio)) {
                System.err.println("[DEBUG] Ruta inválida: " + rutaCarpeta);
                return "# Error: La ruta especificada no es un directorio válido.";
            }

            String promptBase = leerPromptTxt();

            Files.walkFileTree(directorio, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path archivo, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile()) {
                        String nombreArchivo = archivo.getFileName().toString();
                        String extension = getExtension(nombreArchivo);

                        if (esArchivoTexto(extension)) {
                            try {
                                String contenidoArchivo = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
                                List<String> fragmentos = dividirEnFragmentos(contenidoArchivo, lineasPorFragmento);

                                respuesta.append("### Análisis del archivo: `").append(nombreArchivo).append("` (").append(fragmentos.size()).append(" fragmentos)\n\n");

                                for (int i = 0; i < fragmentos.size(); i++) {
                                    String fragmento = fragmentos.get(i);
                                    System.err.println("[DEBUG] Analizando fragmento " + (i + 1) + "/" + fragmentos.size() + " del archivo " + nombreArchivo);
                                    respuesta.append("#### Análisis del Fragmento ").append(i + 1).append(" de ").append(fragmentos.size()).append("\n\n");

                                    // Construir el prompt específico para este fragmento
                                    StringBuilder promptParaAnalisis = new StringBuilder();
                                    promptParaAnalisis.append(promptBase);
                                    if (!prompt.isEmpty()) {
                                        promptParaAnalisis.append("\n\nInstrucción adicional del usuario: ").append(prompt);
                                    }
                                    promptParaAnalisis.append("\n\n---\n\n");
                                    promptParaAnalisis.append("Analiza y documenta el siguiente fragmento de código del archivo `").append(nombreArchivo).append("`:\n\n");
                                    promptParaAnalisis.append("```").append(extension).append("\n");
                                    promptParaAnalisis.append(fragmento);
                                    promptParaAnalisis.append("\n```");

                                    String analisis = procesarConLlamaCpp(promptParaAnalisis.toString(), timeoutSegundos);
                                    respuesta.append(analisis).append("\n\n");

                                    if (analisis.contains("**Error")) {
                                        return FileVisitResult.TERMINATE;
                                    }
                                }

                            } catch (IOException e) {
                                respuesta.append("**Error al leer el archivo `").append(nombreArchivo).append("`:** ").append(e.getMessage()).append("\n\n---\n\n");
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                } 
            });

        } catch (IOException e) {
            return "# Error al procesar la carpeta: " + e.getMessage();
        }

        respuesta.append("\n\n---\n\n**Análisis completado.**");

        return respuesta.toString();
    }

    private static String procesarUnicoArchivo(String prompt, String rutaArchivo, int lineasPorFragmento, long timeoutSegundos) {
        StringBuilder respuesta = new StringBuilder();
        Path archivo = Paths.get(rutaArchivo);
        String nombreArchivo = archivo.getFileName().toString();
        String extension = getExtension(nombreArchivo);
        String promptBase = leerPromptTxt();

        System.err.println("[DEBUG] Iniciando análisis del archivo único: " + rutaArchivo);

        if (esArchivoTexto(extension)) {
            try {
                String contenidoArchivo = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
                List<String> fragmentos = dividirEnFragmentos(contenidoArchivo, lineasPorFragmento);

                respuesta.append("### Análisis del archivo: `").append(nombreArchivo).append("` (").append(fragmentos.size()).append(" fragmentos)\n\n");

                for (int i = 0; i < fragmentos.size(); i++) {
                    String fragmento = fragmentos.get(i);
                    System.err.println("[DEBUG] Analizando fragmento " + (i + 1) + "/" + fragmentos.size() + " del archivo " + nombreArchivo + " (" + fragmento.length() + " caracteres de código)");
                    respuesta.append("#### Análisis del Fragmento ").append(i + 1).append(" de ").append(fragmentos.size()).append("\n\n");

                    // Construir el prompt específico para este fragmento
                    StringBuilder promptParaAnalisis = new StringBuilder();
                    promptParaAnalisis.append(promptBase);
                    if (!prompt.isEmpty()) {
                        promptParaAnalisis.append("\n\nInstrucción adicional del usuario: ").append(prompt);
                    }
                    promptParaAnalisis.append("\n\n---\n\n");
                    promptParaAnalisis.append("Analiza y documenta el siguiente fragmento de código del archivo `").append(nombreArchivo).append("`:\n\n");
                    promptParaAnalisis.append("```").append(extension).append("\n");
                    promptParaAnalisis.append(fragmento);
                    promptParaAnalisis.append("\n```");

                    String analisis = procesarConLlamaCpp(promptParaAnalisis.toString(), timeoutSegundos);
                    respuesta.append(analisis).append("\n\n");

                    if (analisis.contains("**Error")) {
                        // Si un fragmento falla, dejamos de procesar este archivo.
                        break; 
                    }
                }
            } catch (IOException e) {
                respuesta.append("**Error al leer el archivo `").append(nombreArchivo).append("`:** ").append(e.getMessage()).append("\n\n");
            }
        } else {
            respuesta.append("El archivo `").append(nombreArchivo).append("` no es un archivo de texto soportado para el análisis.");
        }
        return respuesta.toString();
    }

    private static String getExtension(String nombreArchivo) {
        if (nombreArchivo == null) return "";
        int i = nombreArchivo.lastIndexOf('.');
        return i > 0 ? nombreArchivo.substring(i + 1) : "";
    }

    private static List<String> dividirEnFragmentos(String codigo, int lineasPorFragmento) {
        List<String> fragmentos = new ArrayList<>();
        String[] lineas = codigo.split("\r\n|\r|\n");
        StringBuilder fragmentoActual = new StringBuilder();
        for (int i = 0; i < lineas.length; i++) {
            fragmentoActual.append(lineas[i]).append("\n");
            if ((i + 1) % lineasPorFragmento == 0 || i == lineas.length - 1) {
                if (fragmentoActual.length() > 0) {
                    fragmentos.add(fragmentoActual.toString());
                    fragmentoActual = new StringBuilder();
                }
            }
        }
        return fragmentos;
    }

    private static String leerPromptTxt() {
        try (Stream<String> lines = Files.lines(Paths.get("prompt.txt"), StandardCharsets.UTF_8)) {
            return lines.collect(Collectors.joining("\n"));
        } catch (IOException e) {
            System.err.println("Error al leer prompt.txt: " + e.getMessage());
            return "";
        }
    }

    private static String procesarConLlamaCpp(String promptCompleto, long timeoutSegundos) {
        System.err.println("[DEBUG] Llamando a Llama.cpp con un prompt de " + promptCompleto.length() + " caracteres.");

        // --- CONFIGURACIÓN OBLIGATORIA ---
        // El error "No such file or directory" o "failed to open GGUF file" significa que estas rutas son incorrectas.
        // Por favor, verifica que estas rutas son las correctas en tu sistema.
        String rutaLlamaCpp = "/home/marcos/llama.cpp/build/bin/llama-cli"; // <-- ¡PON AQUÍ TU RUTA REAL!
        String rutaModelo = "/home/marcos/llama.cpp/models/stable-code-3b.Q2_K.gguf";   // <-- ¡PON AQUÍ TU RUTA REAL!

        ProcessBuilder pb = new ProcessBuilder(
                rutaLlamaCpp,
                "-m", rutaModelo,
                "-t", "4",
                "-n", "1024"
        );

        Path tempFile = null;
        try {
            // SOLUCIÓN DEFINITIVA: Escribimos el prompt en un archivo temporal
            // y le pasamos la ruta con el flag -f. Esto fuerza el modo no-interactivo.
            tempFile = Files.createTempFile("prompt-", ".txt");
            Files.write(tempFile, promptCompleto.getBytes(StandardCharsets.UTF_8));

            // Modificamos la lista de comandos para añadir el flag -f
            List<String> commandList = new ArrayList<>(pb.command());
            commandList.add("-f");
            commandList.add(tempFile.toAbsolutePath().toString());
            pb.command(commandList);

            Process proceso = pb.start();

            // Consumir los streams de salida y error en hilos separados para evitar bloqueos.
            StringBuilder outputGobbler = new StringBuilder();
            StringBuilder errorGobbler = new StringBuilder();

            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines().forEach(line -> outputGobbler.append(line).append("\n"));
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            });
            outputReader.start();

            Thread errorReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getErrorStream(), StandardCharsets.UTF_8))) {
                    reader.lines().forEach(line -> errorGobbler.append(line).append("\n"));
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            });
            errorReader.start();

            // Usamos el timeout configurable
            if (!proceso.waitFor(timeoutSegundos, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                System.err.println("[ERROR] Timeout: Llama.cpp tardó más de " + timeoutSegundos + " segundos en procesar un fragmento.");
                return "**Error de Timeout:** El análisis de un fragmento tardó demasiado (más de " + timeoutSegundos + "s) y fue cancelado. El hardware puede ser un factor.";
            }

            outputReader.join();
            errorReader.join();

            int codigoSalida = proceso.exitValue();
            String errorOutput = errorGobbler.toString();
            String respuestaModelo = outputGobbler.toString();

            if (!errorOutput.isEmpty()) {
                System.err.println("[ERROR] Salida de error de Llama.cpp:\n" + errorOutput);
            }

            if (codigoSalida == 0) {
                // LÓGICA CORREGIDA Y ROBUSTA: Buscamos el final del prompt que enviamos
                // para separar de forma fiable la entrada de la salida real del modelo.
                int promptEndIndex = respuestaModelo.indexOf(promptCompleto);
                if (promptEndIndex != -1) {
                    // La respuesta real del modelo empieza justo después del prompt que se repite.
                    respuestaModelo = respuestaModelo.substring(promptEndIndex + promptCompleto.length());
                }
                return respuestaModelo.trim().isEmpty() ? "**Análisis del modelo (ADVERTENCIA):** El modelo no generó ninguna respuesta." : respuestaModelo;
            } else {
                return "**Error al ejecutar el análisis:** Llama.cpp falló con el código " + codigoSalida + ". Salida de error: " + errorOutput;
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR] Excepción al ejecutar Llama.cpp: " + e.getMessage());
            e.printStackTrace(System.err);
            return "**Error crítico:** No se pudo ejecutar el proceso de análisis. " + e.getMessage();
        } finally {
            if (tempFile != null) {
                try { Files.delete(tempFile); } catch (IOException e) { e.printStackTrace(System.err); }
            }
        }
    }

    private static boolean esArchivoTexto(String extension) {
        String[] extensionesTexto = {"java", "php", "txt", "md", "js", "css", "html", "xml", "json", "py", "sh", "cpp", "c", "h"};
        for (String ext : extensionesTexto) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}