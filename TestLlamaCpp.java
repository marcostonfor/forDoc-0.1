import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TestLlamaCpp {

    public static void main(String[] args) {
        String prompt = "";

        // Leer prompt desde stdin (enviado por PHP)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            prompt = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            System.err.println("Error al leer el prompt desde stdin: " + e.getMessage());
            System.exit(1);
        }

        if (prompt.trim().isEmpty()) {
            System.err.println("Prompt vacío recibido. Nada que procesar.");
            System.exit(1);
        }

        long timeoutSegundos = 180;
        String respuesta = procesarConLlamaCpp(prompt, timeoutSegundos);

        System.out.println(respuesta); // Esto es lo que PHP recibirá
    }

    private static String procesarConLlamaCpp(String promptCompleto, long timeoutSegundos) {
        String rutaLlamaCpp = "/home/marcos/llama.cpp/build/bin/llama-cli";
        String rutaModelo = "/home/marcos/llama.cpp/models/stable-code-3b.Q2_K.gguf";
        int nTokens = 128;

        List<String> command = new ArrayList<>();
        command.add(rutaLlamaCpp);
        command.add("-m");
        command.add(rutaModelo);
        command.add("-t");
        command.add("3");
        command.add("-n");
        command.add(String.valueOf(nTokens));

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("prompt-", ".txt");
            Files.write(tempFile, (promptCompleto + "\n").getBytes(StandardCharsets.UTF_8));

            command.add("-f");
            command.add(tempFile.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Combina stdout + stderr

            Process proceso = pb.start();

            StringBuilder salida = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    salida.append(linea).append("\n");
                }
            }

            boolean terminado = proceso.waitFor(timeoutSegundos, TimeUnit.SECONDS);
            if (!terminado) {
                proceso.destroyForcibly();
                return "**Timeout:** El modelo tardó más de " + timeoutSegundos + " segundos.";
            }

            int exitCode = proceso.exitValue();
            if (exitCode != 0) {
                return "**Error:** llama.cpp terminó con código " + exitCode;
            }

            String respuesta = salida.toString().trim();
            return respuesta.isEmpty()
                    ? "**Advertencia:** El modelo no generó salida."
                    : respuesta;

        } catch (IOException | InterruptedException e) {
            return "**Excepción:** " + e.getMessage();
        } finally {
            if (tempFile != null) {
                try {
                    Files.delete(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
