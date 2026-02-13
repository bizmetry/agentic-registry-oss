import React, { useMemo } from "react";
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, IconButton, Stack, Typography
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import DownloadIcon from "@mui/icons-material/Download";

function downloadJson(filename, data) {
  const json = JSON.stringify(data, null, 2);  // Formateamos el JSON
  const blob = new Blob([json], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);

  console.log("Downloading file:", filename);  // Para verificar si se ejecuta correctamente
}

function safeFilePart(s) {
  return (s || "")
    .toString()
    .trim()
    .replace(/[^\w\-]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+|_+$/g, "");
}

export default function AgentDefinitionDialog(props) {

  const {
    open,
    onClose,
    agentId,
    agentName,
    definition,
    loading,
    error
  } = props;

  // Preparamos la definición para formatearla como JSON
  const pretty = useMemo(() => {
    if (!definition) return "";
    try {
      return JSON.stringify(definition, null, 2);  // Formato legible para el JSON
    } catch (e) {
      console.error("Error formatting definition:", e);
      return String(definition);  // Si no se puede convertir a JSON, devolvemos el valor como string
    }
  }, [definition]);

  // Función para copiar el contenido al portapapeles
  const handleCopy = async () => {
    if (!pretty) return;
    try {
      await navigator.clipboard.writeText(pretty);  // Copiar al portapapeles
      console.log("Copied to clipboard:", pretty);  // Verificar si se copió correctamente
    } catch (err) {
      console.error("Error copying text to clipboard:", err);
    }
  };

  // Función para exportar el contenido a un archivo JSON
  const handleExport = () => {
    if (!definition ) {
      console.error("No definition or agentId provided.");  // Verificamos si la definición y el ID están presentes
      return;
    }

    const agentId = definition.agentId;
    const name = safeFilePart(agentName || "agent");  // Preparamos un nombre seguro para el archivo
    const ts = new Date().toISOString().replace(/[:.]/g, "-");  // Timestamp para evitar sobrescribir
    const filename = `agent-definition_${name}_${agentId}_${ts}.json`;  // Nombre final del archivo

    console.log("Exporting file:", filename);  // Verificamos que estamos a punto de exportar

    downloadJson(filename, definition);  // Llamamos a la función de descarga
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle sx={{ pr: 6 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <div>
            <Typography variant="h6">
              Agent Definition
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {agentName} {agentId && `• ${agentId}`}
            </Typography>
          </div>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Stack>
      </DialogTitle>

      <DialogContent dividers>
        {loading && (
          <Typography color="text.secondary">
            Loading definition…
          </Typography>
        )}

        {!loading && error && (
          <Typography color="error">
            {error}
          </Typography>
        )}

        {!loading && !error && definition && (
          <pre
            style={{
              margin: 0,
              whiteSpace: "pre-wrap",
              wordBreak: "break-word",
              fontSize: 12,
              lineHeight: 1.5
            }}
          >
            {pretty}  {/* Mostramos la definición en formato JSON */}
          </pre>
        )}

        {!loading && !error && !definition && (
          <Typography color="text.secondary">
            No definition returned.
          </Typography>
        )}
      </DialogContent>

      <DialogActions sx={{ justifyContent: "space-between" }}>
        <Typography variant="caption" color="text.secondary">
          /v1/api/registry/agents/{agentId}/definition
        </Typography>

        <Stack direction="row" spacing={1}>
          <Button
            startIcon={<ContentCopyIcon />}
            onClick={handleCopy}
            disabled={!pretty}  
          >
            Copy
          </Button>

          <Button
            startIcon={<DownloadIcon />}
            onClick={handleExport}
            disabled={!definition}  
          >
            Export
          </Button>

          <Button variant="contained" onClick={onClose}>
            Close
          </Button>
        </Stack>
      </DialogActions>
    </Dialog>
  );
}
