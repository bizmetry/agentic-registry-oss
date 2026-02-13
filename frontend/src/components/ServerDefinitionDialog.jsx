import React, { useMemo } from "react";
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, IconButton, Stack, Typography
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import DownloadIcon from "@mui/icons-material/Download";

function downloadJson(filename, data) {
  const json = JSON.stringify(data, null, 2);
  const blob = new Blob([json], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);

  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function safeFilePart(s) {
  return (s || "")
    .toString()
    .trim()
    .replace(/[^\w\-]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+|_+$/g, "");
}

export default function ServerDefinitionDialog(props) {

  const {
    open,
    onClose,
    serverId,
    serverName,
    definition,
    loading,
    error
  } = props;

  const pretty = useMemo(() => {
    if (!definition) return "";
    try {
      return JSON.stringify(definition, null, 2);
    } catch {
      return String(definition);
    }
  }, [definition]);

  const handleCopy = async () => {
    if (!pretty) return;
    try {
      await navigator.clipboard.writeText(pretty);
    } catch {}
  };

  const handleExport = () => {
    if (!definition || !serverId) return;

    const name = safeFilePart(serverName || "mcp-server");
    const ts = new Date().toISOString().replace(/[:.]/g, "-");
    const filename = `mcp-server-definition_${name}_${serverId}_${ts}.json`;

    downloadJson(filename, definition);
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">

      <DialogTitle sx={{ pr: 6 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">

          <div>
            <Typography variant="h6">
              Server Definition
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {serverName} {serverId && `• ${serverId}`}
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
            {pretty}
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
          /v1/api/registry/mcp-servers/{serverId}/definition
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
