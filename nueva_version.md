# ¡Vamos¡Vamos allá, Marc! Aquí allá, Marc! Aquí tienes una versión tienes una versión **mínima, depurada **mínima, depurada y con logs detallados** del método `procesarConLlama y con logs detallados** del método `procesarConLlamaCpp()` para que puedasCpp()` para que puedas probar si el problema es el modelo, el tamaño del prompt o el entorno.

---

## ✅ Versión reducida y robusta de `procesarConLlamaCpp`

```java probar si el problema es el modelo, el tamaño del prompt o el entorno.

---

## ✅ Versión reducida y robusta de `procesarConLlamaCpp`

```java
private static String procesarConLlamaCpp(String promptCompleto, long timeoutSegundos) {
    System.err.println("[DEBUG] Llamando a Llama.cpp
private static String procesarConLlamaCpp(String promptCompleto, long timeoutSegundos) {
    System.err.println("[DEBUG] Llamando a Llama.cpp con prompt de " + promptCompleto.length() + " caracteres");

    String ruta con prompt de " + promptCompleto.length() + " caracteres");

    String rutaLlamaCpp = "/home/marcos/llama.cpp/build/bin/llama-cli";
    String rutaModelo = "/home/marcos/llama.cpp/models/stableLlamaCpp = "/home/marcos/llama.cpp/build/bin/llama-cli";
    String rutaModelo = "/home/marcos/llama.cpp/models/stable-code-3b.Q2_K.gguf";

    // ⚠️ Reducción de tokens-code-3b.Q2_K.gguf";

    // ⚠️ Reducción de tokens para pruebas
    int nTokens = 128;

    ProcessBuilder pb = new ProcessBuilder(
        rutaLlamaCpp,
        "-m", rutaModelo para pruebas
    int nTokens = 128;

    ProcessBuilder pb = new ProcessBuilder(
        rutaLlamaCpp,
        "-m", rutaModelo,
        "-t", "3",
        "-n", String.valueOf(nTokens)
    );

    Path tempFile = null;
    try {
        // Escribir prompt en archivo temporal
        tempFile,
        "-t", "3",
        "-n", String.valueOf(nTokens)
    );

    Path tempFile = null;
    try {
        // Escribir prompt en archivo temporal
        tempFile = Files.createTemp = Files.createTempFile("prompt-", ".txt");
        Files.write(tempFile,File("prompt-", ".txt");
        Files.write(tempFile, (promptCompleto + "\n").getBytes(StandardCharsets (promptCompleto + "\n").getBytes(StandardCharsets.UTF_8));

        // Añadir flag -f con la ruta del archivo
        List<String> commandList = new ArrayList<>(pb.command());
        commandList.add("-f");
.UTF_8));

        // Añadir flag -f con la ruta del archivo
        List<String> commandList = new ArrayList<>(pb.command());
        commandList.add("-f");
        commandList        commandList.add(tempFile.toAbsolutePath().toString());
        pb.command(commandList);

        System.err.println("[DEBUG] Ejecutando comando: " + String.join(" ", pb.command()));

.add(tempFile.toAbsolutePath().toString());
        pb.command(commandList);

        System.err.println("[DEBUG] Ejecutando comando: " + String.join(" ", pb.command()));

        Process proceso = pb.start();

        // Capturar salida estándar        Process proceso = pb.start();

        // Capturar salida estándar
        StringBuilder
        StringBuilder output = new StringBuilder();
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> {
                    System.err.println("[stdout] " + line);
                    output = new StringBuilder();
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> {
                    System.err.println("[stdout] " + line);
                    output.append(line).append("\n");
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
            output.append(line).append("\n");
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        });

        // Capturar errores
        StringBuilder errores = new StringBuilder();
        Thread errorReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getErrorStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> {
                    }
        });

        // Capturar errores
        StringBuilder errores = new StringBuilder();
        Thread errorReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getErrorStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> {
                    System.err.println System.err.println("[stderr] " + line);
                    errores.append(line).append("\n");
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        });

        outputReader("[stderr] " + line);
                    errores.append(line).append("\n");
                });
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        });

.start();
        errorReader.start();

        long inicio = System.nanoTime();
        boolean terminado = proceso.waitFor(timeoutSegundos, TimeUnit.SECONDS        outputReader.start();
        errorReader.start();

        long inicio = System.nanoTime();
        boolean terminado = proceso.waitFor(timeoutSegundos, TimeUnit.SECONDS);
        long duracion = (System.nanoTime() - inicio));
        long duracion = (System.nanoTime() - inicio) / 1_000_000;

        / 1_000_000;

        outputReader.join();
        errorReader.join();

        if (!terminado) {
            proceso.destroyForcibly();
            return "**Timeout:** El modelo tardó más de " + timeout outputReader.join();
        errorReader.join();

        if (!terminado) {
            proceso.destroyForcibly();
            return "**Timeout:** El modelo tardó más de " + timeoutSegundos + "s (" + duracion + " ms reales)";
        }

        int exitCode = proceso.exitValue();
       Segundos + "s (" + duracion + " ms reales)";
        }

        int exitCode = proceso.exitValue();
        System.err.println("[DEBUG] Proceso finalizado con código: " + exitCode + " en " + duracion System.err.println("[DEBUG] Proceso finalizado con código: " + exitCode + " en " + duracion + " ms");

        if (exitCode != 0) {
            return "**Error:** llama.cpp terminó + " ms");

        if (exitCode != 0) {
            return "**Error:** llama.cpp terminó con código " + exit con código " + exitCode + "\n" + errores;
        }

        String respuesta = output.toStringCode + "\n" + errores;
        }

        String respuesta = output.toString().trim();
       ().trim();
        return respuesta.isEmpty() ? "**Advertencia:** El modelo no gener return respuesta.isEmpty() ? "**Advertencia:** El modelo no generó salida." : respuesta;

    } catch (IOException | InterruptedException e) {
        return "**Excepción:** " + e.getMessageó salida." : respuesta;

    } catch (IOException | InterruptedException e) {
        return "**Excepción:** " + e.getMessage();
    } finally {
        if (tempFile != null) {
            try { Files.delete(tempFile); } catch (IOException ignored) {}
        }
    }
}
```

();
    } finally {
        if (tempFile != null) {
            try { Files.delete(tempFile); } catch (IOException ignored) {}
        }
    }
}
```

---

## 🧪 ¿Qué cambia esta versión?

- Reduce `-n` a 128 para evitar cuelgues por prompts---

## 🧪 ¿Qué cambia esta versión?

- Reduce `-n` a 128 para evitar cuelgues por prompts largos.
- Añade logs en `stdout`, `stderr`, duración largos.
- Añade logs en `stdout`, `stderr`, duración real y código de salida.
- Asegura real y código de salida.
- Asegura que el prompt termina que el prompt termina con salto de línea con salto de línea.
- Devuelve mensajes claros si no hay salida o si hay.
- Devuelve mensajes claros si no hay salida o si hay error.

---

Pruébalo con un solo fragmento y dime qué ves en los logs. Si esto funciona error.

---

Pruébalo con un solo fragmento y dime qué ves en los logs. Si esto funciona, podemos escalarlo de nuevo a 256 o 512 tokens. ¿Te animas a probar, podemos escalarlo de nuevo a 256 o 512 tokens. ¿Te animas a probarlo ahora?lo ahora?