import React, { useEffect, useMemo, useState } from "react";
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, TextField, MenuItem,
  Alert, Collapse, IconButton, Stack, Typography,
  Divider, Paper,
  Table, TableHead, TableRow, TableCell, TableBody,
  Tabs, Tab, Box, Chip, InputAdornment
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";

import ToolInvokeDialog from "../../components/ToolInvokeDialog";
import { CircularProgress } from "@mui/material";
import { invokeTool } from "../../api/mcpServers";

function parseApiError(err) {
  const data = err?.response?.data ?? err?.data;
  const status = err?.response?.status ?? err?.status;

  if (data && typeof data === "object") {
    return {
      status,
      code: data.status ?? data.errorCode ?? null,
      message: data.message ?? data.error ?? "Invoke Failed",
      details: data.error ?? data.debug ?? data.stacktrace ?? null
    };
  }

  if (typeof data === "string" && data.trim()) {
    return { status, code: null, message: data, details: null };
  }

  if (err?.message) {
    return { status, code: null, message: err.message, details: null };
  }

  return { status, code: null, message: "Invoke Failed", details: null };
}

function prettyJson(value) {
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value ?? "");
  }
}

/**
 * Normaliza "arguments" (JSON schema-ish) a filas: [{name, description, type, required}]
 */
function argsToRows(argumentsObj) {
  if (!argumentsObj || typeof argumentsObj !== "object") return [];

  const props = argumentsObj.properties && typeof argumentsObj.properties === "object"
    ? argumentsObj.properties
    : {};

  const requiredArr = Array.isArray(argumentsObj.required) ? argumentsObj.required : [];
  const requiredSet = new Set(requiredArr);

  return Object.entries(props).map(([name, def]) => {
    const desc = def?.description || def?.title || "—";
    const type = def?.type || (def?.anyOf ? "anyOf" : def?.oneOf ? "oneOf" : "—");
    return {
      name,
      description: desc,
      type,
      required: requiredSet.has(name)
    };
  });
}

function TabPanel({ value, index, children }) {
  return (
    <div role="tabpanel" hidden={value !== index}>
      {value === index && <Box sx={{ pt: 2 }}>{children}</Box>}
    </div>
  );
}

/**
 * ✅ Normaliza tools para persistir:
 * Devuelve [{name, description, arguments}]
 */
function normalizeTools(tools) {
  if (!Array.isArray(tools)) return [];
  return tools
    .map(t => ({
      name: t?.name ?? "",
      description: t?.description ?? "",
      arguments: t?.arguments ?? null
    }))
    .filter(t => t.name && String(t.name).trim().length > 0);
}

/**
 * ✅ Split URL en {schema, rest} si empieza con http/https
 */
function splitHttpUrl(full) {
  if (!full || typeof full !== "string") return { schema: "https://", rest: "" };
  const v = full.trim();
  if (v.startsWith("https://")) return { schema: "https://", rest: v.substring("https://".length) };
  if (v.startsWith("http://")) return { schema: "http://", rest: v.substring("http://".length) };
  return { schema: "https://", rest: v };
}

/**
 * ✅ Construye URL final a partir de schema + host/path sin protocolo
 */
function buildHttpUrl(schema, rest) {
  const s = (schema || "").trim();
  const r = (rest || "").trim();

  if (!r) return s || "";
  const normalizedSchema = (s === "http://" || s === "https://") ? s : "https://";
  const restNoProto = r.replace(/^https?:\/\//i, "");
  return normalizedSchema + restNoProto;
}

// ✅ NEW: Validación suave de GitHub repo URL (opcional)
function isValidGithubRepoUrl(url) {
  if (!url) return true; // optional
  const v = String(url).trim();
  if (!v) return true;
  return /^https:\/\/github\.com\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+\/?$/.test(v);
}

// ✅ NEW: SemVer helper: parsea "x.y.z" (o "x.y") a partes
function parseSemverParts(versionStr) {
  const raw = String(versionStr ?? "").trim();
  const m = raw.match(/^(\d+)\.(\d+)(?:\.(\d+))?$/);
  if (!m) return { major: 1, minor: 0, patch: 0, ok: false };
  return {
    major: Number(m[1]),
    minor: Number(m[2]),
    patch: m[3] != null ? Number(m[3]) : 0,
    ok: true
  };
}

export default function McpServerForm({ open, initial, onClose, onSave, onTest }) {
  const [tab, setTab] = useState(0);

  const [name, setName] = useState("");
  const [desc, setDesc] = useState("");

  // ============================================================
  // ✅ SemVer: major/minor/patch (x.y.z)
  // ============================================================
  const [verMajor, setVerMajor] = useState(1);
  const [verMinor, setVerMinor] = useState(0);
  const [verPatch, setVerPatch] = useState(0);
  const ver = useMemo(() => `${verMajor}.${verMinor}.${verPatch}`, [verMajor, verMinor, verPatch]);
  const [verError, setVerError] = useState("");

  // ✅ NEW: repository URL
  const [repoUrl, setRepoUrl] = useState("");
  const [repoUrlError, setRepoUrlError] = useState("");

  // ============================================================
  // ✅ Discovery URL split: schema + rest
  // ============================================================
  const [urlSchema, setUrlSchema] = useState("https://");
  const [urlRest, setUrlRest] = useState(""); // sin protocolo (host/path)

  const [connected, setConnected] = useState(false);

  const [testing, setTesting] = useState(false);
  const [testError, setTestError] = useState(null);
  const [showErrorDetails, setShowErrorDetails] = useState(false);

  // resultado del test (o precarga en edit)
  const [testResult, setTestResult] = useState(null);
  const [showMetadata, setShowMetadata] = useState(false);

  // ============================================================
  // ✅ Tool Invoke: estado + handlers
  // ============================================================
  const [invokeOpen, setInvokeOpen] = useState(false);
  const [selectedTool, setSelectedTool] = useState(null);

  // ✅ Resultado de ejecución (API real)
  const [invoking, setInvoking] = useState(false);
  const [invokeResult, setInvokeResult] = useState(null);
  const [invokeError, setInvokeError] = useState(null);
  const [invokeResultOpen, setInvokeResultOpen] = useState(false);

  // ✅ serverId disponible solo si el server ya existe (edit o luego de guardar)
  const serverId = initial?.serverId ?? null;

  const openInvoke = (tool) => {
    setSelectedTool(tool);
    setInvokeOpen(true);
  };

  const closeInvoke = () => {
    setInvokeOpen(false);
    setSelectedTool(null);
  };

  const closeResultDialog = () => {
    setInvokeResultOpen(false);
  };

  // ✅ Construimos URL final siempre desde schema+rest
  const discoveryUrlFull = useMemo(() => buildHttpUrl(urlSchema, urlRest), [urlSchema, urlRest]);

  const handleRunTool = async (payload) => {
    const toolName = payload?.tool?.name;
    const args = payload?.args || {};
    const bearerToken = payload?.auth?.bearerToken || "";
    const timeoutMs = Number(payload?.timeoutMs) || 30000;

    if (!serverId) {
      setInvokeError({
        message: "No serverId available. Save the MCP Server first, then invoke tools."
      });
      setInvokeResult(null);
      setInvokeResultOpen(true);
      return;
    }

    if (!toolName) return;

    setInvoking(true);
    setInvokeError(null);
    setInvokeResult(null);
    setInvokeResultOpen(true);
    closeInvoke();

    try {
      const result = await invokeTool(serverId, toolName, args, {
        timeoutMs,
        bearerToken
      });

      setInvokeResult(result);
    } catch (e) {
      const data = e?.response?.data;
      const status = e?.response?.status;

      setInvokeError({
        status,
        message: data?.message || data?.error || e?.message || "Tool invocation failed",
        details: data || null
      });
    } finally {
      setInvoking(false);
    }
  };

  // ✅ fuente de tools para UI:
  const toolsForUi = useMemo(() => {
    const fromTest = testResult?.tools ?? testResult?.toolDtos;
    if (Array.isArray(fromTest) && fromTest.length > 0) return fromTest;

    const fromInitial = initial?.tools;
    if (Array.isArray(fromInitial) && fromInitial.length > 0) return fromInitial;

    return [];
  }, [testResult, initial]);

  const metadataForUi = useMemo(() => {
    return (
      testResult?.metadata ??
      testResult?.initializeMetadata ??
      testResult?.initializeResult ??
      initial?.metadata ??
      null
    );
  }, [testResult, initial]);

  const resolvedUrlForUi = testResult?.resolvedUrl ?? initial?.resolvedUrl ?? null;
  const latencyMsForUi =
    typeof testResult?.latencyMs === "number"
      ? testResult.latencyMs
      : (typeof initial?.latencyMs === "number" ? initial.latencyMs : null);

  const toolsCount = toolsForUi.length;

  useEffect(() => {
    setTab(0);

    setName(initial?.name || "");
    setDesc(initial?.description || "");

    // ✅ SemVer precarga
    const parsed = parseSemverParts(initial?.version || "1.0.0");
    setVerMajor(parsed.major);
    setVerMinor(parsed.minor);
    setVerPatch(parsed.patch);
    setVerError(parsed.ok ? "" : "Invalid version format (expected x.y.z)");

    // ✅ repo URL precarga en edit
    setRepoUrl(initial?.repositoryUrl || "");
    setRepoUrlError("");

    // ✅ split discoveryUrl (http/https) a dropdown + rest
    const initialDiscovery = initial?.discoveryUrl || "";
    const split = splitHttpUrl(initialDiscovery);
    setUrlSchema(split.schema);
    setUrlRest(split.rest);

    // ✅ editar: permitimos guardar sin test
    setConnected(!!initial);

    setTesting(false);
    setTestError(null);
    setShowErrorDetails(false);

    setTestResult(null);
    setShowMetadata(false);

    setInvokeOpen(false);
    setSelectedTool(null);

    setInvoking(false);
    setInvokeResult(null);
    setInvokeError(null);
    setInvokeResultOpen(false);
  }, [initial, open]);

  // ✅ si cambia URL:
  useEffect(() => {
    setConnected(!!initial ? false : false);
    setTestResult(null);
  }, [urlSchema, urlRest, initial]);

  // ✅ NEW: validar repo URL en vivo (suave)
  useEffect(() => {
    const ok = isValidGithubRepoUrl(repoUrl);
    setRepoUrlError(ok ? "" : "Invalid GitHub repo URL (expected https://github.com/org/repo)");
  }, [repoUrl]);

  // ✅ NEW: validar SemVer parts (no negativos, ints)
  useEffect(() => {
    const ok =
      Number.isInteger(verMajor) && verMajor >= 0 &&
      Number.isInteger(verMinor) && verMinor >= 0 &&
      Number.isInteger(verPatch) && verPatch >= 0;

    setVerError(ok ? "" : "Version parts must be non-negative integers");
  }, [verMajor, verMinor, verPatch]);

  const connect = async () => {
    setTesting(true);
    setTestError(null);
    setShowErrorDetails(false);
    setShowMetadata(false);

    try {
      const fullUrl = discoveryUrlFull;
      const res = await onTest(fullUrl);
      setTestResult(res);
      setConnected(true);
      setTab(1);
    } catch (e) {
      setConnected(false);
      setTestError(parseApiError(e));
      setTab(0);
    } finally {
      setTesting(false);
    }
  };

  const errorHeader = useMemo(() => {
    if (!testError) return null;
    const parts = [];
    if (testError.code) parts.push(testError.code);
    if (testError.status) parts.push(`HTTP ${testError.status}`);
    return parts.length ? parts.join(" • ") : null;
  }, [testError]);

  const buildSavePayload = () => {
    const payload = {
      name,
      description: desc,
      version: ver,
      discoveryUrl: discoveryUrlFull,
      repositoryUrl: (repoUrl || "").trim() || null
    };

    const normalizedTools = normalizeTools(toolsForUi);
    if (normalizedTools.length > 0) payload.tools = normalizedTools;

    if (testResult?.ok === true) {
      payload.resolvedUrl = testResult?.resolvedUrl ?? null;
      payload.latencyMs = typeof testResult?.latencyMs === "number" ? testResult.latencyMs : null;
      payload.metadata = testResult?.metadata ?? null;
      payload.lastTestOk = true;
      payload.lastTestTs = new Date().toISOString();
    }

    return payload;
  };

  const handleSave = async () => {
    const payload = buildSavePayload();

    try {
      await onSave(payload);
    } catch (error) {
      if (error.response) {
        const errorMessage = error.response.data.message;

        setTestError({
          message: errorMessage,
          code: error.response.data.code || null,
          status: error.response.status,
          details: error.response.data.details || null
        });
        setShowErrorDetails(true);
      } else {
        setTestError({ message: "An unexpected error occurred." });
        setShowErrorDetails(false);
      }
    }
  };

  const canTest = useMemo(() => {
    const rest = (urlRest || "").trim().replace(/^https?:\/\//i, "");
    return rest.length > 0;
  }, [urlRest]);

  const canSave = useMemo(() => {
    const baseOk = (!!connected || !!initial) && !testing;
    return baseOk && !repoUrlError && !verError;
  }, [connected, initial, testing, repoUrlError, verError]);

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle sx={{ pb: 1 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <span>{initial ? "Edit MCP Server" : "Create MCP Server"}</span>

          {testResult?.ok && (
            <Chip
              size="small"
              label={`Connected • ${toolsCount} tools`}
              color="success"
              variant="outlined"
              sx={{ fontWeight: 800 }}
            />
          )}
        </Stack>
      </DialogTitle>

      <DialogContent>
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          variant="fullWidth"
          sx={{ borderBottom: 1, borderColor: "divider" }}
        >
          <Tab label="General" />
          <Tab
            label={`Tools${toolsCount ? ` (${toolsCount})` : ""}`}
            disabled={toolsCount === 0}
          />
        </Tabs>

        {/* GENERAL TAB */}
        <TabPanel value={tab} index={0}>
          <Stack spacing={2}>
            <Collapse in={!!testError}>
              {testError && (
                <Alert
                  severity="error"
                  variant="outlined"
                  action={
                    <Stack direction="row" spacing={0.5} alignItems="center">
                      {testError.details && (
                        <IconButton size="small" onClick={() => setShowErrorDetails(v => !v)}>
                          {showErrorDetails ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
                        </IconButton>
                      )}
                      <IconButton size="small" onClick={() => setTestError(null)}>
                        <CloseIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  }
                >
                  <Typography fontWeight={900}>
                    Connection test failed{errorHeader ? ` — ${errorHeader}` : ""}
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    {testError.message}
                  </Typography>

                  {testError.details && (
                    <Collapse in={showErrorDetails} sx={{ mt: 1 }}>
                      <Typography
                        component="pre"
                        variant="body2"
                        sx={{
                          m: 0,
                          p: 1.25,
                          borderRadius: 2,
                          bgcolor: "action.hover",
                          whiteSpace: "pre-wrap",
                          wordBreak: "break-word"
                        }}
                      >
                        {prettyJson(testError.details)}
                      </Typography>
                    </Collapse>
                  )}
                </Alert>
              )}
            </Collapse>

            <Collapse in={testResult?.ok === true}>
              {testResult?.ok === true && (
                <Alert severity="success" variant="outlined">
                  <Typography fontWeight={900}>Connected successfully</Typography>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    {testResult.resolvedUrl ? <>Resolved URL: <b>{testResult.resolvedUrl}</b></> : null}
                    {typeof testResult.latencyMs === "number" ? (
                      <>
                        {testResult.resolvedUrl ? " • " : ""}Latency: <b>{testResult.latencyMs} ms</b>
                      </>
                    ) : null}
                    {Array.isArray(testResult.tools) ? (
                      <>
                        {(testResult.resolvedUrl || typeof testResult.latencyMs === "number") ? " • " : ""}
                        Tools: <b>{testResult.tools.length}</b>
                      </>
                    ) : null}
                  </Typography>
                </Alert>
              )}
            </Collapse>

            <TextField fullWidth label="Name" value={name} onChange={e => setName(e.target.value)} />
            <TextField fullWidth label="Description" value={desc} onChange={e => setDesc(e.target.value)} />

            <Stack spacing={0.5}>
              <Typography fontWeight={800}>Version (x.y.z)</Typography>

              <Stack
                direction="row"
                spacing={1}
                alignItems="center"
                sx={{ width: "fit-content" }}
              >
                <TextField
                  size="small"
                  label="Major"
                  type="number"
                  value={verMajor}
                  onChange={(e) => {
                    const n = parseInt(e.target.value || "0", 10);
                    setVerMajor(Number.isFinite(n) ? Math.max(0, n) : 0);
                  }}
                  inputProps={{ min: 0, step: 1 }}
                  sx={{ width: 90 }}
                  error={!!verError}
                />

                <Typography sx={{ fontWeight: 700 }}>.</Typography>

                <TextField
                  size="small"
                  label="Minor"
                  type="number"
                  value={verMinor}
                  onChange={(e) => {
                    const n = parseInt(e.target.value || "0", 10);
                    setVerMinor(Number.isFinite(n) ? Math.max(0, n) : 0);
                  }}
                  inputProps={{ min: 0, step: 1 }}
                  sx={{ width: 90 }}
                  error={!!verError}
                />

                <Typography sx={{ fontWeight: 700 }}>.</Typography>

                <TextField
                  size="small"
                  label="Patch"
                  type="number"
                  value={verPatch}
                  onChange={(e) => {
                    const n = parseInt(e.target.value || "0", 10);
                    setVerPatch(Number.isFinite(n) ? Math.max(0, n) : 0);
                  }}
                  inputProps={{ min: 0, step: 1 }}
                  sx={{ width: 90 }}
                  error={!!verError}
                />
              </Stack>

              <Typography
                variant="caption"
                sx={{ opacity: 0.75, fontFamily: "monospace" }}
              >
                Version: {ver}
              </Typography>

              {verError && (
                <Typography variant="caption" color="error">
                  {verError}
                </Typography>
              )}
            </Stack>


            {/* ✅ Repository URL (GitHub) */}
            <TextField
              fullWidth
              label="Repository URL (GitHub)"
              value={repoUrl}
              onChange={e => setRepoUrl(e.target.value)}
              placeholder="https://github.com/org/repo"
              error={!!repoUrlError}
              helperText={repoUrlError || "Optional. Example: https://github.com/org/repo"}
            />

            {/* ✅ Discovery URL = schema dropdown + rest sin protocolo */}
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
              <TextField
                select
                label="Scheme"
                value={urlSchema}
                onChange={(e) => setUrlSchema(e.target.value)}
                sx={{ minWidth: 160 }}
              >
                <MenuItem value="https://">https://</MenuItem>
                <MenuItem value="http://">http://</MenuItem>
              </TextField>

              <TextField
                fullWidth
                label="Host / Path"
                value={urlRest}
                onChange={(e) => setUrlRest(e.target.value)}
                helperText="Enter host + path without protocol (e.g. mcp.exa.ai/mcp)"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Typography sx={{ fontFamily: "monospace", opacity: 0.8 }}>
                        {urlSchema}
                      </Typography>
                    </InputAdornment>
                  )
                }}
              />
            </Stack>

            {/* ✅ Preview del URL final */}
            <Typography variant="caption" sx={{ opacity: 0.75, fontFamily: "monospace" }}>
              Final URL: {discoveryUrlFull || "—"}
            </Typography>

            <Stack direction="row" spacing={1}>
              <Button
                onClick={connect}
                disabled={testing || !canTest}
                variant="outlined"
              >
                {testing ? "Testing..." : "Connect / Test"}
              </Button>

              {toolsCount > 0 && (
                <Button variant="text" onClick={() => setTab(1)} sx={{ textTransform: "none" }}>
                  View tools
                </Button>
              )}
            </Stack>

            {(resolvedUrlForUi || latencyMsForUi != null || metadataForUi) && (
              <>
                <Divider />
                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                  <Stack spacing={1}>
                    <Typography fontWeight={900}>Server Details</Typography>
                    <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                      <Typography variant="body2"><b>Resolved URL:</b> {resolvedUrlForUi ?? "—"}</Typography>
                      <Typography variant="body2"><b>Latency:</b> {latencyMsForUi != null ? `${latencyMsForUi} ms` : "—"}</Typography>
                      <Typography variant="body2"><b>Tools:</b> {toolsCount}</Typography>
                    </Stack>

                    {metadataForUi && (
                      <>
                        <Button
                          size="small"
                          onClick={() => setShowMetadata(v => !v)}
                          startIcon={showMetadata ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                          sx={{ width: "fit-content", textTransform: "none" }}
                        >
                          {showMetadata ? "Hide metadata" : "Show metadata"}
                        </Button>

                        <Collapse in={showMetadata}>
                          <Typography
                            component="pre"
                            variant="body2"
                            sx={{
                              m: 0,
                              p: 1.25,
                              borderRadius: 2,
                              bgcolor: "action.hover",
                              whiteSpace: "pre-wrap",
                              wordBreak: "break-word"
                            }}
                          >
                            {prettyJson(metadataForUi)}
                          </Typography>
                        </Collapse>
                      </>
                    )}
                  </Stack>
                </Paper>
              </>
            )}
          </Stack>
        </TabPanel>

        {/* TOOLS TAB */}
        <TabPanel value={tab} index={1}>
          {toolsCount === 0 ? (
            <Alert severity="info" variant="outlined">
              No tools available. Run <b>Connect / Test</b> to fetch tools from the MCP server.
            </Alert>
          ) : (
            <Stack spacing={2}>
              <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                <Stack spacing={0.5}>
                  <Typography fontWeight={900}>Tools</Typography>
                  <Typography variant="body2" sx={{ opacity: 0.8 }}>
                    {toolsCount} tools available.
                  </Typography>
                  {testResult?.ok !== true && (
                    <Typography variant="caption" sx={{ opacity: 0.7 }}>
                      Showing persisted tools (not a fresh test).
                    </Typography>
                  )}
                </Stack>
              </Paper>

              {!serverId && (
                <Alert severity="warning" variant="outlined">
                  Save the MCP Server first to enable tool invocation.
                </Alert>
              )}

              <Stack spacing={2}>
                {toolsForUi.map((t, idx) => {
                  const argRows = argsToRows(t.arguments);
                  return (
                    <Paper
                      key={`${t.name || "tool"}-${idx}`}
                      variant="outlined"
                      sx={{
                        borderRadius: 2,
                        overflow: "hidden",
                        border: "1.5px solid #4fc3f7",
                        "&:hover": {
                          borderColor: "#29b6f6",
                          background: "rgba(79,195,247,0.04)"
                        }
                      }}
                    >
                      <Box sx={{ p: 2 }}>
                        <Stack spacing={0.75}>
                          <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                            <Typography sx={{ fontWeight: 900, fontFamily: "monospace" }}>
                              {t.name ?? "—"}
                            </Typography>

                            <Button
                              size="small"
                              variant="contained"
                              disabled={!serverId}
                              onClick={() => openInvoke(t)}
                              sx={{ textTransform: "none", fontWeight: 800 }}
                            >
                              Invoke
                            </Button>
                          </Stack>

                          <Typography variant="body2" sx={{ opacity: 0.85 }}>
                            {t.description ?? "—"}
                          </Typography>
                        </Stack>
                      </Box>

                      <Divider />

                      <Box sx={{ p: 2 }}>
                        {argRows.length === 0 ? (
                          <Typography variant="body2" sx={{ opacity: 0.7 }}>
                            No arguments.
                          </Typography>
                        ) : (
                          <Table size="small">
                            <TableHead>
                              <TableRow>
                                <TableCell><b>Parameter</b></TableCell>
                                <TableCell><b>Description</b></TableCell>
                                <TableCell><b>Type</b></TableCell>
                                <TableCell align="center"><b>Required</b></TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {argRows.map((r) => (
                                <TableRow key={r.name}>
                                  <TableCell sx={{ fontFamily: "monospace" }}>{r.name}</TableCell>
                                  <TableCell>{r.description || "—"}</TableCell>
                                  <TableCell sx={{ fontFamily: "monospace" }}>{r.type || "—"}</TableCell>
                                  <TableCell align="center">
                                    {r.required ? <Chip size="small" label="Yes" color="primary" variant="outlined" /> : "—"}
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        )}
                      </Box>
                    </Paper>
                  );
                })}
              </Stack>
            </Stack>
          )}
        </TabPanel>

        {/* ✅ Dialog de Invoke */}
        <ToolInvokeDialog
          open={invokeOpen}
          onClose={closeInvoke}
          tool={selectedTool}
          server={{
            name,
            discoveryUrl: discoveryUrlFull,
            resolvedUrl: resolvedUrlForUi,
            version: ver
          }}
          onRun={handleRunTool}
        />

        {/* ✅ Dialog de resultado de ejecución */}
        <Dialog
          open={invokeResultOpen}
          onClose={closeResultDialog}
          fullWidth
          maxWidth="md"
        >
          <DialogTitle sx={{ pb: 1 }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Stack direction="row" spacing={1} alignItems="center">
                {invoking && <CircularProgress size={16} />}
                <span>
                  {invoking ? "Executing tool..." : "Tool Result"}
                </span>
              </Stack>

              <IconButton
                onClick={closeResultDialog}
                size="small"
                disabled={invoking}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Stack>
          </DialogTitle>

          <DialogContent>
            <Stack spacing={2} sx={{ pt: 1 }}>
              {invoking && (
                <Alert
                  severity="info"
                  variant="outlined"
                  icon={false}
                  sx={{ display: "flex", alignItems: "center" }}
                >
                  <Stack direction="row" spacing={1.25} alignItems="center">
                    <CircularProgress size={18} />
                    <Typography fontWeight={800}>Invoking tool…</Typography>
                    <Typography variant="body2" sx={{ opacity: 0.75 }}>
                      This may take a few seconds.
                    </Typography>
                  </Stack>
                </Alert>
              )}

              {invokeError && (
                <Alert severity="error" variant="outlined">
                  <Typography fontWeight={900}>
                    Invoke failed{invokeError.status ? ` — HTTP ${invokeError.status}` : ""}
                  </Typography>
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    {invokeError.message}
                  </Typography>

                  {invokeError.details && (
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
                      {prettyJson(invokeError.details)}
                    </Typography>
                  )}
                </Alert>
              )}

              {invokeResult && (
                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                  <Typography
                    component="pre"
                    variant="body2"
                    sx={{ m: 0, whiteSpace: "pre-wrap", wordBreak: "break-word" }}
                  >
                    {prettyJson(invokeResult)}
                  </Typography>
                </Paper>
              )}
            </Stack>
          </DialogContent>

          <DialogActions>
            <Button onClick={closeResultDialog}>Close</Button>
          </DialogActions>
        </Dialog>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>

        {tab === 0 && (
          <Button
            variant="contained"
            disabled={!canSave}
            onClick={handleSave}
          >
            Save
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
