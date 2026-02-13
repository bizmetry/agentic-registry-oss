import React, { useState, useRef, useMemo } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Stack,
  Typography,
  Alert,
  CircularProgress,
  Paper,
  IconButton,
  Collapse
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";

// Función para importar los agentes
import { importAgents } from "../api/agents"; // Asegúrate de tener la función en tu API

export function ImportAgentDialog({ open, onClose, onImported }) {
  const inputRef = useRef(null);

  const [file, setFile] = useState(null);
  const [fileText, setFileText] = useState("");
  const [busy, setBusy] = useState(false);

  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  const [importCompleted, setImportCompleted] = useState(false);

  const parsedPayload = useMemo(() => {
    if (!fileText) return null;
    try {
      return JSON.parse(fileText);
    } catch {
      return null;
    }
  }, [fileText]);

  const pickFile = () => inputRef.current?.click();

  const onFileSelected = async (e) => {
    const f = e.target.files?.[0] || null;
    setResult(null);
    setError(null);
    setFile(f);
    setFileText("");

    if (!f) return;

    try {
      const text = await f.text();
      setFileText(text);
    } catch (err) {
      setError({ message: err?.message || "Could not read file." });
    }
  };

  const resetAndClose = () => {
    setFile(null);
    setFileText("");
    setBusy(false);
    setError(null);
    setResult(null);
    setImportCompleted(false);
    onClose?.();
  };

  const doImport = async () => {
    setError(null);
    setResult(null);

    if (!file) {
      setError({ message: "Please select a .json file first." });
      return;
    }
    if (!parsedPayload) {
      setError({ message: "Invalid JSON. Please verify the exported file content." });
      return;
    }

    setBusy(true);
    try {
      const res = await importAgents(parsedPayload); // Llamar a la API para importar agentes
      setResult(res);
      onImported?.(res);
      setImportCompleted(true); // Marca como importación completada
    } catch (e) {
      const data = e?.response?.data;
      setError({
        message: data?.error || data?.message || e?.message || "Import failed",
        details: data || null
      });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onClose={busy ? undefined : resetAndClose} fullWidth maxWidth="sm">
      <DialogTitle sx={{ pb: 1 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Stack spacing={0.25}>
            <Typography fontWeight={900}>Import Agents</Typography>
            <Typography variant="body2" sx={{ opacity: 0.8 }}>
              Upload an exported agent JSON file
            </Typography>
          </Stack>

          <IconButton onClick={resetAndClose} disabled={busy}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Stack>
      </DialogTitle>

      {busy && <CircularProgress sx={{ width: "100%" }} />}

      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Collapse in={!!error}>
            {error && (
              <Alert severity="error" variant="outlined">
                <Typography fontWeight={900}>Import failed</Typography>
                <Typography variant="body2" sx={{ mt: 0.5 }}>
                  {error.message}
                </Typography>
                {error.details && (
                  <Typography
                    component="pre"
                    variant="body2"
                    sx={{
                      mt: 1,
                      m: 0,
                      p: 1.25,
                      borderRadius: 2,
                      bgcolor: "action.hover",
                      whiteSpace: "pre-wrap",
                      wordBreak: "break-word"
                    }}
                  >
                    {typeof error.details === "string" ? error.details : JSON.stringify(error.details, null, 2)}
                  </Typography>
                )}
              </Alert>
            )}
          </Collapse>

          <Collapse in={!!result?.ok}>
            {result?.ok && (
              <Alert severity="success" variant="outlined">
                <Typography fontWeight={900}>Import completed</Typography>
                <Typography variant="body2" sx={{ mt: 0.5 }}>
                  Created: <b>{result.created ?? 0}</b> • Updated: <b>{result.updated ?? 0}</b> • Total:{" "}
                  <b>{result.servers?.length ?? 0}</b>
                </Typography>
              </Alert>
            )}
          </Collapse>

          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
            <Stack spacing={1}>
              <Typography fontWeight={900}>Select file</Typography>
              <Typography variant="body2" sx={{ opacity: 0.8 }}>
                Choose a <b>.json</b> file exported from the Agent registry.
              </Typography>

              <input
                ref={inputRef}
                type="file"
                accept="application/json,.json"
                style={{ display: "none" }}
                onChange={onFileSelected}
              />

              <Stack direction="row" spacing={1} alignItems="center">
                <Button variant="outlined" onClick={pickFile} disabled={busy || importCompleted}>
                  Choose file…
                </Button>
                <Typography variant="body2" sx={{ opacity: 0.85 }}>
                  {file ? <b>{file.name}</b> : "No file selected"}
                </Typography>
              </Stack>

              {file && (
                <Typography variant="caption" sx={{ opacity: 0.75 }}>
                  {parsedPayload ? "✅ JSON looks valid" : "⚠️ JSON invalid or unreadable"}
                </Typography>
              )}
            </Stack>
          </Paper>
        </Stack>
      </DialogContent>

      <DialogActions>
        <Button onClick={resetAndClose} disabled={busy}>
          {importCompleted ? "Close" : "Cancel"}
        </Button>

        {!importCompleted && (
          <Button variant="contained" onClick={doImport} disabled={busy || !file}>
            Import
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
