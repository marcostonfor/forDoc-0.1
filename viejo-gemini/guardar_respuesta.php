<?php
$prompt = $_POST['prompt'] ?? '';

if (empty($prompt)) {
    exit("No se recibió ningún prompt.");
}

file_put_contents("input.txt", $prompt);

$command = "java PromptProcessor";
exec($command, $output, $status);

if ($status !== 0) {
    exit("Error al ejecutar PromptProcessor.");
}

$timestamp = date("Y-m-d_H-i-s");
$filename = "respuesta_$timestamp.md";

file_put_contents($filename, implode("\n", $output));

echo "✅ Respuesta guardada en: $filename";
