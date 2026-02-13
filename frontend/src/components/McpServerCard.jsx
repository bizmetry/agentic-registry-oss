import React, { useMemo, useState } from "react";
import {
  Card, CardContent, CardActions,
  Typography, Stack, Chip, Button,
  IconButton, Tooltip, Divider, Box,
  Dialog, DialogTitle, DialogContent, DialogActions
} from "@mui/material";
import LinkIcon from "@mui/icons-material/Link";
import EditIcon from "@mui/icons-material/Edit";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import WifiTetheringIcon from "@mui/icons-material/WifiTethering";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import CodeIcon from "@mui/icons-material/Code";          // ✅ NEW
import CloseIcon from "@mui/icons-material/Close";        // ✅ NEW
import DownloadIcon from "@mui/icons-material/Download";  // ✅ NEW
import { getServerDefinition } from "../api/mcpServers"; // ajustá el path


function clamp2LinesSx() {
  return {
    display: "-webkit-box",
    WebkitLineClamp: 2,
    WebkitBoxOrient: "vertical",
    overflow: "hidden",
  };
}

// ✅ helper para leer múltiples nombres de campo (camelCase / snake_case / aliases)
function pick(obj, keys, fallback = null) {
  for (const k of keys) {
    const v = obj?.[k];
    if (v !== undefined && v !== null && v !== "") return v;
  }
  return fallback;
}

/**
 * ✅ Estado REAL del backend (enum):
 * ACTIVE | UNKNOWN | FAILED | DISABLED
 * (tolerante a snake_case)
 */
function getBackendStatus(s) {
  const raw = pick(s, ["status", "serverStatus", "server_status"], null);
  if (!raw) return "UNKNOWN";
  const v = String(raw).trim().toUpperCase();

  if (["ACTIVE", "UNKNOWN", "FAILED", "DISABLED"].includes(v)) return v;
  return "UNKNOWN";
}

/**
 * ✅ Mapea el status de backend a chip UI
 */
function StatusChip({ status }) {
  const cfg = {
    ACTIVE: { label: "Active", color: "success" },
    FAILED: { label: "Failed", color: "error" },
    DISABLED: { label: "Disabled", color: "default" },
    UNKNOWN: { label: "Unknown", color: "warning" },
  }[status] || { label: "Unknown", color: "warning" };

  return (
    <Chip
      size="small"
      label={cfg.label}
      color={cfg.color}
      variant="outlined"
      sx={{ fontWeight: 700 }}
    />
  );
}

function formatDate(ts) {
  if (!ts) return "—";
  try {
    const d = new Date(ts);
    if (Number.isNaN(d.getTime())) return "—";
    return d.toLocaleString();
  } catch {
    return "—";
  }
}

function shortUrl(url) {
  if (!url) return "—";
  try {
    const u = new URL(url.startsWith("mcp://") ? url.replace("mcp://", "https://") : url);
    return u.host + u.pathname;
  } catch {
    return url.length > 28 ? url.slice(0, 28) + "…" : url;
  }
}

async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    // silent
  }
}

function safeFilePart(s) {
  return (s || "")
    .toString()
    .trim()
    .replace(/[^\w\-]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+|_+$/g, "");
}

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

export default function McpServerCard({ s, onEdit, onDelete, onTest }) {
  const [confirmOpen, setConfirmOpen] = useState(false);

  // ✅ NEW: view definition modal state
  const [defOpen, setDefOpen] = useState(false);
  const [defLoading, setDefLoading] = useState(false);
  const [defError, setDefError] = useState(null);
  const [definition, setDefinition] = useState(null);

  // ✅ NORMALIZACIÓN ROBUSTA (camel + snake + aliases)
  const id = pick(s, ["serverId", "server_id", "id", "uuid"], "—");

  const name = pick(
    s,
    ["name", "serverName", "server_name", "title"],
    "Unnamed MCP Server"
  );

  const desc = pick(
    s,
    ["description", "desc", "server_description"],
    "—"
  );

  const version = pick(
    s,
    ["version", "serverVersion", "server_version"],
    "—"
  );

  const discoveryUrl = pick(
    s,
    ["discoveryUrl", "discovery_url", "url", "endpoint"],
    "—"
  );

  const updatedAt = pick(
    s,
    ["updatedAt", "updated_ts", "updatedTs", "modifiedAt", "modified_ts", "createdAt", "created_ts"],
    null
  );

  // ✅ AHORA USAMOS EL STATUS REAL DEL BACKEND
  const status = useMemo(() => getBackendStatus(s), [s]);

  // ✅ NEW: fetch server definition from backend
  const fetchDefinition = async () => {
    const serverId = String(id || "").trim();
    if (!serverId || serverId === "—") {
      setDefError("Missing serverId");
      return;
    }

    setDefOpen(true);
    setDefLoading(true);
    setDefError(null);
    setDefinition(null);

    try {
      const data = await getServerDefinition(serverId);
      setDefinition(data);
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        "Failed to load server definition";
      setDefError(msg);
    } finally {
      setDefLoading(false);
    }
  };

  const prettyDefinition = useMemo(() => {
    if (!definition) return "";
    try {
      return JSON.stringify(definition, null, 2);
    } catch {
      return String(definition);
    }
  }, [definition]);

  const handleCopyDefinition = () => {
    if (!prettyDefinition) return;
    copyToClipboard(prettyDefinition);
  };

  const handleExportDefinition = () => {
    if (!definition) return;
    const serverId = String(id || "noid");
    const safeName = safeFilePart(name || "mcp-server");
    const ts = new Date().toISOString().replace(/[:.]/g, "-");
    downloadJson(`mcp-server-definition_${safeName}_${serverId}_${ts}.json`, definition);
  };

  return (
    <>
      <Card
        variant="outlined"
        sx={{
          borderRadius: 2.5,
          overflow: "hidden",
          height: "100%",
          transition: "transform 120ms ease, box-shadow 120ms ease, border-color 120ms ease",
          "&:hover": {
            transform: "translateY(-2px)",
            boxShadow: 3,
            borderColor: "divider"
          }
        }}
      >
        <CardContent sx={{ p: 2.25 }}>
          <Stack spacing={1.25}>
            {/* Header */}
            <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
              <Box sx={{ minWidth: 0 }}>
                <Typography
                  variant="subtitle1"
                  sx={{ fontWeight: 900, lineHeight: 1.2 }}
                  noWrap
                  title={name}
                >
                  {name}
                </Typography>

                <Typography
                  variant="body2"
                  sx={{ opacity: 0.75, mt: 0.25, ...clamp2LinesSx() }}
                  title={desc !== "—" ? desc : undefined}
                >
                  {desc}
                </Typography>
              </Box>

              <StatusChip status={status} />
            </Stack>

            {/* URL row */}
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
              <LinkIcon sx={{ opacity: 0.65 }} fontSize="small" />

              <Typography
                variant="body2"
                sx={{ fontFamily: "monospace", opacity: 0.85, ...clamp2LinesSx() }}
                title={discoveryUrl}
              >
                {shortUrl(discoveryUrl)}
              </Typography>

              {discoveryUrl && discoveryUrl !== "—" && (
                <Tooltip title="Copy discovery URL">
                  <IconButton size="small" onClick={() => copyToClipboard(discoveryUrl)}>
                    <ContentCopyIcon fontSize="inherit" />
                  </IconButton>
                </Tooltip>
              )}
            </Stack>

            <Divider />

            {/* Facts */}
            <Stack direction="row" spacing={2} justifyContent="space-between">
              <Box>
                <Typography variant="caption" sx={{ opacity: 0.7 }}>Version</Typography>
                <Typography variant="body2" sx={{ fontWeight: 800 }}>
                  {version}
                </Typography>
              </Box>

              <Box sx={{ textAlign: "right" }}>
                <Typography variant="caption" sx={{ opacity: 0.7 }}>Updated</Typography>
                <Typography variant="body2" sx={{ fontWeight: 800 }}>
                  {formatDate(updatedAt)}
                </Typography>
              </Box>
            </Stack>

            <Typography variant="caption" sx={{ opacity: 0.55 }}>
              ID: {String(id).slice(-10)}
            </Typography>
          </Stack>
        </CardContent>

        <CardActions sx={{ px: 2.25, pb: 2, pt: 0, gap: 1 }}>
          {onTest && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<WifiTetheringIcon />}
              onClick={() => onTest(s)}
              sx={{ borderRadius: 2 }}
            >
              Test
            </Button>
          )}

          {/* ✅ NEW button */}
        <Tooltip title="View Server Definition">
  <IconButton
    size="small"
    variant="outlined"
    onClick={fetchDefinition}
    sx={{ borderRadius: 2 }}
  >
    <CodeIcon />
  </IconButton>
</Tooltip>

          <Button
            size="small"
            variant="text"
            startIcon={<EditIcon />}
            onClick={onEdit}
            sx={{ borderRadius: 2 }}
          >
            Edit
          </Button>

          <Box sx={{ flex: 1 }} />

          <Tooltip title="Delete">
            <IconButton
              size="small"
              onClick={() => setConfirmOpen(true)}
              sx={{
                borderRadius: 2,
                "&:hover": { bgcolor: "action.hover" }
              }}
            >
              <DeleteOutlineIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </CardActions>
      </Card>

      {/* ✅ NEW: View server definition dialog */}
      <Dialog open={defOpen} onClose={() => setDefOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ pr: 6 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h6" sx={{ fontWeight: 900, lineHeight: 1.2 }}>
                Server Definition
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.7 }}>
                {name} • {String(id)}
              </Typography>
            </Box>

            <IconButton onClick={() => setDefOpen(false)} aria-label="Close">
              <CloseIcon />
            </IconButton>
          </Stack>
        </DialogTitle>

        <DialogContent dividers>
          {defLoading && (
            <Typography variant="body2" sx={{ opacity: 0.75 }}>
              Loading definition…
            </Typography>
          )}

          {!defLoading && defError && (
            <Typography variant="body2" color="error">
              {defError}
            </Typography>
          )}

          {!defLoading && !defError && definition && (
            <pre
              style={{
                margin: 0,
                whiteSpace: "pre-wrap",
                wordBreak: "break-word",
                fontSize: 12,
                lineHeight: 1.5
              }}
            >
              {prettyDefinition}
            </pre>
          )}

          {!defLoading && !defError && !definition && (
            <Typography variant="body2" sx={{ opacity: 0.75 }}>
              No definition returned.
            </Typography>
          )}
        </DialogContent>

        <DialogActions sx={{ justifyContent: "space-between" }}>
          <Typography variant="caption" sx={{ opacity: 0.7, pl: 1 }}>
            GET /v1/api/registry/mcp-servers/{String(id)}/definition
          </Typography>

          <Stack direction="row" spacing={1}>
            <Button
              startIcon={<ContentCopyIcon />}
              onClick={handleCopyDefinition}
              disabled={!prettyDefinition}
            >
              Copy
            </Button>

            <Button
              startIcon={<DownloadIcon />}
              onClick={handleExportDefinition}
              disabled={!definition}
            >
              Export
            </Button>

            <Button variant="contained" onClick={() => setDefOpen(false)}>
              Close
            </Button>
          </Stack>
        </DialogActions>
      </Dialog>

      {/* Confirm delete dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Delete MCP Server?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ opacity: 0.8 }}>
            This will remove <b>{name}</b> from your registry. This action can’t be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={async () => {
              setConfirmOpen(false);
              await onDelete?.();
            }}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
