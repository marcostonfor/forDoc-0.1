<?php
session_start(); // Iniciamos sesión para mantener el estado entre peticiones
?>
<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <title>Chat con PromptProcessor.java</title>
    <style>
        body {
            font-family: sans-serif;
            background: #f4f4f4;
            padding: 20px;
        }

        #chat {
            background: white;
            padding: 15px;
            border-radius: 5px;
            max-width: 700px;
            margin: auto;
        }

        .mensaje {
            margin-bottom: 10px;
        }

        .usuario {
            font-weight: bold;
            color: #007acc;
        }

        .respuesta {
            color: #333;
            white-space: pre-wrap;
        }

        textarea {
            width: 100%;
            height: 100px;
        }

        button {
            padding: 10px 20px;
            margin-top: 10px;
        }
    </style>
</head>

<body>
    <div id="chat">
        <h2>Prompt Processor</h2>
        <form method="post" action="home.php">
            <textarea name="prompt" rows="10" cols="80" placeholder="Escribe tu prompt aquí..."></textarea><br><br>
            <input type="text" name="ruta" size="80"
                placeholder="Ruta de la carpeta para iniciar un nuevo análisis"><br><br>
            <label for="lineas">Líneas por fragmento:</label>
            <input type="number" id="lineas" name="lineas" value="20" min="5" max="100" style="width: 60px;">
            <label for="timeout">Timeout por fragmento (segundos):</label>
            <input type="number" id="timeout" name="timeout" value="360" min="30" max="600" style="width: 60px;"><br><br>
            <button type="submit" name="submit">Iniciar Análisis</button>
        </form>


        <?php        
        if ($_SERVER['REQUEST_METHOD'] === 'POST') {
            set_time_limit(0);

            // Si se proporciona una nueva ruta, creamos la cola de archivos.
            if (isset($_POST['ruta']) && !empty($_POST['ruta'])) {
                $rutaCarpeta = $_POST['ruta'];
                $files = glob($rutaCarpeta . '/*.*'); // Obtener todos los archivos
                $_SESSION['files_to_process'] = array_values($files); // Guardar la lista en la sesión
                $_SESSION['lineas'] = $_POST['lineas'] ?? '20';
                $_SESSION['timeout'] = $_POST['timeout'] ?? '180';
            }

            // Si hay archivos en la cola de la sesión, procesamos el siguiente.
            if (!empty($_SESSION['files_to_process'])) {
                $archivoActual = array_shift($_SESSION['files_to_process']); // Coge el primer archivo y lo quita de la lista

                echo "<hr><h3>Procesando archivo: <code>" . basename($archivoActual) . "</code></h3>";

                $prompt = $_POST['prompt'] ?? '';
                $lineas = $_SESSION['lineas'];
                $timeout = $_SESSION['timeout'];
                $classpath = "/home/marcos/Escritorio/html/forDoc1.2";

                // Llamamos a Java con la ruta del archivo individual
                $command = "java -Dfile.encoding=UTF-8 -cp " . escapeshellarg($classpath) . " PromptProcessor " . escapeshellarg($archivoActual) . " " . escapeshellarg($lineas) . " " . escapeshellarg($timeout);
                $full_command = "echo " . escapeshellarg($prompt) . " | " . $command . " 2>&1";
                exec($full_command, $output, $status);
                $respuesta_completa = implode("\n", $output);

                if ($status === 0) {
                    echo "<h4>Análisis del archivo:</h4><div class='respuesta'>" . nl2br(htmlspecialchars($respuesta_completa)) . "</div>";
                } else {
                    echo "<h4>Error al procesar el archivo (Estado: $status):</h4><pre class='respuesta'>" . htmlspecialchars($respuesta_completa) . "</pre>";
                }

                // Si todavía quedan archivos, mostramos el botón para continuar.
                if (!empty($_SESSION['files_to_process'])) {
                    $siguienteArchivo = basename($_SESSION['files_to_process'][0]);
                    echo '<hr><form method="post" action="home.php"><button type="submit">Continuar con: ' . htmlspecialchars($siguienteArchivo) . '</button></form>';
                } else {
                    echo "<hr><h3>✅ Todos los archivos han sido procesados.</h3>";
                    session_destroy(); // Limpiamos la sesión al terminar
                }
            } else {
                if (isset($_POST['ruta'])) {
                     echo "<hr><p>No se encontraron archivos procesables en la ruta especificada.</p>";
                }
            }
        }
        ?>
    </div>
</body>
</html>